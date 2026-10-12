#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

# Self-bootstrapping, checksum-verified Gradle 9.3.1 wrapper.
# Does not copy files from checkout-guard or any other project.
./gradlew :app:assembleDebug

output='app/build/outputs/apk/debug/app-debug.apk'
if [[ ! -s "$output" ]]; then
    echo "ERROR: Build finished without an APK at $output" >&2
    exit 3
fi
mkdir -p "$HOME/Downloads"
cp "$output" "$HOME/Downloads/Tryvoha-Local-1.0.0.apk"
echo "READY: $HOME/Downloads/Tryvoha-Local-1.0.0.apk"
