# PRISMIO — AAB ÜRETME

Bu klasör, Prismio'yu Google Play'e yüklenebilir bir **AAB** dosyasına
çeviren hazır projedir.

---

## NEDEN AAB HAZIR DEĞİL

AAB üretmek için Android SDK gerekiyor. Benim çalıştığım bulut ortamı
Google sunucularına çıkamıyor (şirket politikası engelliyor). SDK'yı
indiremedim, bu yüzden AAB'yi üretemedim.

Projenin tamamı hazır. Senin bilgisayarında **tek komut** yetiyor.

---

## PLAY GAMES PROJE KİMLİĞİ — YAZILDI

`strings.xml` içindeki proje kimliği **853778504598** olarak ayarlandı.
Kodda yapılacak bir şey kalmadı.

Play Console'da iki şey kaldı:

1. **Oyun hizmetleri → Kayıtlı oyunlar → AÇ.** Bulut kayıt bunsuz çalışmaz.
2. **OAuth istemci kimliği oluştur.** İmza parmak izi gerekir,
   AAB'yi ürettikten sonra alınır. Anlatımı `PLAY_KONTROL.md` içinde.

---

## NE GEREKİYOR

**Android Studio.** Tek ihtiyacın bu. İçinde SDK, Java ve Gradle geliyor.

İndir: https://developer.android.com/studio

Kur, bir kez aç, "Standard" kurulumu seç, bitmesini bekle. Sonra kapat.

---

## EN KOLAY YOL — Android Studio ile

1. Android Studio'yu aç
2. **Open** de, bu klasörü seç (`prismio-android`)
3. Sağ altta "Gradle sync" yazısı çıkar, bitmesini bekle (ilk sefer 5-10 dk)
4. Üst menü: **Build → Generate Signed App Bundle / APK**
5. **Android App Bundle** seç, İleri
6. **Create new...** de, anahtar bilgilerini gir
   - **ÖNEMLİ:** Bu anahtar dosyasını kaybetme ve yedekle.
     Kaybedersen aynı uygulamayı bir daha güncelleyemezsin.
7. **release** seç, Finish
8. AAB şurada oluşur:
   `app/build/outputs/bundle/release/app-release.aab`

---

## KOMUT SATIRI YOLU

Android Studio'yu bir kez açıp kapattıysan SDK hazırdır.

**Linux / macOS:**
```
export ANDROID_HOME=$HOME/Android/Sdk              # Linux
export ANDROID_HOME=$HOME/Library/Android/sdk      # macOS
./yap.sh
```

**Windows:**
```
yap.bat
```

Betik imza anahtarını kendisi üretir ve AAB'yi çıkarır.

---

## AAB'Yİ GOOGLE PLAY'E YÜKLEME

1. Google Play Console'a gir
2. **Uygulama oluştur** → Prismio
3. Sol menü: **Üretim** (veya önce **İç test**)
4. **Yeni sürüm oluştur**
5. AAB dosyasını sürükle
6. Sürüm notlarını yaz, kaydet

**İç test ile başlamanı öneririm.** Kendi telefonuna yükleyip
denersin, sorun varsa mağazaya çıkmadan düzeltiriz.

---

## YÜKLEMEDEN ÖNCE GOOGLE'IN İSTEYECEKLERİ

Bunlar henüz hazır değil, sen hazırlamalısın:

| İş | Durum |
|---|---|
| Gizlilik politikası **web adresi** | GEREKLİ — `GIZLILIK.md` hazır, bir yere koy |
| Uygulama simgesi 512×512 PNG | Elinde var |
| Tanıtım grafiği 1024×500 | Eksik (H1) |
| Ekran görüntüsü (en az 2) | Oyundan alınır |
| Kısa açıklama (80 karakter) | Yazılacak |
| Uzun açıklama (4000 karakter) | Yazılacak |
| İçerik derecelendirme anketi | Console'da doldurulur |
| Veri güvenliği formu | Aşağıya bak |

### Veri güvenliği formu — ne diyeceksin

Bu form Google'ın zorunlu tuttuğu bir ankettir. Prismio için cevaplar:

- **Veri topluyor musunuz?** → HAYIR
- **Veri paylaşıyor musunuz?** → HAYIR
- **Veriler şifreleniyor mu?** → Uygulama veri göndermiyor
- **Kullanıcı veri silmeyi isteyebilir mi?** → Uygulamayı silmek yeterli

Bu cevaplar **doğru**: uygulamanın internet izni bile yok.

**Yedekleme sorusu çıkarsa:** Oyun, Android'in standart yedekleme
sistemini kullanır (`allowBackup=true`). Bu, veriyi **kullanıcının kendi
Google hesabına** yedekler. Bize hiçbir şey gelmez. Google bunu "veri
toplama" saymaz — telefonun kendi özelliğidir.

---

## PAKETTE NE VAR

```
prismio-android/
├── app/
│   ├── build.gradle              derleme ayarları, imza, SDK sürümleri
│   └── src/main/
│       ├── AndroidManifest.xml   izinler, Play Games kimliği
│       ├── assets/index.html     oyunun kendisi
│       ├── java/.../
│       │   ├── PrismioApp.java       Play Games SDK başlatma
│       │   ├── MainActivity.java     WebView kabuğu
│       │   └── OyunKopru.java        Play Games köprüsü
│       └── res/
│           ├── values/strings.xml    proje kimliği (yazıldı)
│           └── xml/backup_rules.xml  yedekleme kuralları
├── yap.sh / yap.bat              tek komutla AAB
├── PLAY_KONTROL.md               Google Play uyum listesi
├── GIZLILIK.md                   gizlilik politikası
└── OKU.md                        bu dosya
```

---

## SÜRÜM NUMARASI

Her yeni yüklemede `app/build.gradle` içinde **versionCode** artmalı:

```
versionCode 1      →  2  →  3 ...
versionName "0.9.0"  →  "0.9.1" ...
```

Google aynı versionCode'u iki kez kabul etmez.
