#!/usr/bin/env bash

# Automatically resolve JAVA_HOME
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"
    fi
fi

# Automatically resolve ANDROID_HOME / ANDROID_SDK_ROOT
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    if [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
        export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Add adb and platform tools to PATH if present
if [ -d "/opt/homebrew/Caskroom/android-platform-tools/37.0.1/platform-tools" ]; then
    export PATH="/opt/homebrew/Caskroom/android-platform-tools/37.0.1/platform-tools:$PATH"
elif [ -d "$ANDROID_HOME/platform-tools" ]; then
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi

echo "============================================="
echo "IRed Build Environment"
echo "JAVA_HOME:    $JAVA_HOME"
echo "ANDROID_HOME: $ANDROID_HOME"
echo "============================================="
