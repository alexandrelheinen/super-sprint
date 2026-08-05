#!/usr/bin/env bash
# Extract Kenney Top-down Tanks Redux PNG/Retina sprites into build/sprites/kenney/.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${1:-${ROOT_DIR}/build}"
ZIP_PATH="${ROOT_DIR}/third_party/kenney-top-down-tanks-redux/kenney_topdownTanksRedux.zip"
LICENSE_SRC="${ROOT_DIR}/third_party/kenney-top-down-tanks-redux/License.txt"
OUT_DIR="${BUILD_DIR}/sprites/kenney"
STAMP="${BUILD_DIR}/sprites/.kenney-stamp"

if [[ ! -f "${ZIP_PATH}" ]]; then
	echo "Missing Kenney pack zip: ${ZIP_PATH}" >&2
	exit 1
fi

mkdir -p "${OUT_DIR}"
TMP_DIR="$(mktemp -d)"
cleanup() {
	rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

# Prefer Retina (2×) sprites; fall back to Default size if Retina is absent.
unzip -q -o "${ZIP_PATH}" "PNG/Retina/*" -d "${TMP_DIR}"
SRC_DIR="${TMP_DIR}/PNG/Retina"
if [[ ! -d "${SRC_DIR}" ]] || [[ -z "$(find "${SRC_DIR}" -maxdepth 1 -name '*.png' -print -quit)" ]]; then
	unzip -q -o "${ZIP_PATH}" "PNG/Default size/*" -d "${TMP_DIR}"
	SRC_DIR="${TMP_DIR}/PNG/Default size"
fi

# Flatten PNG folder contents into the kenney sprite folder.
find "${SRC_DIR}" -maxdepth 1 -type f -name '*.png' -exec cp -f {} "${OUT_DIR}/" \;

if [[ -f "${LICENSE_SRC}" ]]; then
	cp -f "${LICENSE_SRC}" "${OUT_DIR}/License.txt"
fi

REQUIRED=(
	tileGrass1.png
	tileGrass2.png
	tileSand1.png
	tileSand2.png
	treeGreen_large.png
	treeGreen_small.png
	treeGreen_twigs.png
	treeBrown_large.png
	treeBrown_small.png
	treeBrown_twigs.png
)
for file in "${REQUIRED[@]}"; do
	if [[ ! -f "${OUT_DIR}/${file}" ]]; then
		echo "Kenney extract missing required sprite: ${file}" >&2
		exit 1
	fi
done

touch "${STAMP}"
echo "Kenney sprites ready in ${OUT_DIR}"
