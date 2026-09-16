package com.prismio.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
 *    2. Koprude sadece DORT metot var, hicbiri dosya/sistem erisimi vermiyor.
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

    public OyunKopru(Activity etkinlik, WebView web, String kayitAdi) {
        this.etkinlik = etkinlik;
        this.web = web;
        this.kayitAdi = kayitAdi;
    }

    /** MainActivity satin alma sistemini burada baglar. */
    public void odemeyiBagla() {
        if (odeme != null) return;
        odeme = new Odeme(etkinlik, new Odeme.Geri() {
            @Override
            public void durum(boolean sahip, String fiyat, boolean hazir) {
                jsCagir("window.__plusDurum && window.__plusDurum("
                        + sahip + "," + metinKacir(fiyat) + "," + hazir + ")");
            }
            @Override
            public void sonuc(boolean basarili, String mesaj) {
                jsCagir("window.__plusSonuc && window.__plusSonuc("
                        + basarili + "," + metinKacir(mesaj) + ")");
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

    private static String metinKacir(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    /** Oyun aciliyor: zaten giris yapilmis mi? */
    @JavascriptInterface
    public void durumSor() {
        GamesSignInClient istemci = PlayGames.getGamesSignInClient(etkinlik);
        istemci.isAuthenticated().addOnCompleteListener(gorev -> {
            boolean girisli = gorev.isSuccessful()
                    && gorev.getResult() != null
                    && gorev.getResult().isAuthenticated();
            jsCagir("window.__pgDurum && window.__pgDurum(" + girisli + ")");
        });
    }

    /** Oyuncu "Giris yap" dedi. */
    @JavascriptInterface
    public void girisYap() {
        GamesSignInClient istemci = PlayGames.getGamesSignInClient(etkinlik);
        istemci.signIn().addOnCompleteListener(gorev -> {
            boolean ok = gorev.isSuccessful()
                    && gorev.getResult() != null
                    && gorev.getResult().isAuthenticated();
            jsCagir("window.__pgGiris && window.__pgGiris(" + ok + ")");
        });
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
        SnapshotsClient istemci = PlayGames.getSnapshotsClient(etkinlik);
        istemci.open(kayitAdi, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener(acmaGorevi -> {
                if (!acmaGorevi.isSuccessful() || acmaGorevi.getResult() == null) {
                    jsCagir("window.__pgYedek && window.__pgYedek(false)");
                    return;
                }
                try {
                    Snapshot anlik = acmaGorevi.getResult().getData();
                    if (anlik == null) {
                        jsCagir("window.__pgYedek && window.__pgYedek(false)");
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
                        .addOnCompleteListener(yazma ->
                            jsCagir("window.__pgYedek && window.__pgYedek("
                                    + yazma.isSuccessful() + ")"));
                } catch (Exception e) {
                    Log.w(ETIKET, "yedekleme hatasi", e);
                    jsCagir("window.__pgYedek && window.__pgYedek(false)");
                }
            });
    }

    /** Buluttaki ilerlemeyi oku. */
    @JavascriptInterface
    public void indir() {
        SnapshotsClient istemci = PlayGames.getSnapshotsClient(etkinlik);
        istemci.open(kayitAdi, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener(gorev -> {
                if (!gorev.isSuccessful() || gorev.getResult() == null) {
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
}
