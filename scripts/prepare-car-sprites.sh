#!/usr/bin/env bash
# Downloads GTA 2 car artwork sprites, flips them horizontally (art faces left;
# the game expects sprites facing right like the bundled voiture*.png files),
# and writes PNGs to $BUILD_DIR/images/voitureN.png.
set -u

BUILD_DIR="${1:-build}"
OUT_DIR="${BUILD_DIR}/images"
FALLBACK_DIR="images"
TARGET_WIDTH="${CAR_SPRITE_WIDTH:-40}"

mkdir -p "${OUT_DIR}"

if ! command -v curl >/dev/null 2>&1; then
	echo "WARNING: curl is not available; using bundled car sprites." >&2
fi

if ! command -v ffmpeg >/dev/null 2>&1; then
	echo "WARNING: ffmpeg is not available; using bundled car sprites." >&2
fi

# Source artwork (French GTA Wiki — Artworks de GTA 2):
# 1 A-Type, 2 B-Type, 3 Z-Type, 4 T-Rex
URLS=(
	"https://static.wikia.nocookie.net/gta/images/7/75/A-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090709121326&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/b/b7/B-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090709121500&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/a/af/Z-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090725135634&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/1/14/T-RexRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090725135132&path-prefix=fr"
)
NAMES=("A-Type" "B-Type" "Z-Type" "T-Rex")

prepare_sprite() {
	local index="$1"
	local url="$2"
	local name="$3"
	local output="${OUT_DIR}/voiture${index}.png"
	local fallback="${FALLBACK_DIR}/voiture${index}.png"
	local temp_file

	temp_file="$(mktemp "${TMPDIR:-/tmp}/voiture${index}.XXXXXX")"

	if [[ ! -f "${fallback}" ]]; then
		echo "WARNING: Missing fallback sprite ${fallback}; cannot prepare car ${index} (${name})." >&2
		rm -f "${temp_file}"
		return 1
	fi

	if command -v curl >/dev/null 2>&1 \
		&& command -v ffmpeg >/dev/null 2>&1 \
		&& curl -fsSL "${url}" -o "${temp_file}" \
		&& ffmpeg -y -loglevel error -i "${temp_file}" -vf hflip -update 1 -frames:v 1 "${output}"; then
		echo "Prepared voiture${index}.png from ${name} artwork (horizontally flipped)."
		rm -f "${temp_file}"
		return 0
	fi

	echo "WARNING: Failed to download or process ${name} sprite; using bundled ${fallback}." >&2
	cp "${fallback}" "${output}"
	rm -f "${temp_file}"
	return 0
}

for index in 1 2 3 4; do
	prepare_sprite "${index}" "${URLS[$((index - 1))]}" "${NAMES[$((index - 1))]}"
done

touch "${OUT_DIR}/.sprites-stamp"
