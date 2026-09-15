# GOOGLE PLAY UYUM KONTROL LİSTESİ

Bu liste Google'ın belgelerinden çıkarıldı. İki bölüm var:
**kodda olanlar** (ben yaptım) ve **senin yapacakların** (Play Console).

Kaynaklar:
- support.google.com/googleplay/android-developer/answer/10788890
- developer.android.com/google/play/requirements/target-sdk
- support.google.com/googleplay/android-developer/answer/10787469
- developer.android.com/games/pgs/android/android-start
- developer.android.com/games/pgs/quality

---

## BÖLÜM 1 — KODDA YAPILANLAR

| Şart | Durum | Nerede |
|---|---|---|
| targetSdk 36 (API 36, Android 16) | TAMAM | app/build.gradle |
| compileSdk 36 | TAMAM | app/build.gradle |
| AGP 8.11.1 (compileSdk 36 en az 8.9 ister) | TAMAM | build.gradle |
| Gradle 8.13 (AGP 8.11 en az 8.13 ister) | TAMAM | iş akışı + wrapper |
| JDK 17 | TAMAM | iş akışı |
| minSdk 23 (Play Games en az 19 ister) | TAMAM | app/build.gradle |
| AAB formatı (APK değil) | TAMAM | bundleRelease görevi |
| Play Games SDK v2 | TAMAM | play-services-games-v2:20.1.2 |
| PlayGamesSdk.initialize() | TAMAM | PrismioApp.java |
| games.APP_ID meta-data | TAMAM | AndroidManifest.xml |
| Otomatik giriş kontrolü | TAMAM | OyunKopru.durumSor() |
| Kayıtlı oyun (bulut kayıt) | TAMAM | OyunKopru.yedekle/indir |
| Kayıt üstverisi: kapak, açıklama, süre | TAMAM | Kalite Kontrol 6.1 |
| Çakışma çözümü | TAMAM | En ileri kayıt kazanır |
| Giriş oyunu ENGELLEMEZ | TAMAM | İsteğe bağlı |
| 10 başarı | TAMAM | 10 adet |
| 4 başarı ilk saatte alınabilir | TAMAM | Doğrulandı |
| Otomatik yedekleme | TAMAM | allowBackup=true |
| İzin listesi minimum | TAMAM | Sadece INTERNET + NETWORK_STATE |
| usesCleartextTraffic=false | TAMAM | Şifresiz trafik yasak |
| WebView güvenlik kısıtları | TAMAM | MainActivity.java |
| Gizlilik politikası metni | TAMAM | GIZLILIK.md |

### PROJE KİMLİĞİ — YAZILDI

`app/src/main/res/values/strings.xml`:

```xml
<string name="game_services_project_id">853778504598</string>
```

Bu gerçek değer. Kodda yapılacak başka bir şey kalmadı.

---

## BÖLÜM 2 — SENİN YAPACAKLARIN (Play Console)

### Hesap kurulumu

| İş | Not |
|---|---|
| Geliştirici hesabı doğrulaması | Kimlik + adres. Kişisel hesap yeterli. |
| Geliştirici adı ve iletişim e-postası | Mağazada görünür |
| Ödeme profili | Ücretsiz oyun için bile isteniyor |

**Kuruluş hesabı gerekmez.** Kuruluş şartı finans, sağlık, VPN ve
devlet uygulamaları için. Oyun bunlara girmiyor.

### Uygulama içeriği bölümü

| Form | Cevap |
|---|---|
| Gizlilik politikası URL | GIZLILIK.md'yi bir yere koy, adresini yaz |
| Reklam içeriyor mu | **HAYIR** (şu an reklam yok) |
| İçerik derecelendirme anketi | Doldur — oyun her yaşa uygun |
| Hedef kitle | 13+ öner. Çocuk seçersen Aile Politikası şartları gelir |
| Devlet uygulaması mı | Hayır |
| Finans uygulaması mı | Hayır |
| Sağlık uygulaması mı | Hayır |
| Haber uygulaması mı | Hayır |
| Giriş bilgisi gerekiyor mu | **HAYIR** — oyun girişsiz tam çalışır |

### Veri güvenliği formu — cevaplar

Google'ın kuralı: **cihazda kalan veri "toplama" sayılmaz.**
Kaynak: support.google.com/googleplay/android-developer/answer/10787469

| Soru | Cevap | Gerekçe |
|---|---|---|
| Veri topluyor musunuz? | **HAYIR** | Bizim sunucumuz yok |
| Veri paylaşıyor musunuz? | **HAYIR** | Kimseye veri gitmiyor |
| Aktarımda şifreleniyor mu? | Evet | Play Games HTTPS kullanır |
| Kullanıcı silme isteyebilir mi? | Evet | Uygulamayı silmek yeterli |

**Play Games sorusu çıkarsa:** Oyun ilerlemesi kullanıcının kendi
Google hesabına kaydedilir. Geliştirici bu veriye erişmez. Bu Google'ın
kendi servisidir, üçüncü tarafa paylaşım değildir.

### Oyun hizmetleri kurulumu

| Adım | Not |
|---|---|
| Play Console → Oyun hizmetleri → Yeni oyun | TAMAM (853778504598) |
| Proje kimliğini strings.xml'e yaz | TAMAM |
| **Kayıtlı oyunları AÇ** | **YAPILACAK** — bulut kayıt bunsuz çalışmaz |
| OAuth istemci kimliği oluştur | **YAPILACAK** — aşağıya bak |
| Test hesabı ekle | Kendi e-postanı ekle, yoksa test edemezsin |
| 10 başarıyı Console'a gir | Oyundaki adlarla aynı olsun |
| Her başarıya 512×512 ikon | Kalite Kontrol 2.4 |

### OAuth istemci kimliği — nasıl alınır

Play Games girişinin çalışması için Google'ın senin uygulamanı tanıması
gerekiyor. Bunu imza sertifikanın parmak iziyle yapıyor.

**Sıra şu:**

1. Önce AAB'yi üret (`yap.sh` veya Android Studio). Bu, `prismio.jks`
   imza dosyasını oluşturur.

2. Parmak izini al:
   ```
   keytool -list -v -keystore prismio.jks -alias prismio
   ```
   Çıktıda **SHA-1** diye bir satır var. Onu kopyala.

3. Play Console → Oyun hizmetleri → Kurulum ve yönetim → Kimlik bilgileri
   → **Kimlik bilgisi ekle** → Android → SHA-1'i yapıştır.

**ÖNEMLİ:** Google Play uygulamaları kendi anahtarıyla yeniden imzalar
("Play App Signing"). Bu yüzden **iki** parmak izi gerekebilir:
- Senin yükleme anahtarının SHA-1'i (yukarıdaki)
- Google'ın uygulama imzalama anahtarının SHA-1'i
  (Play Console → Test ve yayınlama → Uygulama bütünlüğü bölümünde yazar)

İkisini de ekle. Yoksa Play'den indirilen sürümde giriş çalışmaz ama
kendi telefonunda çalışır — bu kafa karıştırıcı bir hatadır.

### Mağaza sayfası

| Öğe | Durum |
|---|---|
| Uygulama adı (30 karakter) | Prismio |
| Kısa açıklama (80 karakter) | **YAZILACAK** |
| Uzun açıklama (4000 karakter) | **YAZILACAK** |
| Uygulama simgesi 512×512 | Elinde var |
| Tanıtım grafiği 1024×500 | **EKSİK** (H1) |
| Telefon ekran görüntüsü (en az 2) | Oyundan alınacak |

---

## ÖNEMLİ TARİH

**31 Ağustos 2026'dan beri** yeni uygulamalar **API 36** hedeflemek
zorunda. Proje API 36'ya ayarlandı. Daha düşük bir değer AAB'nin
reddedilmesine yol açar.

Uzatma istersen 1 Kasım 2026'ya kadar süre alınabiliyor, ama gerek yok.

---

## HENÜZ YAPILMAYANLAR

| Konu | Durum |
|---|---|
| Liderlik tabloları | Yok. Zorunlu değil. |
| Arkadaş listesi | Yok. Zorunlu değil (Kalite Kontrol 4.1 isteğe bağlı). |
| Reklam SDK | Yok. Eklenirse "Reklam içerir" işaretlenecek ve gizlilik politikası güncellenecek. |
| Ses sistemi | Yok. Ayarlar hazır. |
