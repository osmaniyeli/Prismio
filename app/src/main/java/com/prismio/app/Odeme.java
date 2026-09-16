package com.prismio.app;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * PRISMIO PLUS — TEK SEFERLIK SATIN ALMA  (D-107)
 *
 * NE YAPAR:
 *   1. Google Play faturalandirmaya baglanir
 *   2. Urunun fiyatini ogrenir (yerel para biriminde)
 *   3. Satin alma ekranini acar
 *   4. Onceki satin almalari geri yukler
 *
 * NEDEN ABONELIK DEGIL:
 *   Bulmaca oyunu her ay yeni deger uretmez. Tek seferlik odeme
 *   oyuncunun guvenini kazanir. Uzun vadede daha karli.
 *
 * ONEMLI - ONAYLAMA (acknowledge):
 *   Google Play, satin alma 3 GUN icinde onaylanmazsa parayi
 *   OTOMATIK IADE eder. Bu yuzden onaylama zorunlu, atlanamaz.
 *
 * TUKETILMEZ (non-consumable):
 *   Plus bir kez alinir, hep kalir. consumeAsync CAGRILMAZ.
 *   Cagrilsaydi oyuncu ayni seyi tekrar tekrar satin alabilirdi.
 *
 * GUVENLIK SINIRI - DURUSTCE:
 *   Dogrulama CIHAZ uzerinde yapiliyor. Sunucu tarafli dogrulama
 *   (Google Play Developer API) icin backend gerekir. Tek gelistirici
 *   icin bu altyapinin maliyeti, kaybedilecek gelirden yuksek.
 *   Bilincli bir karar - teknik borc degil.
 */
public class Odeme {

    private static final String ETIKET = "PrismioOdeme";

    /**
     * Play Console'da olusturulacak urun kimligi.
     * Console -> Para kazanma -> Uygulama ici urunler -> Urun olustur
     * Urun tipi: "Tek seferlik urun" (one-time product)
     */
    public static final String URUN_ID = "prismio_plus";

    private final Activity etkinlik;
    private final Geri geri;

    private BillingClient istemci;
    private ProductDetails urun;
    private boolean bagli = false;
    private boolean sahip = false;

    /** Oyun tarafina haber vermek icin. */
    public interface Geri {
        /** durum degisti: sahip mi, fiyat metni, satin alma hazir mi */
        void durum(boolean sahip, String fiyat, boolean hazir);
        /** satin alma sonucu */
        void sonuc(boolean basarili, String mesaj);
    }

    public Odeme(Activity etkinlik, Geri geri) {
        this.etkinlik = etkinlik;
        this.geri = geri;
    }

    // ------------------------------------------------------------------
    //  BAGLANTI
    // ------------------------------------------------------------------

    public void basla() {
        if (istemci != null) return;

        istemci = BillingClient.newBuilder(etkinlik)
                .setListener(satinAlmaDinleyici)
                .enablePendingPurchases()
                .build();

        baglan();
    }

    private void baglan() {
        istemci.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult sonuc) {
                bagli = (sonuc.getResponseCode() == BillingClient.BillingResponseCode.OK);
                if (bagli) {
                    urunBilgisiAl();
                    satinAlmalariSorgula();
                } else {
                    Log.w(ETIKET, "baglanti kurulamadi: " + sonuc.getDebugMessage());
                    haberVer();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Baglanti koptu. Bir sonraki istekte tekrar denenecek.
                bagli = false;
                haberVer();
            }
        });
    }

    // ------------------------------------------------------------------
    //  URUN BILGISI (fiyat)
    // ------------------------------------------------------------------

    private void urunBilgisiAl() {
        QueryProductDetailsParams.Product p = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(URUN_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        QueryProductDetailsParams istek = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(p))
                .build();

        istemci.queryProductDetailsAsync(istek, (sonuc, liste) -> {
            if (sonuc.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && liste != null && !liste.isEmpty()) {
                urun = liste.get(0);
            } else {
                Log.w(ETIKET, "urun bulunamadi: " + sonuc.getDebugMessage());
            }
            haberVer();
        });
    }

    // ------------------------------------------------------------------
    //  ONCEKI SATIN ALMALAR (geri yukleme)
    // ------------------------------------------------------------------

    /** Oyuncu telefon degistirdiyse veya uygulamayi sildiyse burasi bulur. */
    public void satinAlmalariSorgula() {
        if (istemci == null || !bagli) { haberVer(); return; }

        QueryPurchasesParams istek = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        istemci.queryPurchasesAsync(istek, (sonuc, liste) -> {
            boolean bulundu = false;
            if (liste != null) {
                for (Purchase sa : liste) {
                    if (gecerliMi(sa)) {
                        bulundu = true;
                        onayla(sa);          // onaylanmamissa onayla
                    }
                }
            }
            sahip = bulundu;
            haberVer();
        });
    }

    private boolean gecerliMi(Purchase sa) {
        return sa.getProducts().contains(URUN_ID)
                && sa.getPurchaseState() == Purchase.PurchaseState.PURCHASED;
    }

    // ------------------------------------------------------------------
    //  SATIN ALMA
    // ------------------------------------------------------------------

    public void satinAl() {
        if (istemci == null || !bagli) {
            geri.sonuc(false, "Google Play'e bağlanılamadı.");
            baglan();
            return;
        }
        if (urun == null) {
            geri.sonuc(false, "Ürün bilgisi alınamadı. Sonra tekrar dene.");
            urunBilgisiAl();
            return;
        }
        if (sahip) {
            geri.sonuc(true, null);
            return;
        }

        BillingFlowParams.ProductDetailsParams pd =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(urun)
                        .build();

        BillingFlowParams akis = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pd))
                .build();

        BillingResult sonuc = istemci.launchBillingFlow(etkinlik, akis);
        if (sonuc.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            geri.sonuc(false, "Satın alma ekranı açılamadı.");
        }
    }

    private final PurchasesUpdatedListener satinAlmaDinleyici = (sonuc, liste) -> {
        int kod = sonuc.getResponseCode();

        if (kod == BillingClient.BillingResponseCode.OK && liste != null) {
            boolean oldu = false;
            for (Purchase sa : liste) {
                if (gecerliMi(sa)) { onayla(sa); oldu = true; }
            }
            if (oldu) { sahip = true; geri.sonuc(true, null); haberVer(); }
            return;
        }

        if (kod == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Oyuncu vazgecti. Hata degil, mesaj gostermiyoruz.
            geri.sonuc(false, null);
            return;
        }

        if (kod == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            sahip = true;
            satinAlmalariSorgula();
            geri.sonuc(true, null);
            return;
        }

        geri.sonuc(false, "Satın alma tamamlanamadı.");
    };

    // ------------------------------------------------------------------
    //  ONAYLAMA — ATLANAMAZ
    //  Onaylanmayan satin alma 3 gun sonra OTOMATIK IADE edilir.
    // ------------------------------------------------------------------

    private void onayla(Purchase sa) {
        if (sa.isAcknowledged()) return;

        AcknowledgePurchaseParams istek = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(sa.getPurchaseToken())
                .build();

        istemci.acknowledgePurchase(istek, sonuc -> {
            if (sonuc.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(ETIKET, "onaylanamadi: " + sonuc.getDebugMessage());
            }
        });
    }

    // ------------------------------------------------------------------

    private void haberVer() {
        String fiyat = null;
        if (urun != null && urun.getOneTimePurchaseOfferDetails() != null) {
            fiyat = urun.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }
        geri.durum(sahip, fiyat, bagli && urun != null);
    }

    public void kapat() {
        if (istemci != null) {
            istemci.endConnection();
            istemci = null;
            bagli = false;
        }
    }
}
