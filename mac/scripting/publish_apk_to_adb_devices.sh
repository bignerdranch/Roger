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
APK_NAME_INITIAL=$(echo "$APK_NAME" | sed 's/\..*//')

REMOTE_PATH="/mnt/sdcard/"

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

    # find out what our actual target path should be
    LAST_NAME=$($ADB -s $DEVICE shell ls $REMOTE_PATH | grep -F "$APK_NAME_INITIAL" | sed 's/\..*//')
    echo Last name: "$LAST_NAME" >&2

    NEXT_NAME=$(echo "$LAST_NAME" | awk -F '-' ' {
        seen[$2] = true;
        count = $2;
        print "    saw this line: " $0 " count: " count > "/dev/stderr";
        if (count > max_count) 
            max_count = count;
    }

    END {
        # get rid of old versions
        for (old_version_number in seen) {
            old_filename = "'"$REMOTE_PATH$APK_NAME_INITIAL-"'" old_version_number ".apk";
            print "    removing old file: " old_filename > "/dev/stderr";
            print "    old version number was: " old_version_number > "/dev/stderr";
            system("'"$ADB -s $DEVICE shell rm "'" old_filename " &>/dev/null");
        }

        print "'"$APK_NAME_INITIAL"'-" (max_count + 1) ".apk";
    }')

    SDCARD_PATH="$REMOTE_PATH$NEXT_NAME"
    echo "next name is $NEXT_NAME"
    echo pushing our apk to the sdcard to "$SDCARD_PATH" >&2
    echo $ADB -s $DEVICE push "$APK_PATH" "$SDCARD_PATH" >&2
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
