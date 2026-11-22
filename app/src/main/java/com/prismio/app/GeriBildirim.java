package com.prismio.app;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OYUN ICI GERI BILDIRIM
 *
 * Oyuncunun yazdigi mesaji Google Apps Script web uygulamasina yollar
 * (sahibinin Google Sheet'ine satir ekler). Ag islemi burada, arka planda yapilir;
 * boylece oyunun CSP'si degismez ve yeni izin gerekmez (INTERNET zaten var).
 *
 * Giden bilgi: tur, mesaj, oyun surumu, dil, seviye, Android surumu, cihaz modeli,
 * gonderim kimligi. Kisisel kimlik (hesap, reklam kimligi, IP kaydi) GONDERILMEZ.
 *
 * Sonuc kodlari (JS'e):
 *   "ok"  : kaydedildi (ya da ayni kimlik daha once kaydedilmisti)
 *   "red" : sunucu mesaji kabul etmedi (yetki/tur/kisa); tekrar denemenin anlami yok
 *   "ag"  : ag yok / sunucu mesgul / gunluk sinir; mesaj saklanip sonra tekrar denenir
 *
 * Apps Script POST'a 302 ile yanit verir; govde yonlendirilen adreste GET ile okunur.
 */
final class GeriBildirim {

    private static final String ETIKET = "PrismioGeriBildirim";
    private static final int ZAMAN_ASIMI = 15000;
    private static final int YONLENDIRME_SINIRI = 4;

    interface Sonuc { void bitti(String id, String kod); }

    private GeriBildirim() {}

    static void gonder(final Context ctx, final String json, final Sonuc sonuc) {
        new Thread(() -> {
            String id = "";
            String kod = "ag";
            try {
                JSONObject girdi = new JSONObject(json);
                id = girdi.optString("id", "");
                String url = ctx.getString(R.string.geri_bildirim_url).trim();
                String anahtar = ctx.getString(R.string.geri_bildirim_anahtar).trim();
                if (url.isEmpty() || anahtar.isEmpty()) {
                    kod = "red";
                } else {
                    JSONObject g = new JSONObject();
                    g.put("anahtar", anahtar);
                    g.put("tur", girdi.optString("tur"));
                    g.put("mesaj", girdi.optString("mesaj"));
                    g.put("surum", girdi.optString("surum"));
                    g.put("dil", girdi.optString("dil"));
                    g.put("seviye", girdi.optString("seviye"));
                    g.put("android", Build.VERSION.RELEASE);
                    g.put("cihaz", Build.MODEL);
                    g.put("id", id);
                    String yanit = yolla(url, g.toString());
                    kod = yorumla(yanit);
                }
            } catch (Throwable t) {
                Log.w(ETIKET, "gonderilemedi: " + t.getClass().getSimpleName());
                kod = "ag";
            }
            try { sonuc.bitti(id, kod); } catch (Throwable ignored) { }
        }, "geri-bildirim").start();
    }

    private static String yorumla(String yanit) {
        if (yanit == null) return "ag";
        try {
            JSONObject o = new JSONObject(yanit.trim());
            if (o.optBoolean("ok", false)) return "ok";
            String hata = o.optString("hata", "");
            if ("yetki".equals(hata) || "tur".equals(hata) || "kisa".equals(hata)) return "red";
            return "ag";   // sinir, sunucu...
        } catch (Throwable t) {
            return "ag";   // JSON degil (giris sayfasi, hata sayfasi...)
        }
    }

    /** POST, sonra 3xx ise Location'a GET. */
    private static String yolla(String adres, String govde) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(adres).openConnection();
        try {
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(ZAMAN_ASIMI);
            c.setReadTimeout(ZAMAN_ASIMI);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            byte[] b = govde.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(b.length);
            try (OutputStream o = c.getOutputStream()) { o.write(b); }
            int kodu = c.getResponseCode();
            if (kodu >= 300 && kodu < 400) {
                String yeni = c.getHeaderField("Location");
                c.disconnect();
                return izle(yeni, 0);
            }
            return oku(c, kodu);
        } finally {
            c.disconnect();
        }
    }

    private static String izle(String adres, int derinlik) throws Exception {
        if (adres == null || derinlik > YONLENDIRME_SINIRI) return null;
        if (!adres.startsWith("https://")) return null;          // sadece guvenli adres
        HttpURLConnection c = (HttpURLConnection) new URL(adres).openConnection();
        try {
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(ZAMAN_ASIMI);
            c.setReadTimeout(ZAMAN_ASIMI);
            c.setRequestMethod("GET");
            int kodu = c.getResponseCode();
            if (kodu >= 300 && kodu < 400) {
                String yeni = c.getHeaderField("Location");
                c.disconnect();
                return izle(yeni, derinlik + 1);
            }
            return oku(c, kodu);
        } finally {
            c.disconnect();
        }
    }

    private static String oku(HttpURLConnection c, int kodu) throws Exception {
        InputStream in = (kodu >= 200 && kodu < 300) ? c.getInputStream() : c.getErrorStream();
        if (in == null) return null;
        try (InputStream gi = in; ByteArrayOutputStream cikis = new ByteArrayOutputStream()) {
            byte[] tampon = new byte[2048];
            int n, toplam = 0;
            while ((n = gi.read(tampon)) > 0) {
                toplam += n;
                if (toplam > 16384) break;                          // beklenmedik buyuk yanit
                cikis.write(tampon, 0, n);
            }
            return cikis.toString("UTF-8");
        }
    }
}
