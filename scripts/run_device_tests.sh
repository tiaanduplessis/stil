#!/usr/bin/env bash
set -euo pipefail

mkdir -p .artifacts/emulator
# The emulator action resizes the log buffer as it boots, which can make an early clear fail.
for attempt in 1 2 3 4 5; do
    adb logcat -c && break
    [[ "$attempt" -lt 5 ]] || exit 1
    sleep 3
done
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
