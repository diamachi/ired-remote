#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

# Source environment
source "$DIR/tools/setup_env.sh"

echo "Building IRed Android APK..."

# Ensure local.properties exists
if [ ! -f "$DIR/local.properties" ] && [ -n "$ANDROID_HOME" ]; then
    echo "sdk.dir=$ANDROID_HOME" > "$DIR/local.properties"
fi

# Run Gradle assembleDebug
if [ -x "$DIR/gradlew" ]; then
    "$DIR/gradlew" assembleDebug --stacktrace
else
    gradle assembleDebug --stacktrace
fi

# Copy artifact to portable bin/ folder
mkdir -p "$DIR/bin"
APK_PATH="$DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$DIR/bin/ired-debug.apk"
    echo ""
    echo "SUCCESS! Build complete."
    echo "APK located at: $DIR/bin/ired-debug.apk"
    echo "Size: $(du -h "$DIR/bin/ired-debug.apk" | cut -f1)"
else
    echo "Build succeeded, but APK was not found at expected location: $APK_PATH"
fi
