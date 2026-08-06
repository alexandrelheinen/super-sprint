#!/usr/bin/env bash
# Extract Kenney scenery sprites onto the generated resources classpath root.
# Usage: scripts/prepare-kenney-sprites.sh [OUTPUT_RESOURCE_ROOT]
# Default OUTPUT_RESOURCE_ROOT: build/generated/resources/main
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_ROOT="${1:-${ROOT_DIR}/build/generated/resources/main}"
ZIP_PATH="${ROOT_DIR}/third_party/kenney-top-down-tanks-redux/kenney_topdownTanksRedux.zip"
LICENSE_SRC="${ROOT_DIR}/third_party/kenney-top-down-tanks-redux/License.txt"
OUT_DIR="${OUTPUT_ROOT}/sprites/kenney"
STAMP="${OUTPUT_ROOT}/sprites/.kenney-stamp"

# Ground tiles + green/brown trees/twigs/leaves used by RaceSceneryPainter.
REQUIRED=(
	tileGrass1.png
	tileGrass2.png
	tileSand1.png
	tileSand2.png
	treeGreen_large.png
	treeGreen_small.png
	treeGreen_twigs.png
	treeGreen_leaf.png
	treeBrown_large.png
	treeBrown_small.png
	treeBrown_twigs.png
	treeBrown_leaf.png
)

if [[ ! -f "${ZIP_PATH}" ]]; then
	echo "Missing Kenney pack zip: ${ZIP_PATH}" >&2
	exit 1
fi

rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}"

extract_one() {
	local member="$1"
	local dest_name="$2"
	local tmp
	tmp="$(mktemp -d)"
	if unzip -q -o -j "${ZIP_PATH}" "${member}" -d "${tmp}" 2>/dev/null; then
		if [[ -f "${tmp}/${dest_name}" ]]; then
			mv -f "${tmp}/${dest_name}" "${OUT_DIR}/${dest_name}"
			rm -rf "${tmp}"
			return 0
		fi
		# Basename fallback if zip member naming differs.
		local found
		found="$(find "${tmp}" -maxdepth 1 -type f -name '*.png' | head -n 1 || true)"
		if [[ -n "${found}" ]]; then
			mv -f "${found}" "${OUT_DIR}/${dest_name}"
			rm -rf "${tmp}"
			return 0
		fi
	fi
	rm -rf "${tmp}"
	return 1
}

for file in "${REQUIRED[@]}"; do
	if extract_one "PNG/Retina/${file}" "${file}"; then
		continue
	fi
	if extract_one "PNG/Default size/${file}" "${file}"; then
		continue
	fi
	echo "Kenney pack missing required sprite: ${file}" >&2
	exit 1
done

if [[ -f "${LICENSE_SRC}" ]]; then
	cp -f "${LICENSE_SRC}" "${OUT_DIR}/License.txt"
fi

touch "${STAMP}"
echo "Kenney scenery sprites ready in ${OUT_DIR} (${#REQUIRED[@]} PNGs)"
