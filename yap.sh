#!/usr/bin/env bash
# Prismio - AAB uretme betigi (Linux / macOS)
set -e
cd "$(dirname "$0")"

echo "=== PRISMIO AAB URETIMI ==="

if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
  echo "HATA: Android SDK bulunamadi."
  echo "Android Studio kurup bir kez actiysan SDK zaten vardir."
  echo "Sonra su satiri calistir:"
  echo "  export ANDROID_HOME=\$HOME/Android/Sdk      # Linux"
  echo "  export ANDROID_HOME=\$HOME/Library/Android/sdk   # macOS"
  exit 1
fi

# --- IMZA ANAHTARI ---
# Google Play, AAB'nin IMZALI olmasini ister. Anahtar yoksa uretilir.
if [ ! -f prismio.jks ]; then
  echo ""
  echo "Imza anahtari yok, uretiliyor..."
  echo "ONEMLI: prismio.jks dosyasini KAYBETME ve YEDEKLE."
  echo "Kaybedersen ayni uygulamayi bir daha guncelleyemezsin."
  echo ""
  keytool -genkeypair -v \
    -keystore prismio.jks -alias prismio \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass prismio123 -keypass prismio123 \
    -dname "CN=Prismio, OU=Oyun, O=Osmaniyeli Abdullah, L=Istanbul, C=TR"
  echo "prismio.jks uretildi."
fi

echo ""
echo "AAB uretiliyor (ilk sefer 5-10 dakika surebilir)..."
./gradlew --no-daemon bundleRelease

AAB="app/build/outputs/bundle/release/app-release.aab"
if [ -f "$AAB" ]; then
  echo ""
  echo "=== BITTI ==="
  echo "AAB: $(pwd)/$AAB"
  ls -lh "$AAB" | awk '{print "Boyut: "$5}'
  echo ""
  echo "Bunu Google Play Console > Uretim > Yeni surum bolumune yukle."
else
  echo "HATA: AAB uretilemedi."; exit 1
fi
