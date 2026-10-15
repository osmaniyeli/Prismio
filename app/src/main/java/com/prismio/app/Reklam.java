package com.prismio.app;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REKLAMLAR (AdMob)
 *
 * IKI TUR, IKISI DE OYUNCUYU BOLMEZ:
 *   Odullu : oyuncu KENDI ister ("izle, isik/can kazan"). Odul yalnizca
 *            sonuna kadar izlenince verilir.
 *   Banner : menu / harita / magaza gibi ekranlarin EN ALTINDA ince serit.
 *            Oyun ekraninda ve Plus sahiplerinde gosterilmez (JS karar verir).
 *   Gecis (tam ekran zorla) reklam YOK - projenin kurali.
 *
 * KIMLIKLER: res/values/reklam_kimlikleri.xml. Birim kimligi bossa o tur
 *   reklam hic istenmez, JS de arayuzunu gostermez.
 *
 * ONAY (GDPR / UMP): AB ve Birlesik Krallik'ta reklamdan ONCE izin penceresi
 *   gosterilmesi zorunlu. Pencerenin metni AdMob > Gizlilik ve mesajlasma
 *   bolumunde ayarlanir. Onay alinmadan (canRequestAds false) hicbir reklam
 *   istegi yapilmaz. Oyuncu kararini Hakkinda > Gizlilik secenekleri'nden degistirir.
 */
public class Reklam {

    private static final String ETIKET = "PrismioReklam";

    public interface Geri {
        /** tamam=true -> oyuncu sonuna kadar izledi, odul hak edildi */
        void bitti(boolean tamam);
        /** reklam yuklendi/yuklenemedi - JS tarafi durumu tazeler */
        void durumDegisti();
    }

    private final Activity etkinlik;
    private final Geri geri;
    private final String odulluId, bannerId;

    private ConsentInformation onay;
    private final AtomicBoolean sdkBaslatildi = new AtomicBoolean(false);
    private RewardedAd reklam;
    private boolean yukleniyor = false;

    private FrameLayout bannerKutu;
    private AdView banner;
    private boolean bannerIstendi = false;

    public Reklam(Activity etkinlik, Geri geri) {
        this.etkinlik = etkinlik;
        this.geri = geri;
        this.odulluId = kimlik(R.string.reklam_odullu);
        this.bannerId = kimlik(R.string.reklam_banner);
    }

    private String kimlik(int k) {
        try { return etkinlik.getString(k).trim(); } catch (Throwable t) { return ""; }
    }

    private boolean acik() { return !odulluId.isEmpty() || !bannerId.isEmpty(); }

    /** MainActivity banner'in duracagi alani verir. */
    public void bannerKutusu(FrameLayout kutu) { this.bannerKutu = kutu; }

    // ------------------------------------------------------------------
    //  BASLATMA: once onay, sonra SDK
    // ------------------------------------------------------------------
    public void basla() {
        if (!acik()) return;                     // kimlik yok: hic ag istegi yapma
        try {
            onay = UserMessagingPlatform.getConsentInformation(etkinlik);
            ConsentRequestParameters p = new ConsentRequestParameters.Builder().build();
            onay.requestConsentInfoUpdate(etkinlik, p,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(etkinlik, hata -> {
                    if (hata != null) Log.w(ETIKET, "onay formu: " + hata.getMessage());
                    if (onay.canRequestAds()) sdkBaslat();
                    if (geri != null) geri.durumDegisti();
                }),
                hata -> Log.w(ETIKET, "onay bilgisi alinamadi: " + hata.getMessage()));
            // Onceki oturumdan onay varsa beklemeden basla
            if (onay.canRequestAds()) sdkBaslat();
        } catch (Throwable t) {
            Log.w(ETIKET, "reklam baslatilamadi", t);
        }
    }

    private void sdkBaslat() {
        if (sdkBaslatildi.getAndSet(true)) return;
        try {
            MobileAds.initialize(etkinlik, durum -> {
                yukle();
                if (bannerIstendi) bannerAc();
            });
        } catch (Throwable t) {
            Log.w(ETIKET, "AdMob baslatilamadi", t);
        }
    }

    /** Gizlilik secenekleri dugmesi gerekli mi (AB/BK'da onay verilmisse evet). */
    public boolean gizlilikGerekli() {
        return onay != null && onay.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void gizlilikAc() {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(etkinlik, hata -> {
                if (hata != null) Log.w(ETIKET, "gizlilik formu: " + hata.getMessage());
                if (onay != null && onay.canRequestAds()) sdkBaslat();
            });
        } catch (Throwable t) { Log.w(ETIKET, "gizlilik formu acilamadi", t); }
    }

    // ------------------------------------------------------------------
    //  ODULLU REKLAM
    // ------------------------------------------------------------------
    public boolean hazirMi() { return !odulluId.isEmpty() && reklam != null; }

    private void yukle() {
        if (odulluId.isEmpty() || yukleniyor || reklam != null || !sdkBaslatildi.get()) return;
        yukleniyor = true;
        try {
            RewardedAd.load(etkinlik, odulluId, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd r) {
                        reklam = r; yukleniyor = false;
                        if (geri != null) geri.durumDegisti();
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError hata) {
                        reklam = null; yukleniyor = false;
                        Log.w(ETIKET, "reklam yuklenemedi: " + hata.getMessage());
                        if (geri != null) geri.durumDegisti();
                        // Doluluk yoksa bir dakika sonra yeniden dene
                        if (bannerKutu != null) bannerKutu.postDelayed(Reklam.this::yukle, 60_000);
                    }
                });
        } catch (Throwable t) {
            yukleniyor = false;
            Log.w(ETIKET, "reklam yukleme hatasi", t);
        }
    }

    /** Odul SADECE onUserEarnedReward gelince verilir; yarida kapatan odul almaz. */
    public void goster() {
        if (reklam == null) { if (geri != null) geri.bitti(false); return; }
        final boolean[] kazandi = {false};
        RewardedAd r = reklam;
        reklam = null;                   // ayni reklam iki kez gosterilemez
        r.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (geri != null) geri.bitti(kazandi[0]);
                yukle();
            }
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError hata) {
                Log.w(ETIKET, "reklam gosterilemedi: " + hata.getMessage());
                if (geri != null) geri.bitti(false);
                yukle();
            }
        });
        try {
            r.show(etkinlik, odul -> kazandi[0] = true);
        } catch (Throwable t) {
            Log.w(ETIKET, "reklam show hatasi", t);
            if (geri != null) geri.bitti(false);
            yukle();
        }
    }

    // ------------------------------------------------------------------
    //  BANNER
    // ------------------------------------------------------------------
    public boolean bannerVar() { return !bannerId.isEmpty(); }

    /** JS ekran degisince cagirir: goster=true menu vb., false oyun ekrani / Plus. */
    public void banner(boolean goster) {
        bannerIstendi = goster;
        if (bannerKutu == null || bannerId.isEmpty()) return;
        if (!goster) { bannerKutu.setVisibility(View.GONE); return; }
        if (sdkBaslatildi.get()) bannerAc();
    }

    private void bannerAc() {
        if (bannerKutu == null || bannerId.isEmpty() || !bannerIstendi) return;
        try {
            if (banner == null) {
                banner = new AdView(etkinlik);
                banner.setAdUnitId(bannerId);
                DisplayMetrics dm = etkinlik.getResources().getDisplayMetrics();
                int genislikDp = (int) (dm.widthPixels / dm.density);
                banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(etkinlik, genislikDp));
                banner.setAdListener(new AdListener() {
                    @Override public void onAdLoaded() { if (bannerIstendi) bannerKutu.setVisibility(View.VISIBLE); }
                    @Override public void onAdFailedToLoad(@NonNull LoadAdError h) {
                        Log.w(ETIKET, "banner yuklenemedi: " + h.getMessage());
                        bannerKutu.setVisibility(View.GONE);      // bos serit birakma
                    }
                });
                bannerKutu.removeAllViews();
                bannerKutu.addView(banner);
                banner.loadAd(new AdRequest.Builder().build());
            } else {
                bannerKutu.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            Log.w(ETIKET, "banner acilamadi", t);
        }
    }

    public void duraklat() { if (banner != null) banner.pause(); }
    public void devam()    { if (banner != null) banner.resume(); }
    public void kapat()    { if (banner != null) { banner.destroy(); banner = null; } }
}
