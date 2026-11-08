package com.prismio.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Random;

/**
 * HATIRLATICILAR - gunde EN FAZLA 3 kisa bildirim
 *
 *   1. ogle        12:00 - 12:59   (mola)
 *   2. aksam ustu  17:00 - 17:59   (is cikisi)
 *   3. gece        20:45 - 21:14   (~21:00)
 *
 * Telefonun en cok elde tutuldugu saatler. Her pencerede dakika RASTGELE secilir,
 * her gun yeniden (tohum = cihaz tuzu + yil + gun + pencere -> ayni gun icinde
 * yeniden kurulsa da ayni dakika cikar, ertesi gun degisir). Yerel saat dilimi.
 *
 * ALARM TURU: AlarmManager.setAndAllowWhileIdle (yaklasik, tek seferlik).
 * Kesin alarm (SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM) KULLANILMAZ: ozel izin ister,
 * Play oyunlar icin kabul etmez, hatirlatma icin dakika kesinligi gerekmez.
 * Alarm calisinca ayni pencerenin ERTESI GUNKU kurulur (zincir).
 * Yeniden baslatma / surum guncelleme / saat dilimi degisimi: hepsi yeniden kurulur.
 *
 * SUSMA KURALLARI (basit):
 *   - O gunun gorevleri bittiyse hicbiri atilmaz.
 *   - Bugun zaten bolum cozulduyse 3. (gece) atilmaz.
 *   - Oyun son 90 dakikada acildiysa o bildirim atilmaz (oyuncu zaten orada).
 *   - Alarm 2 saatten fazla gec calistiysa (Doze/kapali telefon) atilmaz.
 *
 * Metinler oyunun dilinde, oyun tarafindan verilir (hatirlaticilariAyarla).
 */
public class Hatirlatma extends BroadcastReceiver {

    private static final String ETIKET = "PrismioHatirlatma";
    private static final String KANAL = "gunluk_gorev";
    private static final String DEPO = "prismio_hatirlatma";
    private static final String EYLEM = "com.prismio.app.HATIRLAT";

    private static final int SLOT = 3;
    private static final int ISTEK_ESKI = 7101;      // eski surumdeki tekrarlayan alarm
    private static final int ISTEK_TABAN = 7110;     // alarmlar: 7110, 7111, 7112
    private static final int BILDIRIM_TABAN = 7120;  // bildirimler: 7120, 7121, 7122 (ayri kimlik)
    private static final int ISTEK_AC = 7130;        // dokununca uygulamayi ac: 7130..7132

    /** Pencere baslangici (gun icinde dakika) ve uzunlugu (dakika). */
    private static final int[] PENCERE_BAS = { 12 * 60, 17 * 60, 20 * 60 + 45 };
    private static final int[] PENCERE_UZ = { 60, 60, 30 };

    private static final long GEC_SINIR_MS = 2L * 60 * 60 * 1000;
    private static final long ETKIN_SINIR_MS = 90L * 60 * 1000;

    private static final String[] VARSAYILAN_METIN = {
            "Kısa bir mola: bir bulmaca ışığını bekliyor.",
            "Günlük görevlerin seni bekliyor. Birkaç bölüm çöz, ışığını al.",
            "Günü sakin bir bulmacayla kapat. Birkaç bölüm yeter."
    };

    private static SharedPreferences depo(Context c) {
        return c.getSharedPreferences(DEPO, Context.MODE_PRIVATE);
    }

    /** Eski tek metinli bicim: ayni metin 3 pencereye de gider. */
    public static void ayarla(Context c, boolean aktif, String baslik, String metin, String bittiGun) {
        ayarla(c, aktif, baslik, new String[] { metin, metin, metin }, bittiGun, "");
    }

    /**
     * Oyun her kayitta ve acilista cagirir.
     * aktif     : ayarlardaki "Hatirlaticilar"
     * metinler  : 3 metin (ogle, aksam ustu, gece); null/bos olan varsayilana duser
     * bittiGun  : gorevlerin hepsinin bittigi gun ("2026-11-8" biciminde, yoksa "")
     * oynadiGun : bugun bolum cozuldu ise bugunun anahtari, yoksa ""
     */
    public static void ayarla(Context c, boolean aktif, String baslik, String[] metinler,
                              String bittiGun, String oynadiGun) {
        SharedPreferences.Editor e = depo(c).edit();
        e.putBoolean("aktif", aktif);
        if (baslik != null) e.putString("baslik", kisalt(baslik, 60));
        for (int i = 0; i < SLOT; i++) {
            String m = (metinler != null && i < metinler.length) ? metinler[i] : null;
            if (m != null && m.length() > 0) e.putString("m" + i, kisalt(m, 200));
        }
        e.putString("bittiGun", bittiGun == null ? "" : bittiGun);
        e.putString("oynadiGun", oynadiGun == null ? "" : oynadiGun);
        e.putLong("sonEtkin", System.currentTimeMillis());
        e.apply();
        if (aktif) zamanla(c); else iptal(c);
    }

    private static String kisalt(String s, int n) { return s.length() > n ? s.substring(0, n) : s; }

    /** Cihaza ozel sabit tuz: herkesin ayni dakikada bildirim almasini engeller. */
    private static long tuz(Context c) {
        SharedPreferences d = depo(c);
        long t = d.getLong("tuz", 0L);
        if (t == 0L) {
            t = new Random().nextLong() | 1L;
            d.edit().putLong("tuz", t).apply();
        }
        return t;
    }

    /** gunEkle gun sonrasinin, slot penceresindeki rastgele dakikasi (ms). */
    private static long tetik(Context c, int slot, int gunEkle) {
        Calendar k = Calendar.getInstance();
        k.add(Calendar.DAY_OF_YEAR, gunEkle);
        long tohum = tuz(c) + k.get(Calendar.YEAR) * 1000L + k.get(Calendar.DAY_OF_YEAR) * 31L + slot * 7919L;
        int dakika = PENCERE_BAS[slot] + new Random(tohum).nextInt(PENCERE_UZ[slot]);
        k.set(Calendar.HOUR_OF_DAY, dakika / 60);
        k.set(Calendar.MINUTE, dakika % 60);
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        return k.getTimeInMillis();
    }

    /** Uc alarmi da kurar. Bugunku gectiyse yarininki. Tekrar tekrar cagrilmasi zararsiz. */
    static void zamanla(Context c) {
        eskiAlarmiIptal(c);
        for (int s = 0; s < SLOT; s++) {
            long t = tetik(c, s, 0);
            if (t <= System.currentTimeMillis() + 5000L) t = tetik(c, s, 1);
            kur(c, s, t);
        }
    }

    /** Tek bir slotun ertesi gunku alarmini kurar (alarm calisinca). */
    private static void yarinaKur(Context c, int slot) {
        kur(c, slot, tetik(c, slot, 1));
    }

    private static void kur(Context c, int slot, long zaman) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, niyet(c, slot, zaman));
        } catch (Throwable t) {
            Log.w(ETIKET, "alarm kurulamadi", t);
        }
    }

    static void iptal(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            for (int s = 0; s < SLOT; s++) am.cancel(niyet(c, s, 0L));
            eskiAlarmiIptal(c);
        } catch (Throwable t) {
            Log.w(ETIKET, "alarm iptal edilemedi", t);
        }
    }

    /** Onceki surumlerde kurulan tek tekrarlayan alarm (aksam 19:00). */
    private static void eskiAlarmiIptal(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent i = new Intent(c, Hatirlatma.class).setAction(EYLEM);
            am.cancel(PendingIntent.getBroadcast(c, ISTEK_ESKI, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Throwable t) {
            Log.w(ETIKET, "eski alarm iptal edilemedi", t);
        }
    }

    private static PendingIntent niyet(Context c, int slot, long zaman) {
        Intent i = new Intent(c, Hatirlatma.class).setAction(EYLEM)
                .putExtra("slot", slot)
                .putExtra("plan", zaman);
        return PendingIntent.getBroadcast(c, ISTEK_TABAN + slot, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String bugun() {
        Calendar k = Calendar.getInstance();
        return k.get(Calendar.YEAR) + "-" + (k.get(Calendar.MONTH) + 1) + "-" + k.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String eylem = i == null ? null : i.getAction();
        SharedPreferences d = depo(c);
        boolean aktif = d.getBoolean("aktif", false);

        // Yeniden baslatma, surum guncelleme, saat dilimi degisimi: alarmlari yeniden kur.
        if (Intent.ACTION_BOOT_COMPLETED.equals(eylem)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(eylem)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(eylem)) {
            if (aktif) zamanla(c); else eskiAlarmiIptal(c);
            return;
        }
        if (!EYLEM.equals(eylem)) return;

        int slot = i.getIntExtra("slot", -1);
        if (slot < 0 || slot >= SLOT) {
            // Eski surumden kalma alarm: sil ve yenilerini kur, bildirim atma.
            if (aktif) zamanla(c); else eskiAlarmiIptal(c);
            return;
        }
        if (!aktif) return;

        // Once ertesi gunu kur; asagidaki susma kurallari sonraki gunleri etkilemesin.
        yarinaKur(c, slot);

        long simdi = System.currentTimeMillis();
        long plan = i.getLongExtra("plan", 0L);
        if (plan > 0L && simdi - plan > GEC_SINIR_MS) return;                 // cok gec kalmis
        String bugun = bugun();
        if (bugun.equals(d.getString("bittiGun", ""))) return;                // gorevler bitti
        if (slot == SLOT - 1 && bugun.equals(d.getString("oynadiGun", ""))) return;   // bugun oynandi: gece susar
        if (simdi - d.getLong("sonEtkin", 0L) < ETKIN_SINIR_MS) return;       // oyuncu zaten oyunda
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        String metin = d.getString("m" + slot, VARSAYILAN_METIN[slot]);
        goster(c, slot, d.getString("baslik", "Prismio"), metin);
    }

    private static void goster(Context c, int slot, String baslik, String metin) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = c.getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(KANAL) == null) {
                    NotificationChannel k = new NotificationChannel(KANAL, baslik,
                            NotificationManager.IMPORTANCE_DEFAULT);
                    nm.createNotificationChannel(k);
                }
            }
            Intent ac = new Intent(c, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent dokun = PendingIntent.getActivity(c, ISTEK_AC + slot, ac,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder b = new NotificationCompat.Builder(c, KANAL)
                    .setSmallIcon(R.drawable.ic_bildirim)
                    .setContentTitle(baslik)
                    .setContentText(metin)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(metin))
                    .setContentIntent(dokun)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat.from(c).notify(BILDIRIM_TABAN + slot, b.build());
        } catch (SecurityException se) {
            Log.w(ETIKET, "bildirim izni yok", se);
        } catch (Throwable t) {
            Log.w(ETIKET, "bildirim gosterilemedi", t);
        }
    }
}
