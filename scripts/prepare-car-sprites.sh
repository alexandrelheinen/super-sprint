#!/usr/bin/env bash
# Downloads GTA 2 car artwork sprites, flips them horizontally (art faces left;
# the game expects sprites facing right like the bundled car PNG files),
# keys out the top-left background pixel, crops transparent borders, and writes
# RGBA PNGs to $BUILD_DIR/sprites/car_XX.png (zero-based, two-digit index).
set -u

BUILD_DIR="${1:-build}"
OUT_DIR="${BUILD_DIR}/sprites"
FALLBACK_DIR="src/sprites"
TARGET_WIDTH="${CAR_SPRITE_WIDTH:-40}"
CHROMA_TOLERANCE="${CAR_CHROMA_TOLERANCE:-36}"

mkdir -p "${OUT_DIR}"

if ! command -v curl >/dev/null 2>&1; then
	echo "WARNING: curl is not available; using bundled car sprites." >&2
fi

if ! command -v ffmpeg >/dev/null 2>&1; then
	echo "WARNING: ffmpeg is not available; using bundled car sprites." >&2
fi

if ! python3 -c "from PIL import Image" >/dev/null 2>&1; then
	echo "WARNING: Python Pillow is not available; sprites will not be trimmed." >&2
fi

# Source artwork (French GTA Wiki — Artworks de GTA 2):
# 0 A-Type, 1 B-Type, 2 Z-Type, 3 T-Rex
URLS=(
	"https://static.wikia.nocookie.net/gta/images/7/75/A-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090709121326&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/b/b7/B-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090709121500&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/a/af/Z-TypeRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090725135634&path-prefix=fr"
	"https://static.wikia.nocookie.net/gta/images/1/14/T-RexRL.jpg/revision/latest/scale-to-width-down/${TARGET_WIDTH}?cb=20090725135132&path-prefix=fr"
)
NAMES=("A-Type" "B-Type" "Z-Type" "T-Rex")

sprite_file_name() {
	local index="$1"
	printf 'car_%02d.png' "${index}"
}

chroma_filter() {
	local tolerance="$1"
	cat <<EOF
format=rgba,hflip,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='if(lte(abs(r(X,Y)-r(0,0))+abs(g(X,Y)-g(0,0))+abs(b(X,Y)-b(0,0)),${tolerance}),0,255)'
EOF
}

trim_transparent_png() {
	local input="$1"
	local output="$2"
	python3 - "${input}" "${output}" <<'PY'
from PIL import Image
import sys

source_path, output_path = sys.argv[1:3]
image = Image.open(source_path).convert("RGBA")
bbox = image.getbbox()
if bbox is not None:
	image = image.crop(bbox)
image.save(output_path)
PY
}

finalize_sprite() {
	local source="$1"
	local output="$2"
	if python3 -c "from PIL import Image" >/dev/null 2>&1; then
		trim_transparent_png "${source}" "${output}"
	else
		cp "${source}" "${output}"
	fi
}

prepare_sprite() {
	local index="$1"
	local url="$2"
	local name="$3"
	local file_name
	file_name="$(sprite_file_name "${index}")"
	local output="${OUT_DIR}/${file_name}"
	local fallback="${FALLBACK_DIR}/${file_name}"
	local temp_file
	local processed_file

	temp_file="$(mktemp "${TMPDIR:-/tmp}/car_${index}.XXXXXX")"
	processed_file="$(mktemp "${TMPDIR:-/tmp}/car_${index}.proc.XXXXXX.png")"

	if [[ ! -f "${fallback}" ]]; then
		echo "WARNING: Missing fallback sprite ${fallback}; cannot prepare car ${index} (${name})." >&2
		rm -f "${temp_file}" "${processed_file}"
		return 1
	fi

	if command -v curl >/dev/null 2>&1 \
		&& command -v ffmpeg >/dev/null 2>&1 \
		&& curl -fsSL "${url}" -o "${temp_file}" \
		&& ffmpeg -y -loglevel error -i "${temp_file}" \
			-vf "$(chroma_filter "${CHROMA_TOLERANCE}")" \
			-update 1 -frames:v 1 "${processed_file}" \
		&& finalize_sprite "${processed_file}" "${output}"; then
		echo "Prepared ${file_name} from ${name} artwork (flipped, chroma keyed, trimmed)."
		rm -f "${temp_file}" "${processed_file}"
		return 0
	fi

	echo "WARNING: Failed to download or process ${name} sprite; using trimmed bundled ${fallback}." >&2
	finalize_sprite "${fallback}" "${output}"
	rm -f "${temp_file}" "${processed_file}"
	return 0
}

for index in 0 1 2 3; do
	prepare_sprite "${index}" "${URLS[$index]}" "${NAMES[$index]}"
done

touch "${OUT_DIR}/.sprites-stamp"
