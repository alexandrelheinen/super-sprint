#!/usr/bin/env bash
# Headless smoke test: launch the Gradle installDist application briefly under Xvfb.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

SMOKE_TIMEOUT_SEC="${SMOKE_TIMEOUT_SEC:-5}"
APP_SCRIPT="${ROOT_DIR}/build/install/super-sprint-supelec/bin/super-sprint-supelec"

if [[ ! -x "${APP_SCRIPT}" ]]; then
	echo "Missing installed app launcher at ${APP_SCRIPT}. Run: ./gradlew installDist" >&2
	exit 1
fi

command -v xvfb-run >/dev/null 2>&1 || {
	echo "xvfb-run is required for smoke-test" >&2
	exit 1
}

xvfb-run -a timeout "${SMOKE_TIMEOUT_SEC}s" "${APP_SCRIPT}" || test $? -eq 124
echo "Smoke test passed (process started successfully)"
