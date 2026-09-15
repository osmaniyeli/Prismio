@echo off
REM Prismio - AAB uretme betigi (Windows)
cd /d "%~dp0"
echo === PRISMIO AAB URETIMI ===

if "%ANDROID_HOME%"=="" (
  echo HATA: Android SDK bulunamadi.
  echo Android Studio kurup bir kez ac, sonra:
  echo   setx ANDROID_HOME "%%LOCALAPPDATA%%\Android\Sdk"
  echo Sonra bu pencereyi kapatip yeniden ac.
  pause
  exit /b 1
)

if not exist prismio.jks (
  echo.
  echo Imza anahtari yok, uretiliyor...
  echo ONEMLI: prismio.jks dosyasini KAYBETME ve YEDEKLE.
  echo.
  keytool -genkeypair -v -keystore prismio.jks -alias prismio ^
    -keyalg RSA -keysize 2048 -validity 10000 ^
    -storepass prismio123 -keypass prismio123 ^
    -dname "CN=Prismio, OU=Oyun, O=Osmaniyeli Abdullah, L=Istanbul, C=TR"
)

echo.
echo AAB uretiliyor (ilk sefer 5-10 dakika surebilir)...
call gradlew.bat --no-daemon bundleRelease

if exist app\build\outputs\bundle\release\app-release.aab (
  echo.
  echo === BITTI ===
  echo AAB: %cd%\app\build\outputs\bundle\release\app-release.aab
  echo Bunu Google Play Console'a yukle.
) else (
  echo HATA: AAB uretilemedi.
)
pause
