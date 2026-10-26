#!/usr/bin/env bash
set -euo pipefail

mkdir -p .artifacts/emulator
adb logcat -c
adb logcat -v threadtime > .artifacts/emulator/logcat.txt &
logcat_pid=$!

cleanup() {
    local status=$?
    trap - EXIT
    adb exec-out screencap -p > .artifacts/emulator/final-screen.png || true
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    exit "$status"
}
trap cleanup EXIT

./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=1 --no-parallel --console=plain
