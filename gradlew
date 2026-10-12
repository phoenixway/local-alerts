#!/usr/bin/env bash
# Self-bootstrapping Gradle 9.3.1 wrapper for a source-only distribution.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_SHA='b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13'
WRAPPER_URL='https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar'

if ! command -v java >/dev/null 2>&1; then
    echo 'ERROR: Java executable is not on PATH. Install JDK 17+.' >&2
    exit 2
fi
if ! command -v sha256sum >/dev/null 2>&1; then
    echo 'ERROR: sha256sum is required to verify the official Gradle wrapper.' >&2
    exit 2
fi

mkdir -p "$(dirname "$JAR")"
if [[ ! -f "$JAR" ]]; then
    echo 'Downloading official Gradle 9.3.1 wrapper JAR (one-time setup)...'
    tmp="$(mktemp "${JAR}.download.XXXXXX")"
    trap 'rm -f "$tmp"' EXIT
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 --connect-timeout 15 --max-time 180 \
            "$WRAPPER_URL" -o "$tmp" || {
            echo 'ERROR: Could not download Gradle wrapper. Check internet access.' >&2
            exit 2
        }
    elif command -v wget >/dev/null 2>&1; then
        wget -q --timeout=30 -O "$tmp" "$WRAPPER_URL" || {
            echo 'ERROR: Could not download Gradle wrapper. Check internet access.' >&2
            exit 2
        }
    else
        echo 'ERROR: Install curl or wget to download the Gradle wrapper.' >&2
        exit 2
    fi
    printf '%s  %s\n' "$WRAPPER_SHA" "$tmp" | sha256sum -c - >/dev/null || {
        echo 'ERROR: Downloaded Gradle wrapper JAR failed SHA-256 verification.' >&2
        exit 2
    }
    mv -f "$tmp" "$JAR"
    trap - EXIT
fi

printf '%s  %s\n' "$WRAPPER_SHA" "$JAR" | sha256sum -c - >/dev/null || {
    echo 'ERROR: Gradle wrapper JAR has an unexpected SHA-256 checksum.' >&2
    echo 'Remove gradle/wrapper/gradle-wrapper.jar to download a verified copy.' >&2
    exit 2
}

exec java ${JAVA_OPTS:-} -Dorg.gradle.appname=gradlew \
    -cp "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
