package com.prismio.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * OYUN ILE PLAY GAMES ARASINDAKI KOPRU
 *
 * Oyun HTML/JavaScript. Play Games Java. Bu sinif ikisini bagliyor.
 *
 * GUVENLIK NOTU - bu onemli:
 *   WebView'da JavaScript koprusu (addJavascriptInterface) normalde
 *   tehlikelidir. Kotu niyetli bir sayfa bu koprüyu kullanip
 *   uygulamanin icine girebilir.
 *
 *   Burada tehlike YOK cunku:
 *    1. WebView'a SADECE kendi dosyamiz yukleniyor (file:///android_asset).
 *       Her dis istek MainActivity'de engelleniyor.
 *    2. Koprudeki metotlarin hicbiri dosya/sistem erisimi vermiyor.
 *    3. Gelen veri sadece metin olarak kayit dosyasina yaziliyor,
 *       kod olarak calistirilmiyor.
 *    4. minSdk 23 -> @JavascriptInterface zorunlu, eski API 16 acigi yok.
 */
public class OyunKopru {

    private static final String ETIKET = "PrismioKopru";

    private final Activity etkinlik;
    private final WebView web;
    private final String kayitAdi;

    /** D-107: Prismio Plus satin alma. Null olabilir - o zaman Plus sekmesi
     *  "yakinda" der ve satin alma butonu GOSTERILMEZ. */
    private Odeme odeme;

    /** D-129: Odullu reklam. Kimlik bos oldukca hazirMi() false doner
     *  ve JS tarafi reklamla ilgili HICBIR SEY gostermez. */
    private Reklam reklam;

    public OyunKopru(Activity etkinlik, WebView web, String kayitAdi) {
        this.etkinlik = etkinlik;
        this.web = web;
        this.kayitAdi = kayitAdi;
    }

    /** MainActivity reklam sistemini burada baglar. */
    public void reklamiBagla() {
        if (reklam != null) return;
        reklam = new Reklam(etkinlik, new Reklam.Geri() {
            @Override
            public void bitti(boolean tamam) {
                jsCagir("window.__reklamSonuc && window.__reklamSonuc(" + tamam + ")");
            }
            @Override
            public void durumDegisti() {
                jsCagir("window.__reklamDurum && window.__reklamDurum()");
            }
        });
        reklam.basla();
    }

    /** MainActivity satin alma sistemini burada baglar. */
    public void odemeyiBagla() {
        if (odeme != null) return;
        odeme = new Odeme(etkinlik, new Odeme.Geri() {
            @Override
            public void durum(boolean sahip, String fiyat, boolean hazir, boolean dogrulandi) {
                jsCagir("window.__plusDurum && window.__plusDurum("
                        + sahip + "," + metinKacir(fiyat) + "," + hazir + "," + dogrulandi + ")");
            }
            @Override
            public void sonuc(boolean basarili, String mesaj) {
                jsCagir("window.__plusSonuc && window.__plusSonuc("
                        + basarili + "," + metinKacir(mesaj) + ")");
            }
            @Override
            public void fiyatlar(String json) {
                jsCagir("window.__fiyatlar && window.__fiyatlar(" + metinKacir(json) + ")");
            }
            @Override
            public void teslim(String urunId, String anahtar) {
                jsCagir("window.__urunTeslim && window.__urunTeslim("
                        + metinKacir(urunId) + "," + metinKacir(anahtar) + ")");
            }
            @Override
            public void urunSonuc(String urunId, boolean basarili, String mesaj) {
                jsCagir("window.__urunSonuc && window.__urunSonuc("
                        + metinKacir(urunId) + "," + basarili + "," + metinKacir(mesaj) + ")");
            }
        });
        odeme.basla();
    }

    public void odemeyiKapat() {
        if (odeme != null) { odeme.kapat(); odeme = null; }
    }

    // --- JavaScript'e geri haber verme ---
    private void jsCagir(final String js) {
        if (web == null) return;
        etkinlik.runOnUiThread(() -> {
            try { web.evaluateJavascript(js, null); }
            catch (Exception e) { Log.w(ETIKET, "js cagrilamadi", e); }
        });
    }

    /**
     * Java metnini JavaScript'e GUVENLI metin olarak verir.
     * Eskiden sadece \\ " \\n \\r kaciriliyordu; oyuncu adi Google'dan gelir ve
     * her karakteri icerebilir (U+2028, </script>, kontrol karakteri...).
     * JSONObject.quote hepsini kacirir; sonuc her zaman tek bir JS metin sabitidir.
     */
    private static String metinKacir(String s) {
        if (s == null) return "null";
        // U+2028/2029 metin icinde ES2019'dan beri gecerli; WebView (Chrome 66+) sorun cikarmaz.
        return JSONObject.quote(s);
    }

    /**
     * Oyun aciliyor: zaten giris yapilmis mi?
     * Play Games v2 acilista kendiliginden sessiz giris dener;
     * isAuthenticated() o denemenin sonucunu bekler.
     * Cevap: __pgDurum(girisli, oyuncuAdi)
     */
    @JavascriptInterface
    public void durumSor() {
        etkinlik.runOnUiThread(() -> {
            try {
                GamesSignInClient istemci = PlayGames.getGamesSignInClient(etkinlik);
                istemci.isAuthenticated().addOnCompleteListener(gorev -> {
                    boolean girisli = gorev.isSuccessful()
                            && gorev.getResult() != null
                            && gorev.getResult().isAuthenticated();
                    if (!gorev.isSuccessful()) Log.w(ETIKET, "giris durumu alinamadi", gorev.getException());
                    if (girisli) oyuncuAdiyla(ad -> jsCagir("window.__pgDurum && window.__pgDurum(true," + metinKacir(ad) + ")"));
                    else jsCagir("window.__pgDurum && window.__pgDurum(false,null)");
                });
            } catch (Throwable t) {
                Log.w(ETIKET, "durumSor hatasi", t);
                jsCagir("window.__pgDurum && window.__pgDurum(false,null)");
            }
        });
    }

    /**
     * Oyuncu "Giris yap" dedi. Play Games giris penceresi acilir.
     * signIn() arayuz actigi icin ANA IS PARCACIGINDA cagrilmali -
     * JavascriptInterface metotlari ise ayri bir is parcaciginda calisir.
     * Cevap: __pgGiris(ok, oyuncuAdi)
     */
    @JavascriptInterface
    public void girisYap() {
        etkinlik.runOnUiThread(() -> {
            try {
                GamesSignInClient istemci = PlayGames.getGamesSignInClient(etkinlik);
                istemci.signIn().addOnCompleteListener(gorev -> {
                    boolean ok = gorev.isSuccessful()
                            && gorev.getResult() != null
                            && gorev.getResult().isAuthenticated();
                    if (!ok) {
                        // En sik sebep: Play Console'da OAuth istemcisi / SHA-1 eksik
                        // ya da hesap test kullanicisi degil. Bkz. PLAY_GAMES_KURULUM.md
                        Log.w(ETIKET, "giris basarisiz", gorev.getException());
                        jsCagir("window.__pgGiris && window.__pgGiris(false,null)");
                        return;
                    }
                    oyuncuAdiyla(ad -> jsCagir("window.__pgGiris && window.__pgGiris(true," + metinKacir(ad) + ")"));
                });
            } catch (Throwable t) {
                Log.w(ETIKET, "girisYap hatasi", t);
                jsCagir("window.__pgGiris && window.__pgGiris(false,null)");
            }
        });
    }

    private interface AdGeri { void ad(String ad); }

    /** Girisli oyuncunun Play Games adini alir. Alinamazsa null verir, giris yine gecerlidir. */
    private void oyuncuAdiyla(final AdGeri geri) {
        try {
            PlayGames.getPlayersClient(etkinlik).getCurrentPlayer().addOnCompleteListener(g -> {
                String ad = null;
                if (g.isSuccessful() && g.getResult() != null) ad = g.getResult().getDisplayName();
                geri.ad(ad);
            });
        } catch (Throwable t) {
            Log.w(ETIKET, "oyuncu adi alinamadi", t);
            geri.ad(null);
        }
    }

    /**
     * Ilerlemeyi buluta yaz. veri = JSON metni.
     * aciklama = oyuncuya gosterilecek kisa metin ("Bolum 247 - 12 yildiz")
     * oynamaSn = toplam oynama suresi (saniye)
     *
     * Play Games Kalite Kontrol 6.1 ZORUNLU:
     * kayda kapak resmi, aciklama ve sure eklenmelidir.
     */
    @JavascriptInterface
    public void yedekle(final String veri, final String aciklama, final long oynamaSn) {
        if (veri == null || veri.length() == 0) {
            jsCagir("window.__pgYedek && window.__pgYedek(false)");
            return;
        }
        // Ayni kayit dosyasi ayni anda iki kez acilamaz. Ust uste hizli
        // bolum bitince ikinci yedek "zaten acik" hatasiyla dusuyordu.
        // Yedek suruyorsa en son veriyi sakla, bitince onu yaz.
        synchronized (this) {
            if (yedekSuruyor) {
                bekleyenVeri = veri; bekleyenAciklama = aciklama; bekleyenSn = oynamaSn;
                return;
            }
            yedekSuruyor = true;
        }
        etkinlik.runOnUiThread(() -> {
            try { yedekYaz(veri, aciklama, oynamaSn); }
            catch (Throwable t) { Log.w(ETIKET, "yedek baslatilamadi", t); yedekBitti(false); }
        });
    }

    private boolean yedekSuruyor = false;
    private String bekleyenVeri, bekleyenAciklama;
    private long bekleyenSn;

    /** Bir yedek bitti: sonucu JS'e bildir, bekleyen varsa onu yaz. */
    private void yedekBitti(boolean ok) {
        jsCagir("window.__pgYedek && window.__pgYedek(" + ok + ")");
        String v, a; long sn;
        synchronized (this) {
            if (bekleyenVeri == null) { yedekSuruyor = false; return; }
            v = bekleyenVeri; a = bekleyenAciklama; sn = bekleyenSn;
            bekleyenVeri = null; bekleyenAciklama = null;
        }
        try { yedekYaz(v, a, sn); }
        catch (Throwable t) { Log.w(ETIKET, "bekleyen yedek yazilamadi", t); yedekBitti(false); }
    }

    private void yedekYaz(final String veri, final String aciklama, final long oynamaSn) {
        SnapshotsClient istemci = PlayGames.getSnapshotsClient(etkinlik);
        istemci.open(kayitAdi, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener(acmaGorevi -> {
                if (!acmaGorevi.isSuccessful() || acmaGorevi.getResult() == null) {
                    yedekBitti(false);
                    return;
                }
                try {
                    Snapshot anlik = acmaGorevi.getResult().getData();
                    if (anlik == null) {
                        yedekBitti(false);
                        return;
                    }
                    anlik.getSnapshotContents()
                         .writeBytes(veri.getBytes(StandardCharsets.UTF_8));

                    // Kalite Kontrol 6.1: ustveri ZORUNLU
                    SnapshotMetadataChange.Builder yapici = new SnapshotMetadataChange.Builder()
                            .setDescription(
                                (aciklama != null && aciklama.length() > 0)
                                    ? aciklama : "Prismio ilerleme")
                            .setPlayedTimeMillis(Math.max(0, oynamaSn) * 1000L);

                    // Kapak resmi: oyun tahtasinin o anki gorunumu.
                    Bitmap kapak = kapakCek();
                    if (kapak != null) yapici.setCoverImage(kapak);

                    SnapshotMetadataChange degisim = yapici.build();

                    istemci.commitAndClose(anlik, degisim)
                        .addOnCompleteListener(yazma -> {
                            if (!yazma.isSuccessful()) Log.w(ETIKET, "kayit yazilamadi", yazma.getException());
                            yedekBitti(yazma.isSuccessful());
                        });
                } catch (Exception e) {
                    Log.w(ETIKET, "yedekleme hatasi", e);
                    yedekBitti(false);
                }
            });
    }

    /** Buluttaki ilerlemeyi oku. */
    @JavascriptInterface
    public void indir() {
        etkinlik.runOnUiThread(this::indirBasla);
    }

    private void indirBasla() {
        SnapshotsClient istemci = PlayGames.getSnapshotsClient(etkinlik);
        istemci.open(kayitAdi, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener(gorev -> {
                if (!gorev.isSuccessful() || gorev.getResult() == null) {
                    Log.w(ETIKET, "kayit acilamadi", gorev.getException());
                    jsCagir("window.__pgIndir && window.__pgIndir(null)");
                    return;
                }
                try {
                    Snapshot anlik = gorev.getResult().getData();
                    if (anlik == null) {
                        jsCagir("window.__pgIndir && window.__pgIndir(null)");
                        return;
                    }
                    byte[] ham = anlik.getSnapshotContents().readFully();
                    // Dosyayi kapat, yoksa bir sonraki acma takilir.
                    istemci.discardAndClose(anlik);

                    if (ham == null || ham.length == 0) {
                        jsCagir("window.__pgIndir && window.__pgIndir(null)");
                        return;
                    }
                    String metin = new String(ham, StandardCharsets.UTF_8);
                    jsCagir("window.__pgIndir && window.__pgIndir(" + metinKacir(metin) + ")");
                } catch (IOException e) {
                    Log.w(ETIKET, "indirme hatasi", e);
                    jsCagir("window.__pgIndir && window.__pgIndir(null)");
                }
            });
    }

    /**
     * Kayit kapak resmi. WebView'in o anki gorunumunu kucultup verir.
     * Play Games bunu kayit listesinde gosterir - oyuncu hangi kaydin
     * hangisi oldugunu resimden anlar.
     */
    private Bitmap kapakCek() {
        try {
            if (web == null) return null;
            int g = web.getWidth(), y = web.getHeight();
            if (g <= 0 || y <= 0) return null;
            // Play Games kapak resmi icin 512x342 oneriyor
            Bitmap tam = Bitmap.createBitmap(g, y, Bitmap.Config.RGB_565);
            Canvas tuval = new Canvas(tam);
            web.draw(tuval);
            Bitmap kucuk = Bitmap.createScaledBitmap(tam, 512, 342, true);
            if (kucuk != tam) tam.recycle();
            return kucuk;
        } catch (Throwable t) {
            Log.w(ETIKET, "kapak cekilemedi", t);
            return null;   // kapak olmazsa kayit yine de yazilir
        }
    }

    /**
     * DIS BAGLANTI: gizlilik politikasi, destek e-postasi.
     * WebView kendi icinde hicbir sayfaya gitmez; adres telefonun
     * tarayicisinda / e-posta uygulamasinda acilir.
     * Yalnizca https ve mailto kabul edilir - sayfa baska bir sey
     * (intent://, file://, javascript:) isteyemez.
     */
    @JavascriptInterface
    public void disariAc(final String adres) {
        if (adres == null) return;
        final String a = adres.trim();
        String kucuk = a.toLowerCase(java.util.Locale.ROOT);
        if (!kucuk.startsWith("https://") && !kucuk.startsWith("mailto:")) return;
        etkinlik.runOnUiThread(() -> {
            try {
                Intent niyet = new Intent(Intent.ACTION_VIEW, Uri.parse(a));
                niyet.addCategory(Intent.CATEGORY_BROWSABLE);
                etkinlik.startActivity(niyet);
            } catch (ActivityNotFoundException e) {
                Log.w(ETIKET, "baglanti acacak uygulama yok", e);
            }
        });
    }

    // ======================================================================
    //  KAYIT AYNASI
    //
    //  Oyun kaydi WebView localStorage'inda durur (app_webview klasoru).
    //  Android'in otomatik yedegi o klasoru ALMIYOR - yedek kurallari
    //  sadece files / databases / shared_prefs'i kapsiyor. Yani "telefon
    //  degistirirsen ilerlemen geri gelir" vaadi tutmuyordu.
    //
    //  Cozum: her kayitta ayni JSON burada SharedPreferences'a da yazilir
    //  (yedege dahil). Yeni telefonda localStorage bossa oyun buradan okur.
    // ======================================================================
    private static final int KAYIT_SINIR = 1_000_000;   // 1 MB - normal kayit ~20 KB

    private SharedPreferences kayitDeposu() {
        return etkinlik.getSharedPreferences("prismio_kayit", Context.MODE_PRIVATE);
    }

    @JavascriptInterface
    public void kayitYaz(final String json) {
        if (json == null || json.isEmpty() || json.length() > KAYIT_SINIR) return;
        if (json.charAt(0) != '{') return;               // sadece JSON nesnesi
        kayitDeposu().edit().putString("kayit", json).apply();
    }

    @JavascriptInterface
    public String kayitOku() {
        try { return kayitDeposu().getString("kayit", null); }
        catch (Throwable t) { return null; }
    }

    // ======================================================================
    //  UYGULAMA ICI URUNLER (isik paketleri, can) - bkz. Odeme
    // ======================================================================
    @JavascriptInterface
    public void urunSatinAl(final String urunId) {
        if (odeme == null || urunId == null) {
            jsCagir("window.__urunSonuc && window.__urunSonuc(" + metinKacir(urunId) + ",false,null)");
            return;
        }
        etkinlik.runOnUiThread(() -> odeme.satinAl(urunId));
    }

    /** Oyun odulu verip kaydetti: satin alma tuketilir. */
    @JavascriptInterface
    public void teslimEdildi(final String anahtar) {
        if (odeme == null || anahtar == null) return;
        etkinlik.runOnUiThread(() -> odeme.teslimEdildi(anahtar));
    }

    // ======================================================================
    //  GUNLUK HATIRLATMA - bkz. Hatirlatma
    // ======================================================================
    @JavascriptInterface
    public void hatirlatmaAyarla(boolean aktif, String baslik, String metin, String bittiGun) {
        try { Hatirlatma.ayarla(etkinlik.getApplicationContext(), aktif, baslik, metin, bittiGun); }
        catch (Throwable t) { Log.w(ETIKET, "hatirlatma ayarlanamadi", t); }
    }

    /** "var" / "yok" */
    @JavascriptInterface
    public String bildirimIzni() {
        return (etkinlik instanceof MainActivity && !((MainActivity) etkinlik).bildirimIzniVar()) ? "yok" : "var";
    }

    @JavascriptInterface
    public void bildirimIzniIste() {
        if (etkinlik instanceof MainActivity)
            etkinlik.runOnUiThread(() -> ((MainActivity) etkinlik).bildirimIzniIste());
    }

    /** Hakkinda ekrani icin GERCEK surum: "0.9.3 (1023)". */
    @JavascriptInterface
    public String uygulamaSurumu() {
        try {
            android.content.pm.PackageInfo pi = etkinlik.getPackageManager()
                    .getPackageInfo(etkinlik.getPackageName(), 0);
            long kod = (android.os.Build.VERSION.SDK_INT >= 28) ? pi.getLongVersionCode() : pi.versionCode;
            return pi.versionName + " (" + kod + ")";
        } catch (Throwable t) {
            return null;
        }
    }

    /** Play Games kullanilabilir mi? (SDK varsa her zaman true) */
    @JavascriptInterface
    public boolean destekVar() {
        return true;
    }

    /**
     * PLUS DURUMU SOR  (D-107)
     * Oyun acilista ve "geri yukle" basilinca cagirir.
     * Cevap __plusDurum ile doner.
     */
    @JavascriptInterface
    public void plusDurum() {
        if (odeme == null) {
            jsCagir("window.__plusDurum && window.__plusDurum(false,null,false)");
            return;
        }
        etkinlik.runOnUiThread(() -> odeme.satinAlmalariSorgula());
    }

    /**
     * PLUS SATIN AL  (D-107)
     * Google Play satin alma ekranini acar.
     * Sonuc __plusSonuc ile doner.
     */
    @JavascriptInterface
    public void plusSatinAl() {
        if (odeme == null) {
            jsCagir("window.__plusSonuc && window.__plusSonuc(false,\"Satın alma kullanılamıyor.\")");
            return;
        }
        etkinlik.runOnUiThread(() -> odeme.satinAl());
    }

    /**
     * UYGULAMADAN CIK  (D-092)
     *
     * Android geri tusu ana menude iki kez basilinca buraya gelir.
     * finish() cagrilir - uygulama kapanir, sistem geri doner.
     *
     * NEDEN JAVA TARAFI: Tarayicidan bir sayfayi kapatmak
     * (window.close) WebView'da calismaz. Activity'yi kapatmak
     * gerekir.
     */
    @JavascriptInterface
    public void cikis() {
        etkinlik.runOnUiThread(etkinlik::finish);
    }

    // ======================================================================
    //  DUNYA SIRALAMASI  (D-128)
    //
    //  Rutbe merdiveni CEVRIMDISI calisir ve tek basina yeterlidir.
    //  Siralama EKSTRADIR: giris yapan oyuncu dunya listesinde gorunur.
    //  Giris yoksa hicbir sey bozulmaz - JS tarafi zaten kontrol ediyor.
    //
    //  YAYINDAN ONCE YAPILACAK:
    //  Play Console > Oyun hizmetleri > Siralamalar bolumunden
    //  "Toplam isik" adinda bir siralama olustur. Aldigin kimligi
    //  res/values/oyun_kimlikleri.xml icindeki pg_siralama_toplam_isik
    //  satirina yapistir. Kimlik yoksa metodlar sessizce hicbir sey
    //  yapmaz, oyun etkilenmez.
    // ======================================================================
    private String siralamaId() { return kimlik(R.string.pg_siralama_toplam_isik); }
    private static final int ISTEK_SIRALAMA = 9101;
    private static final int ISTEK_BASARI = 9102;

    private String kimlik(int kaynak) {
        try { return etkinlik.getString(kaynak).trim(); }
        catch (Throwable t) { return ""; }
    }

    // ======================================================================
    //  ODULLU REKLAM  (D-129)
    //  JS tarafi once reklamHazir() sorar. false ise reklamla ilgili
    //  hicbir arayuz gostermez - bos kutu da, calismayan dugme de yok.
    // ======================================================================

    /** Gosterilmeye hazir bir reklam var mi? */
    @JavascriptInterface
    public boolean reklamHazir() {
        return reklam != null && reklam.hazirMi();
    }

    /** Odullu reklami gosterir. Sonuc __reklamSonuc ile doner. */
    @JavascriptInterface
    public void reklamGoster() {
        if (reklam == null) {
            jsCagir("window.__reklamSonuc && window.__reklamSonuc(false)");
            return;
        }
        etkinlik.runOnUiThread(() -> reklam.goster());
    }

    /**
     * Siralama GERCEKTEN kurulu mu?
     * JS tarafi buna bakar: false ise siralama dugmesi hic gosterilmez.
     * Calismayan bir dugme gostermek oyuncuyu kandirmaktir.
     */
    @JavascriptInterface
    public boolean siralamaHazir() {
        return !siralamaId().isEmpty();
    }

    /** Omur boyu kazanilan isigi siralamaya yazar. Sessiz calisir. */
    @JavascriptInterface
    public void siralamaGonder(final long puan) {
        if (siralamaId().isEmpty() || puan < 0) return;
        etkinlik.runOnUiThread(() -> {
            try {
                LeaderboardsClient istemci = PlayGames.getLeaderboardsClient(etkinlik);
                istemci.submitScore(siralamaId(), puan);
            } catch (Throwable t) {
                // Giris yoksa veya Play Hizmetleri eskiyse buraya duser.
                // Siralama SUS bir ozellik - oyunu durdurmaz.
                Log.w(ETIKET, "siralama gonderilemedi", t);
            }
        });
    }

    /** Siralama ekranini acar. Giris yoksa once girisi ister. */
    @JavascriptInterface
    public void siralamaAc() {
        if (siralamaId().isEmpty()) return;
        etkinlik.runOnUiThread(() -> {
            try {
                PlayGames.getLeaderboardsClient(etkinlik)
                        .getLeaderboardIntent(siralamaId())
                        .addOnSuccessListener(niyet ->
                                etkinlik.startActivityForResult(niyet, ISTEK_SIRALAMA))
                        .addOnFailureListener(hata -> {
                            Log.w(ETIKET, "siralama ekrani acilamadi", hata);
                            // Muhtemelen giris yok. Girisi baslat.
                            try {
                                GamesSignInClient g = PlayGames.getGamesSignInClient(etkinlik);
                                g.signIn();
                            } catch (Throwable ignored) { }
                        });
            } catch (Throwable t) {
                Log.w(ETIKET, "siralama acilamadi", t);
            }
        });
    }

    // ======================================================================
    //  BASARILAR
    //
    //  Oyun kendi basarilarini CEVRIMDISI tutar (Odul Odasi). Giris yapan
    //  oyuncunun acilan basarilari Play Games'e de yazilir.
    //  unlock() ayni basari icin tekrar cagrilirsa zararsizdir; bu yuzden
    //  giris sonrasi JS tarafi butun acik basarilari bir kez gonderir -
    //  giristen once kazanilanlar da Play Games'e gecer.
    //  Kimlikler res/values/oyun_kimlikleri.xml icinde. Bos olan atlanir.
    // ======================================================================
    private static final Map<String, Integer> BASARI_KAYNAK = new HashMap<>();
    static {
        BASARI_KAYNAK.put("a-ilk",     R.string.pg_a_ilk);
        BASARI_KAYNAK.put("a-bes",     R.string.pg_a_bes);
        BASARI_KAYNAK.put("a-mile",    R.string.pg_a_mile);
        BASARI_KAYNAK.put("a-gezgin",  R.string.pg_a_gezgin);
        BASARI_KAYNAK.put("a-200",     R.string.pg_a_200);
        BASARI_KAYNAK.put("a-500",     R.string.pg_a_500);
        BASARI_KAYNAK.put("a-1000",    R.string.pg_a_1000);
        BASARI_KAYNAK.put("a-ilkuc",   R.string.pg_a_ilkuc);
        BASARI_KAYNAK.put("a-usta",    R.string.pg_a_usta);
        BASARI_KAYNAK.put("a-mukem",   R.string.pg_a_mukem);
        BASARI_KAYNAK.put("a-y100",    R.string.pg_a_y100);
        BASARI_KAYNAK.put("a-zaman",   R.string.pg_a_zaman);
        BASARI_KAYNAK.put("a-buyuk",   R.string.pg_a_buyuk);
        BASARI_KAYNAK.put("s-3",       R.string.pg_s_3);
        BASARI_KAYNAK.put("s-7",       R.string.pg_s_7);
        BASARI_KAYNAK.put("s-30",      R.string.pg_s_30);
        BASARI_KAYNAK.put("s-100",     R.string.pg_s_100);
        BASARI_KAYNAK.put("a-kolek",   R.string.pg_a_kolek);
        BASARI_KAYNAK.put("a-temaall", R.string.pg_a_temaall);
        BASARI_KAYNAK.put("a-deko",    R.string.pg_a_deko);
        BASARI_KAYNAK.put("a-dekoall", R.string.pg_a_dekoall);
    }

    /** En az bir basari kimligi girilmis mi? false ise JS dugmeyi gostermez. */
    @JavascriptInterface
    public boolean basariHazir() {
        for (Integer k : BASARI_KAYNAK.values()) if (!kimlik(k).isEmpty()) return true;
        return false;
    }

    /**
     * Oyundaki basari kimliklerini (virgulle ayrilmis, ornek "a-ilk,s-3")
     * Play Games'te acar. Giris yoksa SDK sessizce reddeder - oyun etkilenmez.
     */
    @JavascriptInterface
    public void basariAc(final String oyunIdleri) {
        if (oyunIdleri == null || oyunIdleri.isEmpty()) return;
        etkinlik.runOnUiThread(() -> {
            try {
                AchievementsClient istemci = PlayGames.getAchievementsClient(etkinlik);
                for (String id : oyunIdleri.split(",")) {
                    Integer k = BASARI_KAYNAK.get(id.trim());
                    if (k == null) continue;
                    String pg = kimlik(k);
                    if (!pg.isEmpty()) istemci.unlock(pg);
                }
            } catch (Throwable t) {
                Log.w(ETIKET, "basari gonderilemedi", t);
            }
        });
    }

    /** Play Games basari ekranini acar. Giris yoksa once girisi baslatir. */
    @JavascriptInterface
    public void basarilarAc() {
        etkinlik.runOnUiThread(() -> {
            try {
                PlayGames.getAchievementsClient(etkinlik)
                        .getAchievementsIntent()
                        .addOnSuccessListener(niyet ->
                                etkinlik.startActivityForResult(niyet, ISTEK_BASARI))
                        .addOnFailureListener(hata -> {
                            Log.w(ETIKET, "basari ekrani acilamadi", hata);
                            try { PlayGames.getGamesSignInClient(etkinlik).signIn(); }
                            catch (Throwable ignored) { }
                        });
            } catch (Throwable t) {
                Log.w(ETIKET, "basarilar acilamadi", t);
            }
        });
    }
}
