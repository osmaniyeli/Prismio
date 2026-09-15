#!/usr/bin/env bash
# Prismio icin sadelestirilmis gradlew.
# gradle-wrapper.jar bu pakette YOK (indirilemedi).
# Bu betik once sistem gradle'ini arar, yoksa wrapper'i kurar.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$DIR" "$@"
fi

# Android Studio ile gelen gradle
for g in "$HOME"/.gradle/wrapper/dists/*/*/gradle-*/bin/gradle \
         /opt/gradle/bin/gradle \
         "$HOME"/Library/Android/sdk/../gradle/bin/gradle; do
  if [ -x "$g" ]; then exec "$g" -p "$DIR" "$@"; fi
done

echo "HATA: Gradle bulunamadi."
echo "Secenekler:"
echo "  1) Android Studio kur (gradle ile gelir) ve projeyi oradan ac"
echo "  2) Gradle kur:  https://gradle.org/install/"
exit 1
