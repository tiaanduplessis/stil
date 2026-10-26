#!/usr/bin/env python3
"""Check free disk space after the emulator runner installs its system image."""

from pathlib import Path
import shutil


def main():
    free = shutil.disk_usage(Path.home()).free
    required = 10 * 1024 ** 3
    print(f"Emulator disk space: {free / 1024 ** 3:.1f} GiB free")
    if free < required:
        raise SystemExit("At least 10 GiB free is required for the Android test emulator")


if __name__ == "__main__":
    main()
