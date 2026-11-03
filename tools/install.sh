#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

source "$DIR/tools/setup_env.sh"

APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    APK="$DIR/bin/ired-debug.apk"
fi

if [ ! -f "$APK" ]; then
    echo "APK not found. Building first..."
    "$DIR/tools/build.sh"
    APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
fi

echo "Checking for connected Android devices via adb..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" || true)

if [ -z "$DEVICES" ]; then
    echo "ERROR: No Android device detected via ADB."
    echo "Please ensure:"
    echo "1. Your Oppo Find X9 Ultra is connected via USB."
    echo "2. USB Debugging is enabled in Developer Options."
    echo "3. You accepted the 'Allow USB debugging' prompt on your phone screen."
    exit 1
fi

echo "Connected device found:"
echo "$DEVICES"
echo ""
echo "Installing $APK to device..."
adb install -r "$APK"

echo ""
echo "Launching IRed Remote on device..."
adb shell am start -n com.ired.app/.MainActivity

echo "IRed is now running on your Oppo Find X9 Ultra!"
