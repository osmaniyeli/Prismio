# Prismio

Işık kırılması üzerine kurulu bir mobil bulmaca oyunu.

Prizmadan çıkan ışınları aynalarla yönlendirip kristalleri yakarsın.
Her tahtanın en az aynayla bir çözümü vardır. Asıl mesele çözmek değil,
**daha az aynayla çözmek**.

---

## Bu depo ne işe yarar

Oyun tek bir HTML dosyası. Bu depo onu Android uygulamasına çeviriyor
ve Google Play'e yüklenecek **AAB** dosyasını üretiyor.

Derleme GitHub Actions ile yapılıyor — bilgisayarına hiçbir şey
kurmana gerek yok.

**Nasıl yapılır: [GITHUB.md](GITHUB.md)**

---

## Dosyalar

| Dosya | Ne anlatır |
|---|---|
| [GITHUB.md](GITHUB.md) | AAB ve APK üretme, adım adım |
| [PLAY_KONTROL.md](PLAY_KONTROL.md) | Google Play uyum listesi |
| [GIZLILIK.md](GIZLILIK.md) | Gizlilik politikası metni |
| [OKU.md](OKU.md) | Kendi bilgisayarında derleme |

---

## Teknik

| | |
|---|---|
| Oyun | Tek dosya HTML + CSS + JavaScript + SVG |
| Kabuk | Android WebView |
| Bölüm sayısı | 1000 (yordamsal üretim, tohumlu) |
| minSdk | 23 (Android 6.0) |
| targetSdk | 36 (Android 16) |
| Bulut kayıt | Google Play Games |

Oyun tamamen çevrimdışı çalışır. İnternet yalnızca isteğe bağlı
Play Games girişi için kullanılır.

---

## Sürüm

0.9.0 — test yapımı

---

Tasarım ve yapım: Osmaniyeli Abdullah
