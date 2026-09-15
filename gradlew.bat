@echo off
REM Prismio icin sadelestirilmis gradlew (Windows).
REM gradle-wrapper.jar bu pakette yok. Sistem gradle'i kullanilir.
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  gradle -p "%~dp0" %*
  exit /b %ERRORLEVEL%
)
echo HATA: Gradle bulunamadi.
echo Android Studio kur veya https://gradle.org/install/ adresinden gradle kur.
exit /b 1
