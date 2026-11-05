#!/bin/bash
# 专用版 C 一键打包（release）。用法：
#   ANDROID_HOME=/opt/android-sdk GRADLE=/opt/gradle-9.3.1/bin/gradle ./build-apk-c.sh
# 默认路径按本地构建机写，可用环境变量覆盖。产物在 ./apk-out-c/。
set -e
cd "$(dirname "$0")"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
GRADLE="${GRADLE:-/opt/gradle-9.3.1/bin/gradle}"
OUT_DIR="${OUT_DIR:-./apk-out-c}"
"$GRADLE" assembleRelease --console=plain
mkdir -p "$OUT_DIR"
cp app/build/outputs/apk/release/*-release.apk "$OUT_DIR"/ 2>/dev/null || true
ls -la "$OUT_DIR"/
echo "BUILD_DONE"
