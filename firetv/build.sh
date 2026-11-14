#!/bin/sh
# Fabrique l'application Fire TV / Android TV à partir de index.html (jeu complet, sans internet)
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/tmp/claude-0/android/sdk}; BT=$SDK/build-tools/34.0.0; AJ=$SDK/platforms/android-34/android.jar
T=/tmp/claude-0/t; GAME=${GAME:-/home/user/test/index.html}
rm -rf build && mkdir -p build/classes build/dex assets
# 1. page du jeu autonome : CSS compilé + three.js intégrés, drapeau SOEN_APP
grep -v "window.DOG_GLB\|window.HERO_GLB" "$GAME" > build/tw_src.html
echo "module.exports={content:['$(pwd)/build/tw_src.html'],theme:{extend:{}},plugins:[]}" > build/tw.config.js
(cd $T && npx tailwindcss -c /tmp/claude-0/firetv/build/tw.config.js -i in.css -o /tmp/claude-0/firetv/build/app.css --minify 2>/dev/null)
python3 - "$GAME" <<'PY'
import sys
s=open(sys.argv[1],encoding='utf-8').read()
T='/tmp/claude-0/t/node_modules/three/'
def sub(tag, new):
    global s
    assert s.count(tag)==1, tag
    s=s.replace(tag,new)
css=open('build/app.css',encoding='utf-8').read()
sub('<script src="https://cdn.tailwindcss.com/3.4.17"></script>','<script>window.SOEN_APP=true</script>\n<style>'+css+'</style>')
sub('<script src="https://cdn.jsdelivr.net/npm/three@0.147.0/build/three.min.js"></script>','<script>'+open(T+'build/three.min.js',encoding='utf-8').read()+'</script>')
sub('<script src="https://cdn.jsdelivr.net/npm/three@0.147.0/examples/js/loaders/GLTFLoader.js"></script>','<script>'+open(T+'examples/js/loaders/GLTFLoader.js',encoding='utf-8').read()+'</script>')
assert 'cdn.' not in s.split('<script>window.SOEN_APP')[0]
open('assets/index.html','w',encoding='utf-8').write(s)
PY
# 2. ressources, code, paquet
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $AJ --manifest AndroidManifest.xml -A assets build/res.zip --min-sdk-version 22 --target-sdk-version 33 --version-code ${VCODE:-1} --version-name ${VNAME:-1.0}
javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath $AJ -d build/classes src/fr/soen/tour/*.java 2>&1 | grep -v "^warning\|^Note\|^1 warning\|JAVA_TOOL" || true
[ -f build/classes/fr/soen/tour/MainActivity.class ] || { echo 'ERREUR : la compilation Java a échoué'; exit 1; }
$BT/d8 --lib $AJ --min-api 22 --output build/dex build/classes/fr/soen/tour/*.class
(cd build/dex && zip -q -j ../base.apk classes.dex)
$BT/zipalign -f -p 4 build/base.apk build/aligned.apk
[ -f soen.keystore ] || keytool -genkeypair -keystore soen.keystore -storepass soen-tour -keypass soen-tour -alias soen -keyalg RSA -keysize 2048 -validity 20000 -dname "CN=Soen" >/dev/null 2>&1
$BT/apksigner sign --ks soen.keystore --ks-pass pass:soen-tour --key-pass pass:soen-tour --ks-key-alias soen --out soen-firetv.apk build/aligned.apk
$BT/apksigner verify soen-firetv.apk && ls -la soen-firetv.apk
