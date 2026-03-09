#!/usr/bin/env python3
"""Enforce that core/ contains only pure Kotlin — no Android SDK dependencies.

Checks:
  1. No `import android.*` or `import androidx.*` in core/ files
  2. No `import dev.ashera.slumbr.android.*` in core/ files (dependency inversion)

Exit code 0 = clean, 1 = violations found.
"""

import re
import sys
from pathlib import Path

CORE_DIR = Path("app/src/main/java/dev/ashera/slumbr/core")

ANDROID_SDK_IMPORT = re.compile(r"^\s*import\s+(android|androidx)\.")
ANDROID_PKG_IMPORT = re.compile(r"^\s*import\s+dev\.ashera\.slumbr\.android\.")


def check_file(path: Path) -> list[str]:
    violations: list[str] = []
    for lineno, line in enumerate(path.read_text().splitlines(), start=1):
        if ANDROID_SDK_IMPORT.match(line):
            violations.append(f"  {path}:{lineno}  {line.strip()}")
        elif ANDROID_PKG_IMPORT.match(line):
            violations.append(f"  {path}:{lineno}  {line.strip()}")
    return violations


def main() -> int:
    if not CORE_DIR.is_dir():
        print(f"ERROR: core/ directory not found at {CORE_DIR}")
        return 1

    all_violations: list[str] = []
    for kt_file in sorted(CORE_DIR.rglob("*.kt")):
        all_violations.extend(check_file(kt_file))

    if all_violations:
        print(f"FAILED: {len(all_violations)} Android import(s) found in core/:\n")
        print("\n".join(all_violations))
        print("\ncore/ must contain only pure Kotlin — no android.*/androidx.* imports,")
        print("and no imports from dev.ashera.slumbr.android.*")
        return 1

    print("OK: core/ boundary is clean — no Android dependencies found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
