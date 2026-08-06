#!/usr/bin/env bash
# Build downloadable release artifacts for Super Sprint Supelec.
#
# Produces:
#   - A portable zip with a runnable jar (needs JDK/JRE 17+ on PATH)
#   - Optionally a jpackage app-image with a bundled runtime
#
# Assets live inside the jar on the classpath; no sidecar sprite folders.
#
# Usage:
#   scripts/package-release.sh [--app-image] [--version VERSION] [--dest DIR]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

APP_NAME="SuperSprintSupelec"
MAIN_CLASS="controller.Main"
MAIN_JAR_NAME="${APP_NAME}.jar"
BUILD_APP_IMAGE=0
VERSION="0.0.0-dev"
DEST_DIR="${ROOT_DIR}/artifacts/release"
ICON_PNG="${ROOT_DIR}/src/main/resources/sprites/icon.png"

usage() {
	cat <<EOF
Usage: $(basename "$0") [options]

Options:
  --app-image          Also build a jpackage app-image for this OS
  --version VERSION    Version string embedded in artifact names (default: ${VERSION})
  --dest DIR           Output directory (default: artifacts/release)
  -h, --help           Show this help
EOF
}

while [[ $# -gt 0 ]]; do
	case "$1" in
		--app-image)
			BUILD_APP_IMAGE=1
			shift
			;;
		--version)
			VERSION="${2:?--version requires a value}"
			shift 2
			;;
		--dest)
			DEST_DIR="${2:?--dest requires a value}"
			shift 2
			;;
		-h|--help)
			usage
			exit 0
			;;
		*)
			echo "Unknown option: $1" >&2
			usage >&2
			exit 1
			;;
	esac
done

APP_VERSION="${VERSION#v}"
if [[ -z "${APP_VERSION}" ]]; then
	APP_VERSION="0.0.0"
fi

detect_platform() {
	local os
	os="$(uname -s 2>/dev/null || echo unknown)"
	case "${os}" in
		Linux*) echo "linux-x64" ;;
		Darwin*) echo "macos-x64" ;;
		MINGW*|MSYS*|CYGWIN*) echo "windows-x64" ;;
		*) echo "unknown" ;;
	esac
}

PLATFORM="$(detect_platform)"

echo "==> Resolving game jar"
if [[ -n "${PACKAGE_JAR:-}" && -f "${PACKAGE_JAR}" ]]; then
	GRADLE_JAR="${PACKAGE_JAR}"
	echo "Using prebuilt jar from Gradle task: ${GRADLE_JAR}"
else
	echo "==> Building jar with Gradle"
	./gradlew --no-daemon jar -PappVersion="${APP_VERSION}"
	GRADLE_JAR="$(ls -1 build/libs/super-sprint-supelec-*.jar | head -n 1)"
fi
if [[ ! -f "${GRADLE_JAR}" ]]; then
	echo "Gradle jar not found (PACKAGE_JAR=${PACKAGE_JAR:-unset})" >&2
	exit 1
fi

STAGE_DIR="${DEST_DIR}/staging/${APP_NAME}-${APP_VERSION}"
INPUT_DIR="${DEST_DIR}/jpackage-input"
rm -rf "${STAGE_DIR}" "${INPUT_DIR}"
mkdir -p "${STAGE_DIR}" "${INPUT_DIR}" "${DEST_DIR}"

cp "${GRADLE_JAR}" "${INPUT_DIR}/${MAIN_JAR_NAME}"
cp "${GRADLE_JAR}" "${STAGE_DIR}/${MAIN_JAR_NAME}"

cat > "${STAGE_DIR}/run.sh" <<EOF
#!/usr/bin/env bash
set -euo pipefail
HERE="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
exec java -jar "\${HERE}/${MAIN_JAR_NAME}" "\$@"
EOF
chmod +x "${STAGE_DIR}/run.sh"

cat > "${STAGE_DIR}/run.bat" <<EOF
@echo off
setlocal
set "HERE=%~dp0"
java -jar "%HERE%${MAIN_JAR_NAME}" %*
EOF

cat > "${STAGE_DIR}/README.txt" <<EOF
Super Sprint Supelec ${APP_VERSION}
=================================

This portable package needs a JDK or JRE 17+ on your PATH.
Sprites and config are embedded in the jar.

Linux / macOS:
  ./run.sh

Windows:
  run.bat

For a fully standalone build (no Java install required), download the
platform app-image zip/tarball from the same GitHub Release.
EOF

PORTABLE_ZIP="${DEST_DIR}/${APP_NAME}-${APP_VERSION}-portable.zip"
echo "==> Writing ${PORTABLE_ZIP}"
rm -f "${PORTABLE_ZIP}"
python3 - <<PY
import pathlib
import zipfile

staging = pathlib.Path(${DEST_DIR@Q}) / "staging"
folder_name = ${APP_NAME@Q} + "-" + ${APP_VERSION@Q}
root = staging / folder_name
out = pathlib.Path(${PORTABLE_ZIP@Q})
with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob("*")):
        if path.is_file():
            archive.write(path, arcname=str(path.relative_to(staging)))
print(f"Wrote {out} ({out.stat().st_size} bytes)")
PY

if [[ "${BUILD_APP_IMAGE}" -eq 1 ]]; then
	if ! command -v jpackage >/dev/null 2>&1; then
		echo "jpackage not found; install a full JDK 17+ to build app-images" >&2
		exit 1
	fi

	APPIMAGE_OUT="${DEST_DIR}/app-image"
	rm -rf "${APPIMAGE_OUT}"
	mkdir -p "${APPIMAGE_OUT}"

	JPACKAGE_ARGS=(
		--type app-image
		--name "${APP_NAME}"
		--app-version "${APP_VERSION}"
		--description "Super Sprint Supelec arcade racing game"
		--vendor "Super Sprint Supelec"
		--input "${INPUT_DIR}"
		--main-jar "${MAIN_JAR_NAME}"
		--main-class "${MAIN_CLASS}"
		--dest "${APPIMAGE_OUT}"
	)

	if [[ -f "${ICON_PNG}" ]]; then
		case "${PLATFORM}" in
			linux-x64|macos-x64)
				JPACKAGE_ARGS+=(--icon "${ICON_PNG}")
				;;
		esac
	fi

	echo "==> Building jpackage app-image for ${PLATFORM}"
	jpackage "${JPACKAGE_ARGS[@]}"

	ARCHIVE="${DEST_DIR}/${APP_NAME}-${APP_VERSION}-${PLATFORM}"
	rm -f "${ARCHIVE}.zip" "${ARCHIVE}.tar.gz"
	if [[ "${PLATFORM}" == windows-x64 ]]; then
		python3 - <<PY
import pathlib
import zipfile

appimage = pathlib.Path(${APPIMAGE_OUT@Q})
folder_name = ${APP_NAME@Q}
root = appimage / folder_name
out = pathlib.Path(${ARCHIVE@Q} + ".zip")
with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob("*")):
        if path.is_file():
            archive.write(path, arcname=str(path.relative_to(appimage)))
print(f"Wrote {out} ({out.stat().st_size} bytes)")
PY
	else
		tar -C "${APPIMAGE_OUT}" -czf "${ARCHIVE}.tar.gz" "${APP_NAME}"
		echo "==> Wrote ${ARCHIVE}.tar.gz"
	fi
fi

echo "==> Release artifacts in ${DEST_DIR}:"
python3 - <<PY
import pathlib
dest = pathlib.Path(${DEST_DIR@Q})
for path in sorted(dest.iterdir()):
    if path.suffix in {".zip", ".gz"} or path.name.endswith(".tar.gz"):
        print(f"  {path.name} ({path.stat().st_size} bytes)")
PY

echo "Done."
