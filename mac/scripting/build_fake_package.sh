#!/bin/sh

ANDROID_SDK=$1
ANDROID_MANIFEST=$2
APK_NAME=$3
FAKE_PROJECT_PATH=$4


AAPT="$ANDROID_SDK"/platform-tools/aapt

WD=$(echo "$0" | sed 's /[^/]*$  ')

cd "$WD"

PROJECT_DIR=$(echo "$ANDROID_MANIFEST" | sed 's /[^/]*$  ')
PROJECT_PROPERTIES="$PROJECT_DIR"/project.properties
if [ ! -e "$PROJECT_PROPERTIES" ]; then
    PROJECT_PROPERTIES="$PROJECT_DIR"/default.properties
fi
PROJECT_RESOURCES="$PROJECT_DIR"/res
TARGET=$(cat "$PROJECT_PROPERTIES" | grep 'target=' | sed 's/^.*=//')

# for now, only handle android targets - anything else, strip down
if [ ! $(echo $TARGET | grep -q 'android-') ]; then
    TARGET=$(echo $TARGET | sed 's/^.*:/android-/')
fi

FAKE_PROJECT_RESOURCES="$FAKE_PROJECT_PATH"/res

PLATFORM_JAR="$ANDROID_SDK/platforms/$TARGET/android.jar"

rm -f "$APK_NAME"

echo Building $APK_NAME at $FAKE_PROJECT_PATH using $ANDROID_MANIFEST
echo Resources: $PROJECT_RESOURCES
echo Fake Resources: $FAKE_PROJECT_RESOURCES

$AAPT package -F "$APK_NAME" -S "$FAKE_PROJECT_RESOURCES" -M "$ANDROID_MANIFEST" -I "$PLATFORM_JAR"
exit $?
