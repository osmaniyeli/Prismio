package com.prismio.app;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * ÖDÜLLÜ REKLAM  (D-129)
 *
 * NEDEN SADECE ÖDÜLLÜ REKLAM:
 * Projenin monetizasyon kuralı: "Zorla reklam yok, sürekli açılır pencere yok."
 * Geçiş reklamı (interstitial) oyuncuyu bölerdi. Ödüllü reklam ise oyuncunun
 * KENDİ İSTEĞİYLE izlediği bir takastır: izle, ışık/can kazan.
 *
 * KİMLİKLER BOŞKEN NE OLUR:
 * hazirMi() false döner. JS tarafı bunu okur ve:
 *   - reklam alanı kutularını gizler
 *   - "Reklam izle" ürünlerini mağazada göstermez
 * Yani yarım bir özellik oyuncuya HİÇ görünmez. Kimlikler girilince
 * her şey kendiliğinden açılır.
 *
 * YAYINDAN ÖNCE YAPILACAK:
 *   1. admob.google.com > uygulama ekle > Prismio'yu Play paket adıyla bağla
 *   2. "Ödüllü" tipinde bir reklam birimi oluştur
 *   3. Aşağıdaki BIRIM_ID'ye o kimliği yapıştır
 *   4. AndroidManifest.xml'e APPLICATION_ID meta-data satırını ekle
 *   5. Gizlilik politikası + Play "Veri güvenliği" formu GÜNCELLENMELİ
 *      (reklam kimliği toplanmaya başlıyor - yanlış beyan askıya aldırır)
 */
public class Reklam {

    private static final String ETIKET = "PrismioReklam";

    /** Play Console/AdMob'dan gelecek. BOŞ = reklam sistemi kapalı. */
    private static final String BIRIM_ID = "";

    /** Google'ın resmi TEST kimliği. Geliştirme sırasında BIRIM_ID yerine
     *  bunu kullanabilirsin - gerçek reklam yerine test reklamı gelir.
     *  UYARI: yayına bununla çıkma, AdMob hesabını askıya alır. */
    @SuppressWarnings("unused")
    private static final String TEST_BIRIM_ID = "ca-app-pub-3940256099942544/5224354917";

    public interface Geri {
        /** tamam=true -> oyuncu sonuna kadar izledi, ödül hak edildi */
        void bitti(boolean tamam);
        /** reklam yüklendi/yüklenemedi - JS tarafı durumu tazeler */
        void durumDegisti();
    }

    private final Activity etkinlik;
    private final Geri geri;

    private RewardedAd reklam;
    private boolean yukleniyor = false;
    private boolean baslatildi = false;

    public Reklam(Activity etkinlik, Geri geri) {
        this.etkinlik = etkinlik;
        this.geri = geri;
    }

    /** Kimlik yoksa hiç başlatma - boşuna ağ isteği yapma. */
    public void basla() {
        if (BIRIM_ID.isEmpty() || baslatildi) return;
        baslatildi = true;
        try {
            MobileAds.initialize(etkinlik, durum -> yukle());
        } catch (Throwable t) {
            Log.w(ETIKET, "AdMob baslatilamadi", t);
        }
    }

    /** Gösterilmeye hazır bir reklam var mı? */
    public boolean hazirMi() {
        return !BIRIM_ID.isEmpty() && reklam != null;
    }

    /** Bir sonraki reklamı önceden yükler. Oyuncu bekletilmesin diye. */
    private void yukle() {
        if (BIRIM_ID.isEmpty() || yukleniyor || reklam != null) return;
        yukleniyor = true;
        try {
            RewardedAd.load(etkinlik, BIRIM_ID, new AdRequest.Builder().build(),
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
                    }
                });
        } catch (Throwable t) {
            yukleniyor = false;
            Log.w(ETIKET, "reklam yukleme hatasi", t);
        }
    }

    /**
     * Reklamı gösterir.
     *
     * ÖDÜL KURALI: Ödül SADECE onUserEarnedReward geldiğinde verilir.
     * Oyuncu reklamı yarıda kapatırsa ödül YOKTUR. Bu AdMob'un kuralı
     * ve doğrusu da budur - yoksa reklam izlemeden ödül alınır.
     */
    public void goster() {
        if (reklam == null) { if (geri != null) geri.bitti(false); return; }
        final boolean[] kazandi = {false};
        RewardedAd r = reklam;
        reklam = null;                   // aynı reklam iki kez gösterilemez

        r.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (geri != null) geri.bitti(kazandi[0]);
                yukle();                 // sonraki için hazırlan
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
}
