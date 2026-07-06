#!/bin/sh
set -eu

echo "[BOOT] Starting ReqBot (PORT=${PORT:-unset}, PROFILE=${SPRING_PROFILES_ACTIVE:-unset})"

exec java \
  -XX:TieredStopAtLevel=1 \
  -Xss256k \
  -XX:MaxRAMPercentage=55.0 \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar
