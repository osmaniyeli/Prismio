# GOOGLE PLAY GAMES — KURULUM (senin yapacakların)

Kod tarafı hazır: giriş, oyuncu adı, bulut kayıt, 21 başarı, ışık sıralaması.
Play Console'da aşağıdakiler yapılmadan **giriş çalışmaz**. Sıra önemli.

---

## 1. Kimlik bilgisi (OAuth) — girişin çalışması için ŞART

Giriş başarısız oluyorsa sebep %90 budur.

1. GitHub → Actions → **2 - Derle ve yayinla** çalışsın, çıktıdaki
   `parmak-izi.txt` dosyasını aç. **SHA1** satırını kopyala (yükleme anahtarı).
2. Play Console → Uygulama → **Test ve yayınlama → Uygulama bütünlüğü →
   Uygulama imzalama**. Oradaki **Uygulama imzalama anahtarı SHA-1**'i de kopyala
   (Google'ın anahtarı; Play'den indirilen sürüm bununla imzalanır).
3. Play Console → **Play Games Services → Kurulum ve yönetim → Yapılandırma →
   Kimlik bilgileri → Kimlik bilgisi ekle → Android**
   - Paket adı: `com.prismio.app`
   - SHA-1: 1. adımdaki → kaydet
4. Aynısını 2. adımdaki SHA-1 ile **ikinci bir kimlik bilgisi** olarak tekrarla.

İkisi de gerekli: biri olmazsa giriş ya senin APK'nda ya da Play'den
indirilen sürümde çalışmaz.

## 2. Test kullanıcıları

Oyun hizmetleri yayınlanana kadar sadece test kullanıcıları giriş yapabilir.

Play Games Services → **Test kullanıcıları** → kendi Gmail'ini ve test
edecek herkesi ekle.

## 3. Kayıtlı oyunlar

Play Games Services → Yapılandırma → **Kayıtlı oyunlar: AÇIK**.
Kapalıysa bulut yedek çalışmaz.

## 4. Başarılar (21 adet)

Play Games Services → **Başarılar → Başarı oluştur**. Hepsi **standart**
(artımlı değil). Her birine 512×512 ikon gerekir. Toplam puan 1000'i geçemez.

| Kimlik satırı (`oyun_kimlikleri.xml`) | Ad | Açıklama | Puan |
|---|---|---|---|
| `pg_a_ilk` | İlk Işık | İlk bölümü tamamla. | 10 |
| `pg_a_bes` | Isınma | 5 bölüm tamamla. | 10 |
| `pg_a_ilkuc` | Temiz İş | Bir bölümü 3 yıldızla bitir. | 10 |
| `pg_a_mile` | Bölge Ustası | İlk bölgeyi tamamla. | 20 |
| `pg_a_zaman` | Hızlı Düşün | Zamana karşı 3 bölüm çöz. | 20 |
| `pg_a_kolek` | Koleksiyoncu | 3 tema aç. | 20 |
| `pg_s_3` | Üç Gün | 3 gün üst üste oyna. | 15 |
| `pg_a_usta` | Usta | 10 bölümü 3 yıldızla bitir. | 30 |
| `pg_s_7` | Bir Hafta | 7 gün üst üste oyna. | 30 |
| `pg_a_gezgin` | Gezgin | 50 bölüm tamamla. | 40 |
| `pg_a_mukem` | Mükemmeliyetçi | 20 bölümü en iyi çözümle bitir. | 40 |
| `pg_a_deko` | Dünya Kurucu | 5 dekorasyon aç. | 30 |
| `pg_a_200` | Uzun Yol | 200 bölüm tamamla. | 60 |
| `pg_a_buyuk` | Büyük Sınav | 10 büyük bölümü geç (her 50.). | 60 |
| `pg_s_30` | Bir Ay | 30 gün üst üste oyna. | 60 |
| `pg_a_temaall` | Tam Koleksiyon | Bütün temaları aç. | 60 |
| `pg_a_y100` | Kusursuz Yüz | 100 bölümü 3 yıldızla bitir. | 80 |
| `pg_a_500` | Yarı Yol | 500 bölüm tamamla. | 80 |
| `pg_a_dekoall` | Dünya Tamam | Bütün dünya parçalarını aç. | 80 |
| `pg_s_100` | Yüz Gün | 100 gün üst üste oyna. | 100 |
| `pg_a_1000` | Işığın Sonu | 1000 bölüm tamamla. | 145 |

Toplam: 1000. Her başarının oluşunca verdiği kimliği (ör. `CgkI9tKf3IgYEAIQAQ`)
`app/src/main/res/values/oyun_kimlikleri.xml` içinde ilgili satıra yapıştır.

Kolay yol: Başarılar sayfasında **Kaynakları al** düğmesi tüm kimlikleri
XML olarak verir; o listeyi bana yapıştırırsan dosyayı ben doldururum.

Boş bırakılan satır sessizce atlanır, oyun bozulmaz. Giriş yapmadan önce
kazanılan başarılar giriş anında Play Games'e toplu gönderilir.

## 5. Sıralama

Play Games Services → **Sıralamalar → Oluştur**
- Ad: **Toplam ışık** · Sıralama: **Büyük daha iyi** · Biçim: Sayısal
- Kimliğini `oyun_kimlikleri.xml` → `pg_siralama_toplam_isik` satırına yapıştır.

## 6. Yayınla

Play Games Services → **Yayınla**. Bu yapılmadan sadece test kullanıcıları
giriş yapabilir.

---

## OTOMATİK PLAY YÜKLEMESİ (FinScout'taki gibi)

Bir kez kurarsın, sonra `main`'e her push dahili test kanalına gider.

1. **İlk AAB'yi Console'dan elle yükle** (Google ilk sürümü API ile kabul etmez):
   Actions çıktısındaki `app-release.aab` → Test ve yayınlama → Dahili test →
   Yeni sürüm.
2. Google Cloud Console → Prismio'nun bağlı projesi → **IAM → Hizmet hesapları
   → Oluştur** → **Anahtarlar → JSON anahtar ekle** → dosya iner.
3. Play Console → **Kullanıcılar ve izinler → Yeni kullanıcı davet et** →
   hizmet hesabının e-postası → uygulamaya **Sürümleri yayınla** izni.
4. GitHub → Settings → Secrets → Actions → **PLAY_SERVICE_ACCOUNT_JSON** →
   JSON dosyasının tamamını yapıştır.

Bundan sonra her push: kalite kontrolü → imzalı AAB/APK → Play dahili test.
Sürüm kodu kendiliğinden artar (1000 + çalışma numarası). Üretime geçiş
Console'dan elle onaylanır. Mağaza "Yenilikler" metni:
`distribution/whatsnew/whatsnew-tr-TR` (ve `-en-US`).

## GİZLİLİK SAYFASI

Zaten yayında: `https://osmaniyeli.github.io/Prismio/` (GitHub Pages, main → /docs).
Oyundaki Hakkında → Gizlilik politikası bu adresi açar. Aynı adresi Play Console →
Uygulama içeriği → Gizlilik politikası alanına yaz.

---

## GİRİŞ ÇALIŞMIYORSA

| Belirti | Sebep |
|---|---|
| "Giriş yapılamadı" hemen çıkıyor | SHA-1 kimlik bilgisi eksik (1. adım) |
| Kendi APK'nda çalışıyor, Play sürümünde çalışmıyor | Google imzalama SHA-1'i eklenmemiş |
| Bir hesapla çalışıyor, diğeriyle çalışmıyor | Hesap test kullanıcısı değil (2. adım) |
| Giriş var ama bulut kayıt yok | Kayıtlı oyunlar kapalı (3. adım) |
| Başarı düğmesi görünmüyor | `oyun_kimlikleri.xml` boş |

Ayrıntılı hata: telefonu bağla, `adb logcat -s PrismioKopru`.
