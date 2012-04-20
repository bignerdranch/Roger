#!/bin/sh

ANDROID_SDK=$1
ANDROID_MANIFEST=$2
APK_NAME=$3
FAKE_MANIFEST=$4


AAPT="$ANDROID_SDK"/platform-tools/aapt

WD=$(echo "$0" | sed 's /[^/]*$  ')

cd "$WD"

PROJECT_DIR=$(echo "$ANDROID_MANIFEST" | sed 's /[^/]*$  ')
PROJECT_RESOURCES="$PROJECT_DIR"/res

PROJECT_PROPERTIES="$PROJECT_DIR"/project.properties

TARGET=$(cat "$PROJECT_PROPERTIES" | grep 'target=' | sed 's/^.*=//')

PLATFORM_JAR="$ANDROID_SDK/platforms/$TARGET/android.jar"

rm -f "$APK_NAME"

# fake manifest
echo Building $APK_NAME using $FAKE_MANIFEST
$AAPT package -F "$APK_NAME" -S "$PROJECT_RESOURCES" -M "$FAKE_MANIFEST" -I "$PLATFORM_JAR" 


## real manifest
#echo Building $APK_NAME using $ANDROID_MANIFEST
#$AAPT package -F "$APK_NAME" -S "$PROJECT_RESOURCES" -M "$ANDROID_MANIFEST" -I "$PLATFORM_JAR" 
