#!/usr/bin/env bash

# Resolve APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME="$(cd "`dirname \"$PRG\"`" && pwd -P)"

# Resolve JAVA_HOME if not set
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"
    fi
fi

# Resolve ANDROID_HOME if not set
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    if [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
        export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Check for portable Gradle in tools/gradle/bin/gradle
if [ -x "$APP_HOME/tools/gradle/bin/gradle" ]; then
    exec "$APP_HOME/tools/gradle/bin/gradle" "$@"
fi

# Fallback to system gradle
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

echo "ERROR: Neither tools/gradle/bin/gradle nor system gradle was found."
exit 1
