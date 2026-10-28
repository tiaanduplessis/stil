#!/usr/bin/env bash
set -euo pipefail

mkdir -p .artifacts/emulator
# sys.boot_completed can precede the package service on a slow runner; installs fail until it answers.
for attempt in $(seq 1 60); do
    adb shell pm path android > /dev/null 2>&1 && break
    [[ "$attempt" -lt 60 ]] || { echo "The emulator's package service never became ready." >&2; exit 1; }
    sleep 2
done
echo "Runner: $(nproc 2>/dev/null || echo '?') CPUs, $(free -m 2>/dev/null | awk '/^Mem:/ {print $2}') MB RAM"
# The emulator's -dpi-device flag is not honored on every image (hosted runs came up at 440 dpi), so pin the density the suite is written for.
adb shell wm density 280
density=$(adb shell wm density | tail -n 1 | tr -d '\r')
[[ "$density" == *"density: 280" ]] || { echo "The emulator density is '$density', not 280." >&2; exit 1; }
# Clearing the log only tidies diagnostics. An API 26 emulator can reject it for a minute after boot, so retry and carry on.
for attempt in 1 2 3 4 5; do
    adb logcat -c && break
    [[ "$attempt" -lt 5 ]] || echo "Could not clear the logcat buffer; continuing with the existing log." >&2
    sleep "${STIL_LOGCAT_RETRY_DELAY:-3}"
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
