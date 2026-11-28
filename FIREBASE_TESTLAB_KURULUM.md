# Cihaz çiftliği testi (Firebase Test Lab) — kurulum

Amaç: Her sürümde, testler geçtikten sonra temiz AAB Google'ın cihaz çiftliğinde açılıp "Robo" testiyle gezdirilsin.
Çökme olursa raporda görünsün (isteğe bağlı: yayın dursun). Secret'lar yoksa bu adım atlanır, yayın hattı bozulmaz.

## Senin yapacakların (yaklaşık 10 dk)
1. https://console.firebase.google.com → "Proje ekle" (ücretsiz Spark planı yeter). Test Lab günlük ücretsiz kotası vardır; güncel kotayı Firebase'den kontrol et.
2. Aynı Google Cloud projesinde şu API'leri etkinleştir: **Cloud Testing API** ve **Cloud Tool Results API**
   (Google Cloud Console → APIs & Services → Library).
3. Google Cloud Console → IAM → Service Accounts → yeni servis hesabı oluştur (ör. `testlab-ci`).
   Rolleri: **Firebase Test Lab Admin** ve **Firebase Viewer** (ya da proje düzeyinde Editor; en az yetki tercih edilir).
   Hesap için JSON anahtarı üret. **Anahtarı sohbete yapıştırma.**
4. GitHub → osmaniyeli/Prismio → Settings → Secrets and variables → Actions → New repository secret:
   - `FIREBASE_SERVICE_ACCOUNT_JSON` = anahtar dosyasının tüm içeriği
   - `FIREBASE_PROJECT_ID` = Firebase proje kimliği
5. (İsteğe bağlı) Variables sekmesi → `TESTLAB_ENGELLE` = `true` yaparsan Robo testi hata verince Play'e yükleme DURUR.
   Varsayılan: yalnız raporlar, yayını durdurmaz (cihaz kimliği değişimi gibi dış sebeplerle yayın tıkanmasın diye).

## Sonuçlar nerede?
- GitHub Actions günlüğü → "Test Lab - Robo (farklı cihazlar)" adımı; sonunda Firebase konsol bağlantısı yazılır
  (ekran videosu, çökme izi, performans).
- Aynı adım kullanılabilir cihaz modellerini de listeler. Eski/yeni model eklemek için `aab-uret.yml` içindeki `--device` satırlarını değiştir.

## Bilinen sınırlar
- Robo testi oyunu kendi kendine dokunarak gezer; bölüm çözmeyi garanti etmez. Amaç: açılış, çökme, uyumsuzluk, yavaşlık yakalamak.
- Cihaz kimlikleri (`Pixel2` vb.) zamanla katalogdan kalkabilir; günlükteki liste ile güncelle.
- Play Console'daki **Ön yayın raporu** (ücretsiz, otomatik) ayrıca bakılmalı: Yayın → Test → Ön yayın raporu.
