#!/usr/bin/env bash
# Generates track preview PNGs via the Gradle JavaExec task.
# Prefer: ./gradlew generateTrackPreviews
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
./gradlew generateTrackPreviews
