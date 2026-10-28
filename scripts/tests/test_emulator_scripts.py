"""Check disk limits and the device runner's diagnostics and failure handling."""

from contextlib import redirect_stdout
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch


SCRIPTS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("check_emulator_disk", SCRIPTS / "check_emulator_disk.py")
disk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(disk)


class EmulatorDiskTest(unittest.TestCase):
    def test_accepts_exactly_ten_gibibytes(self):
        with patch.object(disk.shutil, "disk_usage", return_value=SimpleNamespace(free=10 * 1024 ** 3)):
            with redirect_stdout(io.StringIO()) as output:
                disk.main()
        self.assertIn("10.0 GiB free", output.getvalue())

    def test_rejects_one_byte_below_the_limit(self):
        with patch.object(disk.shutil, "disk_usage", return_value=SimpleNamespace(free=10 * 1024 ** 3 - 1)):
            with redirect_stdout(io.StringIO()), self.assertRaisesRegex(SystemExit, "At least 10 GiB"):
                disk.main()


class DeviceRunnerTest(unittest.TestCase):
    def run_device_script(self, gradle_status, capture_status, clear_failures=0, package_failures=0):
        with tempfile.TemporaryDirectory(prefix="stil-emulator-test-") as directory:
            root = Path(directory)
            binary = root / "bin"
            binary.mkdir()
            adb = binary / "adb"
            adb.write_text('''#!/usr/bin/env bash
set -eu
case "$1 $2" in
    'logcat -c')
        attempts=$(cat clear-attempts.txt 2>/dev/null || echo 0)
        echo $((attempts + 1)) > clear-attempts.txt
        [[ "$attempts" -ge "$STIL_CLEAR_FAILURES" ]] || { echo "failed to clear the 'main' log" >&2; exit 1; } ;;
    'logcat -v')
        echo $$ > logcat.pid
        trap 'exit 0' TERM
        echo 'device log'
        while :; do sleep 0.01; done ;;
    'shell pm')
        attempts=$(cat package-attempts.txt 2>/dev/null || echo 0)
        echo $((attempts + 1)) > package-attempts.txt
        [[ "$attempts" -ge "$STIL_PACKAGE_FAILURES" ]] || { echo "cmd: Can't find service: package" >&2; exit 20; } ;;
    'exec-out screencap')
        echo 'final screenshot'
        exit "$STIL_CAPTURE_STATUS" ;;
    *) exit 91 ;;
esac
''')
            adb.chmod(0o700)
            gradle = root / "gradlew"
            gradle.write_text('''#!/usr/bin/env bash
set -eu
printf '%s\\n' "$@" > gradle-args.txt
for ((i=0; i<200; i++)); do
    if [[ -s .artifacts/emulator/logcat.txt ]]; then
        exit "$STIL_GRADLE_STATUS"
    fi
    sleep 0.01
done
exit 92
''')
            gradle.chmod(0o700)
            env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ["PATH"],
                       STIL_GRADLE_STATUS=str(gradle_status), STIL_CAPTURE_STATUS=str(capture_status), STIL_CLEAR_FAILURES=str(clear_failures),
                       STIL_PACKAGE_FAILURES=str(package_failures), STIL_LOGCAT_RETRY_DELAY="0")
            result = subprocess.run(["bash", str(SCRIPTS / "run_device_tests.sh")], cwd=root,
                                    env=env, capture_output=True, text=True, timeout=10)
            self.assertEqual(result.returncode, gradle_status, result.stderr)
            self.assertEqual((root / ".artifacts/emulator/logcat.txt").read_text(), "device log\n")
            self.assertEqual((root / ".artifacts/emulator/final-screen.png").read_text(), "final screenshot\n")
            self.assertEqual((root / "gradle-args.txt").read_text().splitlines(), [
                ":app:connectedDebugAndroidTest", "--no-daemon", "--max-workers=1",
                "--no-parallel", "--console=plain",
            ])
            if package_failures:
                self.assertEqual((root / "package-attempts.txt").read_text().strip(), str(package_failures + 1))
            pid = int((root / "logcat.pid").read_text())
            with self.assertRaises(ProcessLookupError):
                os.kill(pid, 0)

    def test_success_captures_diagnostics_and_stops_its_logger(self):
        self.run_device_script(0, 0)

    def test_device_tests_wait_for_the_package_service(self):
        self.run_device_script(0, 0, package_failures=2)

    def test_logcat_clear_that_never_succeeds_does_not_stop_the_tests(self):
        self.run_device_script(0, 0, clear_failures=99)

    def test_early_logcat_clear_failure_is_retried(self):
        self.run_device_script(0, 0, clear_failures=1)

    def test_device_test_failure_is_preserved_after_cleanup(self):
        self.run_device_script(7, 0)

    def test_screenshot_failure_does_not_hide_the_test_failure(self):
        self.run_device_script(7, 8)


if __name__ == "__main__":
    unittest.main()
