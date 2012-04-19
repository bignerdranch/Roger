#!/bin/sh

ANDROID_SDK=$1
ANDROID_MANIFEST=$2
APK_NAME=$3

WD=$(echo "$0" | sed 's /[^/]*$  ')

cd "$WD"

FAKE_MANIFEST="./AndroidManifest.xml"

PROJECT_DIR=$(echo "$ANDROID_MANIFEST" | sed 's /[^/]*$  ')
PROJECT_RESOURCES="$PROJECT_DIR"/res

PROJECT_PROPERTIES="$PROJECT_DIR"/project.properties

TARGET=$(cat "$PROJECT_PROPERTIES" | grep 'target=' | sed 's/^.*=//')

PLATFORM_JAR="$ANDROID_SDK/platforms/$TARGET/android.jar"

aapt package -F "$APK_NAME" -S "$PROJECT_RESOURCES" -M "$PWD/$FAKE_MANIFEST" -I "$PLATFORM_JAR"
