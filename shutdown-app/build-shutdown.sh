#!/bin/bash
# 关机 APK 最小手工打包：javac -> d8 -> aapt2 -> zipalign -> apksigner
# 签名 keystore 与口令只留在本目录（keystore.pass 600），不进记忆、不进回复。
set -e
cd /opt/rea-lab/work/shutdown-app
SDK=/opt/android-sdk
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-36/android.jar

python3 make-icon.py
rm -rf out && mkdir -p out/classes out/dex
javac -source 8 -target 8 -cp "$JAR" -d out/classes src/org/moontechlab/shutdown/MainActivity.java
"$BT/d8" --lib "$JAR" --output out/dex out/classes/org/moontechlab/shutdown/MainActivity.class
"$BT/aapt2" compile --dir res -o out/compiled.zip
"$BT/aapt2" link -o out/app-unsigned.apk -I "$JAR" --manifest AndroidManifest.xml out/compiled.zip --auto-add-overlay
python3 -c "import zipfile; z=zipfile.ZipFile('out/app-unsigned.apk','a'); z.write('out/dex/classes.dex','classes.dex'); z.close()"
"$BT/zipalign" -f 4 out/app-unsigned.apk out/app-aligned.apk
if [ ! -f shutdown.jks ]; then
  PASS=$(openssl rand -hex 12)
  echo "$PASS" > keystore.pass; chmod 600 keystore.pass
  keytool -genkeypair -keystore shutdown.jks -alias shutdown -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PASS" -keypass "$PASS" -dname "CN=HomeMedia, O=HomeMedia"
fi
PASS=$(cat keystore.pass)
"$BT/apksigner" sign --ks shutdown.jks --ks-pass pass:"$PASS" --key-pass pass:"$PASS" --out out/shutdown.apk out/app-aligned.apk
"$BT/apksigner" verify out/shutdown.apk && echo ALLDONE
"$BT/aapt2" dump badging out/shutdown.apk | grep -E "package:|application-label"
