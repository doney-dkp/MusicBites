#!/bin/bash
export JAVA_HOME="/tmp/jdk-17.0.13+11"
export ANDROID_SDK_ROOT="/tmp/android-sdk"
export ANDROID_HOME="/tmp/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

export MUSIC_RELEASE_KEYSTORE_FILE="/home/doney/github/MusicBites/musicbites-release.jks"
export MUSIC_RELEASE_SIGNING_STORE_PASSWORD="musicbites123"
export MUSIC_RELEASE_KEY_ALIAS="release"
export MUSIC_RELEASE_SIGNING_KEY_PASSWORD="musicbites123"

cd /home/doney/github/MusicBites

# Write local.properties
echo "sdk.dir=/tmp/android-sdk" > local.properties

java -version 2>&1
./gradlew assembleFossRelease
