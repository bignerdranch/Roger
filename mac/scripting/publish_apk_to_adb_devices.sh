#!/bin/sh

APK_PATH=$1
shift
LAYOUT_NAME=$1
shift
LAYOUT_TYPE=$1
shift
PACKAGE_NAME=$1
shift
MIN_VERSION=$1
shift
TXN_ID=$1
shift

APK_NAME=$(echo "$APK_PATH" | sed 's ^.*/  ')
SDCARD_PATH="/mnt/sdcard/$APK_NAME"

echo Pushing to devices with adb at path: $ADB

DEVICES=$($ADB devices | sed '1 d' | awk '{print $1}')

for DEVICE in $DEVICES; do
    # start off by telling everyone what's up - there's a new apk
    # coming down the line
    echo $ADB -s $DEVICE shell am broadcast \
        -a com.bignerdranch.franklin.roger.ACTION_INCOMING_TXN \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID "$TXN_ID" >&2
    $ADB -s $DEVICE shell am broadcast \
        -a com.bignerdranch.franklin.roger.ACTION_INCOMING_TXN \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID "$TXN_ID" >&2
done

for DEVICE in $DEVICES; do 
    echo Pushing to $DEVICE... >&2

    # push our apk to the sdcard
    $ADB -s $DEVICE push "$APK_PATH" "$SDCARD_PATH"

    # then send out a broadcast intent 
    echo Sending broadcast intent like so... >&2
    echo $ADB -s $DEVICE shell am broadcast \
        -a com.bignerdranch.franklin.roger.ACTION_NEW_LAYOUT \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_APK_PATH "$SDCARD_PATH" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_LAYOUT_NAME "$LAYOUT_NAME" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_LAYOUT_TYPE "$LAYOUT_TYPE" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_PACKAGE_NAME "$PACKAGE_NAME" \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_MIN_VERSION "$MIN_VERSION" \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID "$TXN_ID" >&2
    $ADB -s $DEVICE shell am broadcast \
        -a com.bignerdranch.franklin.roger.ACTION_NEW_LAYOUT \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_APK_PATH "$SDCARD_PATH" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_LAYOUT_NAME "$LAYOUT_NAME" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_LAYOUT_TYPE "$LAYOUT_TYPE" \
        -e com.bignerdranch.franklin.roger.EXTRA_LAYOUT_PACKAGE_NAME "$PACKAGE_NAME" \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_MIN_VERSION "$MIN_VERSION" \
        --ei com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID "$TXN_ID" >&2
done