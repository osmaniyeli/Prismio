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

/**
 * GUNLUK GOREV HATIRLATMASI
 *
 * Her gun aksam (~19:00) bir bildirim. O gunun gorevleri bittiyse SUSAR.
 * Metinler oyunun dilinde, oyun tarafindan verilir (hatirlatmaAyarla).
 *
 * NEDEN setInexactRepeating: Kesin alarm (SCHEDULE_EXACT_ALARM) ozel izin
 * ister ve Play bunu oyunlar icin kabul etmez. Hatirlatmanin dakikasi
 * onemli degil; Android pil icin birkac dakika kaydirabilir.
 *
 * Telefon yeniden baslarsa alarmlar silinir: BOOT_COMPLETED ile yeniden kurulur.
 */
public class Hatirlatma extends BroadcastReceiver {

    private static final String ETIKET = "PrismioHatirlatma";
    private static final String KANAL = "gunluk_gorev";
    private static final String DEPO = "prismio_hatirlatma";
    private static final int SAAT = 19;
    private static final int ISTEK = 7101;
    private static final int BILDIRIM_NO = 7102;

    private static SharedPreferences depo(Context c) {
        return c.getSharedPreferences(DEPO, Context.MODE_PRIVATE);
    }

    /**
     * Oyun her kayitta cagirir.
     * aktif     : ayarlardaki "Gunluk hatirlatma"
     * bittiGun  : gorevlerin hepsinin bittigi gun ("2026-10-15" biciminde, yoksa "")
     */
    public static void ayarla(Context c, boolean aktif, String baslik, String metin, String bittiGun) {
        SharedPreferences.Editor e = depo(c).edit();
        e.putBoolean("aktif", aktif);
        if (baslik != null) e.putString("baslik", kisalt(baslik, 60));
        if (metin != null) e.putString("metin", kisalt(metin, 200));
        e.putString("bittiGun", bittiGun == null ? "" : bittiGun);
        e.apply();
        if (aktif) zamanla(c); else iptal(c);
    }

    private static String kisalt(String s, int n) { return s.length() > n ? s.substring(0, n) : s; }

    static void zamanla(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, SAAT);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        if (k.getTimeInMillis() <= System.currentTimeMillis()) k.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, k.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, niyet(c));
    }

    static void iptal(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(niyet(c));
    }

    private static PendingIntent niyet(Context c) {
        Intent i = new Intent(c, Hatirlatma.class).setAction("com.prismio.app.HATIRLAT");
        return PendingIntent.getBroadcast(c, ISTEK, i,
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
        if (Intent.ACTION_BOOT_COMPLETED.equals(eylem)
                || "android.intent.action.MY_PACKAGE_REPLACED".equals(eylem)) {
            if (d.getBoolean("aktif", false)) zamanla(c);
            return;
        }
        if (!d.getBoolean("aktif", false)) return;
        if (bugun().equals(d.getString("bittiGun", ""))) return;     // gorevler bitti, rahatsiz etme
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        goster(c, d.getString("baslik", "Prismio"),
                d.getString("metin", "Günlük görevin seni bekliyor."));
    }

    private static void goster(Context c, String baslik, String metin) {
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
            PendingIntent dokun = PendingIntent.getActivity(c, ISTEK + 1, ac,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder b = new NotificationCompat.Builder(c, KANAL)
                    .setSmallIcon(R.drawable.ic_bildirim)
                    .setContentTitle(baslik)
                    .setContentText(metin)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(metin))
                    .setContentIntent(dokun)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat.from(c).notify(BILDIRIM_NO, b.build());
        } catch (SecurityException se) {
            Log.w(ETIKET, "bildirim izni yok", se);
        } catch (Throwable t) {
            Log.w(ETIKET, "bildirim gosterilemedi", t);
        }
    }
}
