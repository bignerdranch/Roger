#!/bin/sh

SERIAL=$1
APK_PATH=$2
PACKAGE_NAME=$3
ADB=$4

echo Reinstalling roger: $1 $2 $3 $4
# suppress success on uninstall - cocoa app is monitoring for 
# success of everything, not just this
"$ADB" -s "$SERIAL" uninstall $PACKAGE_NAME > /dev/null

"$ADB" -s "$SERIAL" install "$APK_PATH"
