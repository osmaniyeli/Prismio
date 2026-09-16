# GİZLİLİK SAYFASI — NASIL YAYINLANIR

Google Play, gizlilik politikası için **herkese açık bir web adresi** istiyor.
Bu klasör o sayfayı içeriyor. GitHub Pages ile bedava yayınlanır.

## KURULUM (bir kez, 2 dakika)

1. GitHub'da depoya git: https://github.com/osmaniyeli/Prismio
2. Üstten **Settings**
3. Sol menüden **Pages**
4. "Build and deployment" bölümünde:
   - Source: **Deploy from a branch**
   - Branch: **main**
   - Folder: **/docs**  ← burası önemli
5. **Save**

Bir iki dakika sonra sayfa yayında olur.

## ADRESİN

```
https://osmaniyeli.github.io/Prismio/
```

Bu adresi Play Console'a gireceksin:
**Uygulama içeriği → Gizlilik politikası**

## DİL

Sayfa iki dilli. Tarayıcı diline göre açılır.
Doğrudan bir dile gitmek için:

```
https://osmaniyeli.github.io/Prismio/#tr
https://osmaniyeli.github.io/Prismio/#en
```

## KONTROL

Yayına aldıktan sonra adresi **gizli sekmede** aç. Giriş istemeden
açılmalı. Google'ın incelemecisi hesapsız bakacak.

## GÜNCELLEME

Politika değişirse:
1. `GIZLILIK.md` güncellenir (kaynak metin)
2. `docs/index.html` yeniden üretilir
3. push edilir — GitHub Pages kendiliğinden yeniler

**ÖNEMLİ:** Uygulamaya reklam eklenirse bu politika **önceden**
güncellenmeli. Google Play Veri Güvenliği formu da değişmeli.
Yanlış beyan uygulamanın kaldırılmasına sebep olur.
