package com.prismio.app;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GOOGLE PLAY SATIN ALMA
 *
 * URUNLER (Play Console'da tanimli, kimlikler birebir ayni olmali):
 *   prismio_plus  KALICI      Reklamsiz + sinirsiz can + 4 ipucu. Bir kez alinir.
 *   isik_500      TUKETILIR   500 isik
 *   isik_1500     TUKETILIR   1500 isik
 *   isik_4000     TUKETILIR   4000 isik
 *   can_doldur    TUKETILIR   Canlari doldurur
 *
 * TUKETILIR URUN AKISI - cift odul vermemek icin:
 *   1. Google "PURCHASED" der -> oyuna (siparis anahtariyla) haber verilir.
 *   2. Oyun odulu verir, anahtari kayda yazar, teslimEdildi(anahtar) cagirir.
 *   3. Ancak o zaman burada consumeAsync calisir (urun tekrar alinabilir olur).
 *   Uygulama 2 ile 3 arasinda kapanirsa: acilista sorgu ayni satin almayi
 *   yine bulur, oyun anahtari tanir ve odulu IKINCI KEZ VERMEZ, sadece
 *   tuketimi tamamlatir. Odul kaybolmaz, ikilenmez.
 *
 * ONAYLAMA (acknowledge): Kalici urunde 3 gun icinde onaylanmazsa Google
 *   parayi otomatik iade eder. Tuketilir urunde consume ayni zamanda onaydir.
 *
 * GUVENLIK SINIRI - DURUSTCE: Dogrulama cihazda yapiliyor. Sunucu tarafli
 *   dogrulama icin backend gerekir; tek gelistirici icin bilincli tercih.
 */
public class Odeme {

    private static final String ETIKET = "PrismioOdeme";

    public static final String PLUS = "prismio_plus";
    public static final Set<String> TUKETILIR = new HashSet<>(Arrays.asList(
            "isik_500", "isik_1500", "isik_4000", "can_doldur"));
    private static final List<String> TUM_URUNLER = Arrays.asList(
            PLUS, "isik_500", "isik_1500", "isik_4000", "can_doldur");

    private final Activity etkinlik;
    private final Geri geri;

    private BillingClient istemci;
    private final Map<String, ProductDetails> urunler = new HashMap<>();
    /** Oyuna haber verilmis ama henuz tuketilmemis satin almalar: anahtar -> Purchase */
    private final Map<String, Purchase> bekleyenTeslim = new HashMap<>();
    private boolean bagli = false;
    private boolean sahip = false;
    private boolean dogrulandi = false;

    /** Oyun tarafina haber vermek icin. */
    public interface Geri {
        /**
         * Plus durumu: sahip mi, Plus fiyat metni, satin alma hazir mi,
         * dogrulandi = Google Play sorgusu BASARIYLA cevap verdi.
         * "sahip=false" ancak dogrulandi=true iken kesindir (iade / ters ibraz).
         */
        void durum(boolean sahip, String fiyat, boolean hazir, boolean dogrulandi);
        /** Plus satin alma sonucu */
        void sonuc(boolean basarili, String mesaj);
        /** Butun urunlerin yerel fiyatlari: {"isik_500":"₺58,99", ...} */
        void fiyatlar(String json);
        /** Tuketilir urun odenmis: oyun odulu versin, sonra teslimEdildi(anahtar) */
        void teslim(String urunId, String anahtar);
        /** Tuketilir urun satin alma akisi basarisiz / iptal */
        void urunSonuc(String urunId, boolean basarili, String mesaj);
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
                .setListener(this::satinAlmaGuncellendi)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        baglan();
    }

    private void baglan() {
        if (istemci == null) return;
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
                bagli = false;      // bir sonraki istekte yeniden baglanilir
                haberVer();
            }
        });
    }

    // ------------------------------------------------------------------
    //  URUN BILGISI (yerel fiyat)
    // ------------------------------------------------------------------

    private void urunBilgisiAl() {
        List<QueryProductDetailsParams.Product> liste = new ArrayList<>();
        for (String id : TUM_URUNLER) {
            liste.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }
        QueryProductDetailsParams istek = QueryProductDetailsParams.newBuilder()
                .setProductList(liste).build();

        istemci.queryProductDetailsAsync(istek, (sonuc, detaySonuc) -> {
            List<ProductDetails> gelen = (detaySonuc == null) ? null : detaySonuc.getProductDetailsList();
            if (sonuc.getResponseCode() == BillingClient.BillingResponseCode.OK && gelen != null) {
                for (ProductDetails pd : gelen) urunler.put(pd.getProductId(), pd);
            } else {
                Log.w(ETIKET, "urunler alinamadi: " + sonuc.getDebugMessage());
            }
            haberVer();
        });
    }

    private String fiyatMetni(String id) {
        ProductDetails pd = urunler.get(id);
        if (pd == null || pd.getOneTimePurchaseOfferDetails() == null) return null;
        return pd.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    // ------------------------------------------------------------------
    //  ONCEKI SATIN ALMALAR (geri yukleme + yarim kalan teslimler)
    // ------------------------------------------------------------------

    public void satinAlmalariSorgula() {
        if (istemci == null || !bagli) { haberVer(); return; }

        QueryPurchasesParams istek = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        istemci.queryPurchasesAsync(istek, (sonuc, liste) -> {
            // Sorgu basarisizsa eski durumu DEGISTIRME: yanlislikla
            // "sahip degil" demek odeyen oyuncuyu cezalandirir.
            if (sonuc.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                dogrulandi = false;
                haberVer();
                return;
            }
            boolean plusVar = false;
            if (liste != null) {
                for (Purchase sa : liste) {
                    if (sa.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
                    if (sa.getProducts().contains(PLUS)) { plusVar = true; onayla(sa); }
                    tuketilirTeslimEt(sa);       // yarim kalmis teslim varsa tamamlanir
                }
            }
            sahip = plusVar;
            dogrulandi = true;
            haberVer();
        });
    }

    // ------------------------------------------------------------------
    //  SATIN ALMA
    // ------------------------------------------------------------------

    /** Plus icin eski cagri. */
    public void satinAl() { satinAl(PLUS); }

    public void satinAl(String urunId) {
        boolean plus = PLUS.equals(urunId);
        if (!TUM_URUNLER.contains(urunId)) return;
        if (istemci == null || !bagli) {
            sonucBildir(urunId, false, "Google Play'e bağlanılamadı.");
            baglan();
            return;
        }
        ProductDetails pd = urunler.get(urunId);
        if (pd == null) {
            sonucBildir(urunId, false, "Ürün bilgisi alınamadı. Sonra tekrar dene.");
            urunBilgisiAl();
            return;
        }
        if (plus && sahip) { geri.sonuc(true, null); return; }

        BillingFlowParams akis = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()))
                .build();
        BillingResult sonuc = istemci.launchBillingFlow(etkinlik, akis);
        if (sonuc.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            sonucBildir(urunId, false, "Satın alma ekranı açılamadı.");
        }
    }

    private void sonucBildir(String urunId, boolean ok, String mesaj) {
        if (PLUS.equals(urunId)) geri.sonuc(ok, mesaj);
        else geri.urunSonuc(urunId, ok, mesaj);
    }

    /**
     * Satin alma sonucu buraya duser. (Lambda alani degil metot: alan
     * baslaticilari kurucudan once calisir, 'geri' henuz atanmamis olurdu.)
     */
    private void satinAlmaGuncellendi(BillingResult sonuc, List<Purchase> liste) {
        int kod = sonuc.getResponseCode();

        if (kod == BillingClient.BillingResponseCode.OK && liste != null) {
            for (Purchase sa : liste) {
                if (sa.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    // Nakit / havale gibi gecikmeli odeme: onaylanana kadar odul yok.
                    for (String id : sa.getProducts())
                        sonucBildir(id, false, "Ödeme onay bekliyor. Onaylanınca otomatik teslim edilir.");
                    continue;
                }
                if (sa.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
                if (sa.getProducts().contains(PLUS)) {
                    onayla(sa); sahip = true; dogrulandi = true;
                    geri.sonuc(true, null); haberVer();
                }
                tuketilirTeslimEt(sa);
            }
            return;
        }
        // Hangi urun icin oldugu bu noktada bilinmiyor; JS son istegi bilir.
        if (kod == BillingClient.BillingResponseCode.USER_CANCELED) {
            geri.sonuc(false, null);
            geri.urunSonuc("*", false, null);
            return;
        }
        if (kod == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            // Kalici Plus zaten alinmis ya da tuketilmemis eski bir alim var:
            // sorgu ikisini de duzeltir (Plus acilir / eski alim teslim edilir).
            satinAlmalariSorgula();
            return;
        }
        geri.sonuc(false, "Satın alma tamamlanamadı.");
        geri.urunSonuc("*", false, "Satın alma tamamlanamadı.");
    }

    // ------------------------------------------------------------------
    //  TUKETILIR URUN TESLIMI
    // ------------------------------------------------------------------

    private void tuketilirTeslimEt(Purchase sa) {
        for (String id : sa.getProducts()) {
            if (!TUKETILIR.contains(id)) continue;
            String anahtar = sa.getPurchaseToken();
            bekleyenTeslim.put(anahtar, sa);
            geri.teslim(id, anahtar);
        }
    }

    /** Oyun odulu verdi ve kaydetti: artik tuket (tekrar alinabilir olsun). */
    public void teslimEdildi(String anahtar) {
        final Purchase sa = bekleyenTeslim.get(anahtar);
        if (sa == null || istemci == null) return;
        ConsumeParams istek = ConsumeParams.newBuilder().setPurchaseToken(anahtar).build();
        istemci.consumeAsync(istek, (sonuc, tok) -> {
            if (sonuc.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                bekleyenTeslim.remove(anahtar);
            } else {
                // Sonraki acilista sorgu tekrar bulur; oyun anahtari tanidigi
                // icin odul ikinci kez verilmez, sadece tuketim yeniden denenir.
                Log.w(ETIKET, "tuketilemedi: " + sonuc.getDebugMessage());
            }
        });
    }

    // ------------------------------------------------------------------
    //  ONAYLAMA (kalici urun) — ATLANAMAZ, yoksa 3 gun sonra iade
    // ------------------------------------------------------------------

    private void onayla(Purchase sa) {
        if (sa.isAcknowledged()) return;
        AcknowledgePurchaseParams istek = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(sa.getPurchaseToken()).build();
        istemci.acknowledgePurchase(istek, sonuc -> {
            if (sonuc.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(ETIKET, "onaylanamadi: " + sonuc.getDebugMessage());
            }
        });
    }

    // ------------------------------------------------------------------

    private void haberVer() {
        geri.durum(sahip, fiyatMetni(PLUS), bagli && urunler.containsKey(PLUS), dogrulandi);
        try {
            JSONObject f = new JSONObject();
            if (bagli) for (String id : TUM_URUNLER) { String m = fiyatMetni(id); if (m != null) f.put(id, m); }
            geri.fiyatlar(f.toString());
        } catch (Exception e) {
            Log.w(ETIKET, "fiyat listesi", e);
        }
    }

    public void kapat() {
        if (istemci != null) {
            istemci.endConnection();
            istemci = null;
            bagli = false;
        }
    }
}
