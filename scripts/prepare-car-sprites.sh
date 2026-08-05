#!/usr/bin/env bash
# Slices src/sprites/cars.png (3x3 grid) into car_00.png … car_08.png,
# replaces the cyan background with transparency, trims empty margins,
# rotates each sprite to face right (game convention), scales to race size,
# and writes mean-color metadata to cars.properties.
set -euo pipefail

BUILD_DIR="${1:-build}"
OUT_DIR="${BUILD_DIR}/sprites"
CONFIG_OUT_DIR="${BUILD_DIR}/config"
BUNDLED_SPRITE_DIR="src/sprites"
BUNDLED_CONFIG_DIR="src/data/config"
SOURCE_SHEET="${BUNDLED_SPRITE_DIR}/cars.png"
GRID_SIZE=3
CAR_COUNT=$((GRID_SIZE * GRID_SIZE))
# Long axis in pixels after rotate-to-face-right (matches previous ~40px cars).
TARGET_LENGTH="${CAR_SPRITE_LENGTH:-40}"
CHROMA_TOLERANCE="${CAR_CHROMA_TOLERANCE:-40}"

mkdir -p "${OUT_DIR}" "${CONFIG_OUT_DIR}" "${BUNDLED_SPRITE_DIR}" "${BUNDLED_CONFIG_DIR}"

if [[ ! -f "${SOURCE_SHEET}" ]]; then
	echo "ERROR: Missing sprite sheet ${SOURCE_SHEET}" >&2
	exit 1
fi

if ! python3 -c "from PIL import Image" >/dev/null 2>&1; then
	echo "ERROR: Python Pillow is required to prepare car sprites from ${SOURCE_SHEET}." >&2
	echo "Install with: pip3 install Pillow   or   sudo apt-get install python3-pil" >&2
	exit 1
fi

python3 - "${SOURCE_SHEET}" "${OUT_DIR}" "${CONFIG_OUT_DIR}" "${BUNDLED_SPRITE_DIR}" "${BUNDLED_CONFIG_DIR}" \
	"${CAR_COUNT}" "${GRID_SIZE}" "${TARGET_LENGTH}" "${CHROMA_TOLERANCE}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

source_sheet = Path(sys.argv[1])
out_dir = Path(sys.argv[2])
config_out_dir = Path(sys.argv[3])
bundled_sprite_dir = Path(sys.argv[4])
bundled_config_dir = Path(sys.argv[5])
car_count = int(sys.argv[6])
grid_size = int(sys.argv[7])
target_length = int(sys.argv[8])
chroma_tolerance = int(sys.argv[9])

# Index order is row-major (left-to-right, top-to-bottom).
# Stats: acceleration (m/s²), max speed (m/s), handling index.
CAR_META = [
	(12, "Vintage Yellow Hot Rod", 16.5, 30.0, 44.0),
	(8, "Classic Green Formula", 21.0, 35.5, 34.0),
	(21, "Blue GT Coupe", 14.0, 33.0, 46.0),
	(45, "Red Flame Muscle", 18.0, 29.0, 40.0),
	(77, "Silver Open-Wheel Racer", 19.5, 38.0, 32.0),
	(56, "Brown Vintage Wagon", 10.0, 24.0, 50.0),
	(3, "Orange Classic Roadster", 15.0, 28.5, 56.0),
	(9, "Purple Retro Grand Prix", 20.0, 36.5, 36.0),
	(6, "Teal Vintage Sports", 13.5, 31.0, 52.0),
]


def iter_pixels(image: Image.Image):
	if hasattr(image, "get_flattened_data"):
		return image.get_flattened_data()
	return image.getdata()


def channel_distance(pixel, key):
	return abs(pixel[0] - key[0]) + abs(pixel[1] - key[1]) + abs(pixel[2] - key[2])


def dominant_background(image: Image.Image) -> tuple[int, int, int]:
	"""Pick the most common opaque RGB near the sheet corners (cyan key)."""
	width, height = image.size
	margin = max(8, min(width, height) // 32)
	regions = [
		(0, 0, margin, margin),
		(width - margin, 0, width, margin),
		(0, height - margin, margin, height),
		(width - margin, height - margin, width, height),
	]
	counts: dict[tuple[int, int, int], int] = {}
	for left, top, right, bottom in regions:
		region = image.crop((left, top, right, bottom))
		for pixel in iter_pixels(region):
			rgb = pixel[:3]
			counts[rgb] = counts.get(rgb, 0) + 1
	if not counts:
		return image.getpixel((0, 0))[:3]
	return max(counts.items(), key=lambda item: item[1])[0]


def key_out_cyan(cell: Image.Image, key: tuple[int, int, int], tolerance: int) -> Image.Image:
	rgba = cell.convert("RGBA")
	keyed = []
	for red, green, blue, _alpha in iter_pixels(rgba):
		if channel_distance((red, green, blue), key) <= tolerance:
			keyed.append((0, 0, 0, 0))
		else:
			keyed.append((red, green, blue, 255))
	out = Image.new("RGBA", rgba.size)
	out.putdata(keyed)
	return out


def trim_transparent(image: Image.Image) -> Image.Image:
	bbox = image.getbbox()
	if bbox is None:
		return image
	return image.crop(bbox)


def mean_opaque_color(image: Image.Image) -> tuple[int, int, int]:
	total_r = total_g = total_b = count = 0
	for red, green, blue, alpha in iter_pixels(image):
		if alpha == 0:
			continue
		total_r += red
		total_g += green
		total_b += blue
		count += 1
	if count == 0:
		return (128, 128, 128)
	return (total_r // count, total_g // count, total_b // count)


def scale_to_length(image: Image.Image, length: int) -> Image.Image:
	width, height = image.size
	if width <= 0 or height <= 0:
		return image
	# After rotate-to-face-right, width is the car length.
	scale = length / float(width)
	new_size = (
		max(1, int(round(width * scale))),
		max(1, int(round(height * scale))),
	)
	return image.resize(new_size, Image.Resampling.LANCZOS)


def cleanup_fringe(image: Image.Image, key: tuple[int, int, int], tolerance: int) -> Image.Image:
	"""Drop rescale fringe: near-transparent or reintroduced cyan key pixels."""
	cleaned = []
	for red, green, blue, alpha in iter_pixels(image):
		if alpha < 16 or channel_distance((red, green, blue), key) <= tolerance:
			cleaned.append((0, 0, 0, 0))
		else:
			cleaned.append((red, green, blue, 255))
	out = Image.new("RGBA", image.size)
	out.putdata(cleaned)
	return out


def cell_bounds(sheet_size: int, index: int) -> tuple[int, int]:
	start = (index * sheet_size) // grid_size
	end = ((index + 1) * sheet_size) // grid_size
	return start, end


sheet = Image.open(source_sheet).convert("RGBA")
sheet_w, sheet_h = sheet.size
key = dominant_background(sheet)
print(f"Using cyan key color RGB{key} with tolerance {chroma_tolerance}")

config_lines = [
	"# Generated by scripts/prepare-car-sprites.sh from src/sprites/cars.png",
	"# Do not edit by hand — re-run the sprite preparation step instead.",
	"# Fields: index, number, name, mean color, sprite size, and stats",
	"# (acceleration m/s², max speed m/s, handling index).",
	"",
]
names = []

for index in range(car_count):
	row, col = divmod(index, grid_size)
	x0, x1 = cell_bounds(sheet_w, col)
	y0, y1 = cell_bounds(sheet_h, row)
	cell = sheet.crop((x0, y0, x1, y1))
	keyed = key_out_cyan(cell, key, chroma_tolerance)
	trimmed = trim_transparent(keyed)
	if trimmed.getbbox() is None:
		raise SystemExit(f"Car cell {index} is empty after chroma key / trim")

	# Sheet cars face up; the game expects angle 0 = facing right.
	facing_right = trimmed.rotate(-90, expand=True, resample=Image.Resampling.BICUBIC)
	facing_right = trim_transparent(facing_right)
	mean_color = mean_opaque_color(facing_right)
	scaled = scale_to_length(facing_right, target_length)
	scaled = cleanup_fringe(scaled, key, chroma_tolerance)
	scaled = trim_transparent(scaled)

	file_name = f"car_{index:02d}.png"
	build_path = out_dir / file_name
	bundled_path = bundled_sprite_dir / file_name
	scaled.save(build_path, format="PNG")
	scaled.save(bundled_path, format="PNG")

	number, name, acceleration, max_speed, handling = CAR_META[index]
	names.append(name)
	width, height = scaled.size
	config_lines.extend(
		[
			f"car.{index}.index={index}",
			f"car.{index}.number={number}",
			f"car.{index}.name={name}",
			f"car.{index}.color={mean_color[0]},{mean_color[1]},{mean_color[2]}",
			f"car.{index}.width={width}",
			f"car.{index}.height={height}",
			f"car.{index}.acceleration={acceleration}",
			f"car.{index}.maxSpeed={max_speed}",
			f"car.{index}.handling={handling}",
			"",
		]
	)
	print(
		f"Prepared {file_name}: #{number} {name} "
		f"mean=RGB{mean_color} size={width}x{height}"
	)

config_lines.append("catalog.car.names=" + ",".join(names))
config_lines.append("")
config_text = "\n".join(config_lines)
for config_dir in (config_out_dir, bundled_config_dir):
	config_path = config_dir / "cars.properties"
	config_path.write_text(config_text, encoding="utf-8")
	print(f"Wrote {config_path}")
PY

# Keep the source sheet; only individual sprites are derived outputs.
if [[ ! -f "${SOURCE_SHEET}" ]]; then
	echo "ERROR: Source sheet disappeared: ${SOURCE_SHEET}" >&2
	exit 1
fi

touch "${OUT_DIR}/.sprites-stamp"
echo "Car sprites ready in ${OUT_DIR} (source sheet ${SOURCE_SHEET} preserved)."
