#!/usr/bin/env sh
# minimal wrapper
set -e
GRADLE_URL="https://services.gradle.org/distributions/gradle-8.5-bin.zip"
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Downloading gradle wrapper..."
fi
exec gradle "$@"
