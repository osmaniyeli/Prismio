# GITHUB İLE AAB ÜRETME

Bilgisayara hiçbir şey kurmadan AAB ve APK üretirsin. GitHub'ın
sunucusu derler, sen indirirsin. Telefondan bile yapılabilir.

Depo: https://github.com/osmaniyeli/prismio

---

## ADIM 1 — DOSYALARI YÜKLE

Zip dosyasının içinde **hazır bir git deposu** var. Commit atılmış,
uzak adres bağlanmış. Tek yapman gereken göndermek.

### Git kuruluysa (en kolay)

Zip'i aç, klasörün içinde terminal aç ve şunu yaz:

```
git push -u origin main
```

GitHub kullanıcı adı ve şifre (veya token) soracak.

**Depoda zaten bir şey varsa ne olur?**

GitHub yeni depo açarken sana `git init` + `README.md` komutlarını
gösterir. Onları çalıştırdıysan depoda bir commit vardır ve push
reddedilir ("rejected — non-fast-forward").

O zaman şunu yaz:

```
git push -u origin main --force
```

Depo boş ve senin dışında kimse kullanmıyor, bu güvenli. Sadece
GitHub'ın attığı boş README commit'i silinir.

**Not:** GitHub artık şifre kabul etmiyor, **personal access token**
istiyor. Yoksa: github.com → Settings → Developer settings →
Personal access tokens → Tokens (classic) → Generate new token →
`repo` kutusunu işaretle. Çıkan uzun metni şifre yerine yapıştır.

### Git yoksa — web'den yükle

1. github.com/osmaniyeli/prismio → **Add file → Upload files**
2. Zip'i aç, **içindeki dosyaların hepsini** sürükle
3. **DİKKAT:** `.github` klasörü gizli olabilir. Görünmüyorsa:
   - Windows: Görünüm → Gizli öğeler
   - Mac: `Cmd + Shift + .`
   Bu klasör olmadan derleme çalışmaz.
4. Alttaki yeşil **Commit changes** düğmesine bas

Depo şöyle görünmeli:

```
prismio/
├── .github/workflows/     ← bunlar olmadan çalışmaz
├── app/
├── build.gradle
├── settings.gradle
├── README.md
└── ...
```

**`prismio-android` diye bir klasör OLMAMALI.** Dosyalar doğrudan
kökte olmalı.

---

## ADIM 2 — İMZA ANAHTARINI ÜRET

Bunu **sadece bir kez** yapacaksın.

1. Depoda **Actions** sekmesine gir
2. Soldan **"1 - Imza anahtari uret"** seç
3. Sağda **"Run workflow"** düğmesine bas
4. Bir dakika bekle, yeşil tik çıkacak
5. İşe tıkla, aşağıda **"PRISMIO-IMZA-ANAHTARI"** diye bir dosya var,
   indir

İçinde dört dosya var. **OKU-ONEMLI.txt** dosyasını aç, şifreler orada.

### Şimdi gizli değerleri ekle

Depoda: **Settings → Secrets and variables → Actions → New repository secret**

Üç tane ekleyeceksin:

| Ad | Değer |
|---|---|
| `IMZA_ANAHTARI` | `prismio.jks.base64` dosyasının içindeki metnin tamamı |
| `MAGAZA_SIFRE` | OKU-ONEMLI.txt'deki mağaza şifresi |
| `ANAHTAR_SIFRE` | OKU-ONEMLI.txt'deki anahtar şifresi |

### prismio.jks dosyasını SAKLA

Bu dosyayı kaybedersen **aynı uygulamayı bir daha güncelleyemezsin.**
Yeni anahtarla yüklemek, Google için yeni bir uygulama demektir —
oyuncular güncelleme alamaz.

Bulut depolamaya, harici diske, e-postana — nereye olursa yedekle.

**GitHub'daki indirilebilir dosya 7 gün sonra silinir.** İndirmeyi
unutma.

---

## ADIM 3 — AAB ÜRET

1. **Actions** → **"2 - AAB ve APK uret"** → **Run workflow**
2. İki alan soracak:
   - **Surum adi:** `0.9.0` (oyuncunun gördüğü)
   - **Surum kodu:** `1` (Google'ın gördüğü, her yüklemede ARTMALI)
3. Yeşil tik çıkınca **"Prismio-AAB-APK"** dosyasını indir

İçinde:
- `app-release.aab` → Google Play'e yükleyeceğin
- `app-release.apk` → **kendi telefonuna kurup test edeceğin**
- `parmak-izi.txt` → Play Console'a gireceğin SHA-1

İlk derleme 5-10 dakika sürer. Sonrakiler daha hızlı.

---

## SÜRÜM ETİKETİYLE ÜRETME

Elle başlatmak yerine etiket de atabilirsin:

```
git tag v1.0
git push origin v1.0
```

Bu, otomatik derler ve **Releases** bölümüne koyar. Yayın sürümleri
için daha düzenli bir yöntem.

---

## TELEFONDA TEST ETME

`app-release.apk` dosyasını telefona at, dokun, kur.

"Bilinmeyen kaynak" uyarısı çıkarsa izin ver — bu, Play dışından
kurulan her uygulamada çıkar.

**AAB telefona kurulmaz**, o sadece Google Play içindir.

---

## SÜRÜM KODU — ÖNEMLİ

Google aynı sürüm kodunu iki kez kabul etmez.

| Yükleme | Sürüm kodu | Sürüm adı |
|---|---|---|
| İlk | 1 | 0.9.0 |
| İkinci | 2 | 0.9.1 |
| Üçüncü | 3 | 1.0.0 |

Sürüm adı serbest, sürüm kodu hep artmalı.

---

## ÜCRETSİZ KOTA

- **Açık depo:** sınırsız
- **Özel depo:** ayda 2000 dakika

Bir derleme 5-10 dakika. Özel depoda ayda 200 derleme yapabilirsin —
fazlasıyla yeter.

---

## SORUN ÇIKARSA

**"Eksik gizli deger"** → Adım 2'yi tamamlamadın.

**"IMZA ANAHTARI BULUNAMADI"** → `IMZA_ANAHTARI` değeri eksik veya
yanlış kopyalanmış. Base64 metni tek satırdır, tamamını kopyala.

**"Gradle sync failed"** → Dosyalar deponun kökünde değil. Adım 1'e bak.

**Derleme uzun sürüyor** → İlk seferde normal, Android SDK indiriliyor.

---

## PLAY CONSOLE'DA KALAN İŞLER

AAB hazır olduktan sonra:

1. **OAuth istemci kimliği** — `parmak-izi.txt`'deki SHA-1'i gir
2. **Kayıtlı oyunları AÇ** — bulut kayıt bunsuz çalışmaz
3. Diğer her şey `PLAY_KONTROL.md` dosyasında
