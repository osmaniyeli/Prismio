package com.prismio.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.games.PlayGamesSdk;

/**
 * Play Games SDK burada baslatilir.
 *
 * ONEMLI: PlayGamesSdk.initialize() her acilista OTOMATIK GIRIS dener ve
 * oyuncu giris yapmamissa Google'in giris penceresini kendisi acabilir.
 * Oyuncu bir kez reddettiyse (ya da giris yapamadiysa) "durum=red" olarak
 * hatirlanir ve acilista SDK HIC baslatilmaz: pencere cikmaz. SDK ancak
 * oyuncu Hesap ekranindan "Giris yap"a basinca baslatilir (OyunKopru.girisYap).
 *
 * Tercih SharedPreferences "prismio_pg" icinde: durum = "" | "girisli" | "red".
 */
public class PrismioApp extends Application {

    static final String PG_DEPO = "prismio_pg";
    private static final String ETIKET = "PrismioApp";
    private static boolean sdkHazir = false;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences d = getSharedPreferences(PG_DEPO, Context.MODE_PRIVATE);
        if (!"red".equals(d.getString("durum", ""))) {
            sdkBaslat(this);
        }
    }

    /** SDK'yi (en fazla bir kez) baslatir. Zaten baslatildiysa hicbir sey yapmaz. */
    static synchronized void sdkBaslat(Context c) {
        if (sdkHazir) return;
        try {
            PlayGamesSdk.initialize(c.getApplicationContext());
            sdkHazir = true;
        } catch (Throwable t) {
            Log.w(ETIKET, "Play Games SDK baslatilamadi", t);
        }
    }

    static synchronized boolean sdkHazirMi() {
        return sdkHazir;
    }
}
