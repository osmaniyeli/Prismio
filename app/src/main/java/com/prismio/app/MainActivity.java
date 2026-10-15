package com.prismio.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Prismio - tek dosyalik HTML oyunu calistiran kabuk.
 *
 * GUVENLIK YAKLASIMI:
 *  1. WEBVIEW AGA CIKAMAZ. Her istek engellenir, sadece yerel dosya acilir.
 *     Uygulamanin INTERNET izni var ama o izin sadece Play Games icin.
 *     Oyun sayfasi tek bir dis istek bile yapamaz.
 *  2. JavaScript koprusu SINIRLI. Sadece dort metot: durum, giris,
 *     yedekle, indir. Dosya/sistem erisimi vermiyor. Gelen veri kod
 *     olarak calistirilmiyor, sadece kayit dosyasina yaziliyor.
 *  3. Dosya erisimi kapali. WebView yerel dosya sistemine bakamaz.
 *  4. Hata ayiklama kapali.
 *  5. Ekran goruntusu serbest - oyun, gizli veri yok.
 */
public class MainActivity extends AppCompatActivity {

    private WebView web;
    private OyunKopru kopru;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle kayit) {
        super.onCreate(kayit);

        web = new WebView(this);
        setContentView(web);

        WebSettings a = web.getSettings();

        // Oyun JavaScript ile calisiyor, sart.
        a.setJavaScriptEnabled(true);

        // Kayit icin localStorage gerekli.
        a.setDomStorageEnabled(true);

        // --- GUVENLIK KISITLARI ---
        a.setAllowFileAccess(false);              // yerel dosya sistemine bakamaz
        a.setAllowContentAccess(false);           // content:// saglayicilara bakamaz
        a.setGeolocationEnabled(false);           // konum yok
        a.setJavaScriptCanOpenWindowsAutomatically(false);
        a.setSupportMultipleWindows(false);
        a.setDatabaseEnabled(false);
        a.setSaveFormData(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            a.setSafeBrowsingEnabled(false);      // ag yok, gereksiz
        }
        // Karma icerik yasak (zaten ag yok, ikinci kat koruma)
        a.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Olcek: oyun kendi viewport'unu yonetiyor
        a.setUseWideViewPort(true);
        a.setLoadWithOverviewMode(true);
        a.setSupportZoom(false);
        a.setBuiltInZoomControls(false);
        a.setDisplayZoomControls(false);
        a.setTextZoom(100);                       // sistem yazi boyutu oyunu bozmasin

        // Kullanici verisi onbellege yazilmasin
        a.setCacheMode(WebSettings.LOAD_NO_CACHE);

        web.setBackgroundColor(Color.parseColor("#F2EDE4"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setLongClickable(false);
        web.setHapticFeedbackEnabled(true);
        // Uzun basinca metin secme menusu cikmasin
        web.setOnLongClickListener(v -> true);

        // Hata ayiklama KAPALI (yayin surumu)
        WebView.setWebContentsDebuggingEnabled(false);

        // --- PLAY GAMES KOPRUSU ---
        // Sadece bizim kendi sayfamiz yuklendigi icin guvenli.
        // Bkz. shouldInterceptRequest: her dis istek engelleniyor.
        kopru = new OyunKopru(this, web, getString(R.string.kayit_dosya_adi));
        kopru.odemeyiBagla();     // D-107: Prismio Plus satin alma
        kopru.reklamiBagla();     // D-129: odullu reklam (kimlik bossa hicbir sey yapmaz)
        web.addJavascriptInterface(kopru, "PrismioNative");

        web.setWebViewClient(new WebViewClient() {

            // Oyunun kendi dosyasi disinda HICBIR sey yuklenmez.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String u = r.getUrl() != null ? r.getUrl().toString() : "";
                if (u.startsWith("file:///android_asset/")) {
                    return null;                   // izin ver
                }
                // Yazi tipi dahil her sey engellenir. Oyun sistem yazi tipine duser.
                return new WebResourceResponse("text/plain", "utf-8", null);
            }

            // Baglantiya basilsa bile disari cikilmaz.
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return true;                       // her gezinme engellenir
            }

            // Sayfa hazir: oyuna "Play Games var" de.
            @Override
            public void onPageFinished(WebView v, String url) {
                v.evaluateJavascript("window.__pgHazir && window.__pgHazir()", null);
            }
        });

        // Ekran acik kalsin (bulmaca dusunurken sonmesin)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web.loadUrl("file:///android_asset/index.html");
        geriTusunuBagla();
        // Bildirim izni: oyun ekrani gorundukten kisa sure sonra, sadece ilk kez sorulur.
        web.postDelayed(this::bildirimIzniIlkSefer, 1800);
    }

    /**
     * Geri tusu: oyun icinde geri git, menudeyse uygulamadan cik.
     * targetSdk 36 (Android 16) ile sistem onBackPressed()'i ARTIK CAGIRMIYOR
     * (ongorulu geri hareketi). Eski override o surumde hic calismaz,
     * geri tusu oyunu dogrudan kapatirdi. OnBackPressedDispatcher her
     * surumde calisir.
     */
    private void geriTusunuBagla() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { geriBasildi(); }
        });
    }

    private void geriBasildi() {
        if (web == null) { finish(); return; }
        // Butun geri mantigi oyunda (geriGit): acik pencereyi kapatir, ust
        // ekrana doner, menude "cikmak icin tekrar dokun" der ve ikinci
        // basista PrismioNative.cikis() ile kapatir. Sayfa yuklenmediyse cik.
        web.evaluateJavascript(
            "(window.__geriTus ? window.__geriTus() : 'cik')",
            deger -> {
                if (deger == null || deger.contains("cik")) finish();
            });
    }

    // ======================================================================
    //  BILDIRIM IZNI (Android 13+). Ilk acilista bir kez sorulur; reddedilirse
    //  tekrar tekrar sorulmaz. Ayarlardan "Gunluk hatirlatma" acilinca yeniden istenir.
    // ======================================================================
    static final int ISTEK_BILDIRIM = 7001;

    boolean bildirimIzniVar() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void bildirimIzniIlkSefer() {
        android.content.SharedPreferences d = getSharedPreferences("prismio_hatirlatma", MODE_PRIVATE);
        if (d.getBoolean("izinSoruldu", false) || bildirimIzniVar()) return;
        d.edit().putBoolean("izinSoruldu", true).apply();
        bildirimIzniIste();
    }

    void bildirimIzniIste() {
        if (bildirimIzniVar()) { izinSonucu(true); return; }
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, ISTEK_BILDIRIM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int istek, String[] izinler, int[] sonuclar) {
        super.onRequestPermissionsResult(istek, izinler, sonuclar);
        if (istek == ISTEK_BILDIRIM) {
            boolean ok = sonuclar.length > 0 && sonuclar[0] == PackageManager.PERMISSION_GRANTED;
            izinSonucu(ok);
        }
    }

    private void izinSonucu(boolean ok) {
        if (web != null) web.evaluateJavascript("window.__bildirimIzni && window.__bildirimIzni(" + ok + ")", null);
    }

    @Override protected void onPause()  { super.onPause();  web.onPause();  }
    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        // D-107: Her one gelisinde satin alma durumu tazelenir.
        // Oyuncu Play'den iade aldiysa veya baska cihazda satin aldiysa
        // burada yakalanir. Play Billing bunu SART kosuyor.
        if (kopru != null) kopru.plusDurum();
    }

    @Override
    protected void onDestroy() {
        if (kopru != null) kopru.odemeyiKapat();   // D-107: baglantiyi sizdirma
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
