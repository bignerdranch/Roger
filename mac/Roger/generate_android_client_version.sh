#!/bin/sh

CLIENT_VERSION_ID=$(uuidgen)

GENERATED_JAVA_FILE=../../Android/Roger/gen/com/bignerdranch/franklin/roger/RogerBuild.java

cat > $GENERATED_JAVA_FILE <<EOF
package com.bignerdranch.franklin.roger;

public class RogerBuild {
    public static final String CLIENT_VERSION_ID = "$CLIENT_VERSION_ID";
}
EOF

GENERATED_HEADER_FILE=RogerBuild.h

cat > $GENERATED_HEADER_FILE <<EOF
// Automatically generated build constants.
#define kClientVersionId @"$CLIENT_VERSION_ID"
EOF
