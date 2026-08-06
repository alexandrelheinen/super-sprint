#!/usr/bin/env bash
# Slices src/main/resources/sprites/cars.png (3x3 grid) into race and menu car
# sprites, replaces cyan-ish background with a soft alpha matte (color unmix),
# trims empty margins, rotates each sprite to face right (game convention),
# scales to race + menu sizes, and writes mean-color metadata to cars.properties
# under the generated resources root (no source-tree mutation).
#
# Usage: scripts/prepare-car-sprites.sh [OUTPUT_RESOURCE_ROOT]
# Default OUTPUT_RESOURCE_ROOT: build/generated/resources/main
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_ROOT="${1:-${ROOT_DIR}/build/generated/resources/main}"
OUT_DIR="${OUTPUT_ROOT}/sprites"
CONFIG_OUT_DIR="${OUTPUT_ROOT}/data/config"
SOURCE_SHEET="${ROOT_DIR}/src/main/resources/sprites/cars.png"
GRID_SIZE=3
CAR_COUNT=$((GRID_SIZE * GRID_SIZE))
# Long axis in pixels after rotate-to-face-right.
RACE_LENGTH="${CAR_SPRITE_LENGTH:-40}"
MENU_LENGTH="${CAR_MENU_SPRITE_LENGTH:-200}"
# Fixed race-setup preview canvas (matches AppShell CAR_PREVIEW_*).
MENU_CANVAS_WIDTH="${CAR_MENU_CANVAS_WIDTH:-200}"
MENU_CANVAS_HEIGHT="${CAR_MENU_CANVAS_HEIGHT:-110}"
# Near-key L1 distance treated as definite background (sheet key ≈ RGB(18,201,215)).
CHROMA_HARD="${CAR_CHROMA_HARD:-95}"
# Soft matte band width in pixels around background.
CHROMA_FRINGE="${CAR_CHROMA_FRINGE:-4}"

mkdir -p "${OUT_DIR}" "${CONFIG_OUT_DIR}"

if [[ ! -f "${SOURCE_SHEET}" ]]; then
	echo "ERROR: Missing sprite sheet ${SOURCE_SHEET}" >&2
	exit 1
fi

if ! python3 -c "from PIL import Image" >/dev/null 2>&1; then
	echo "ERROR: Python Pillow is required to prepare car sprites from ${SOURCE_SHEET}." >&2
	echo "Install with: pip3 install Pillow   or   sudo apt-get install python3-pil" >&2
	exit 1
fi

python3 - "${SOURCE_SHEET}" "${OUT_DIR}" "${CONFIG_OUT_DIR}" \
	"${CAR_COUNT}" "${GRID_SIZE}" "${RACE_LENGTH}" "${MENU_LENGTH}" \
	"${MENU_CANVAS_WIDTH}" "${MENU_CANVAS_HEIGHT}" "${CHROMA_HARD}" "${CHROMA_FRINGE}" <<'PY'
from __future__ import annotations

import sys
from collections import deque
from pathlib import Path

from PIL import Image

source_sheet = Path(sys.argv[1])
out_dir = Path(sys.argv[2])
config_out_dir = Path(sys.argv[3])
car_count = int(sys.argv[4])
grid_size = int(sys.argv[5])
race_length = int(sys.argv[6])
menu_length = int(sys.argv[7])
menu_canvas_width = int(sys.argv[8])
menu_canvas_height = int(sys.argv[9])
chroma_hard = int(sys.argv[10])
chroma_fringe = int(sys.argv[11])

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


def cyan_amount(pixel) -> float:
	"""How cyan-biased a color is: high for keying cyan, low/neg for car paint."""
	return (pixel[1] + pixel[2]) / 2.0 - pixel[0]


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


def unmix_foreground(pixel, key, alpha: float) -> tuple[float, float, float]:
	"""Recover FG from P = α·F + (1-α)·K (Smith/Blinn-style chroma unmix)."""
	if alpha < 1e-4:
		return (0.0, 0.0, 0.0)
	inv = 1.0 - alpha
	return (
		(pixel[0] - inv * key[0]) / alpha,
		(pixel[1] - inv * key[1]) / alpha,
		(pixel[2] - inv * key[2]) / alpha,
	)


def despill_cyan(fr: float, fg: float, fb: float, alpha: float) -> tuple[float, float, float]:
	"""Pull residual cyan spill in G/B toward R; stronger when α is low."""
	spill = max(0.0, (fg + fb) / 2.0 - fr)
	if spill <= 0:
		return fr, fg, fb
	# Low-α fringe is almost always contaminated - remove most/all spill.
	strength = min(1.0, (1.0 - alpha) * 1.0 + 0.45)
	fg = fr + (fg - fr) * (1.0 - strength)
	fb = fr + (fb - fr) * (1.0 - strength)
	return fr, fg, fb


def soft_key_cyan(cell: Image.Image, key: tuple[int, int, int], t_hard: int, fringe_radius: int) -> Image.Image:
	"""
	Intelligent cyan key:
	1) Hard-remove near-key pixels everywhere (including enclosed pockets).
	2) In a border band around background, estimate key weight from cyan-ness
	   and unmix so "half cyan + half paint" becomes paint at 50% alpha.
	Interior car pixels (including teal paint) stay untouched.
	"""
	rgba = cell.convert("RGBA")
	width, height = rgba.size
	pixels = list(iter_pixels(rgba))
	count = width * height
	key_cyan = max(1.0, cyan_amount(key))

	bg = bytearray(count)
	for index, pixel in enumerate(pixels):
		if channel_distance(pixel, key) <= t_hard:
			bg[index] = 1

	dist = [0 if bg[index] else 10**9 for index in range(count)]
	queue: deque[tuple[int, int]] = deque()
	for index in range(count):
		if bg[index]:
			queue.append((index % width, index // width))
	while queue:
		x, y = queue.popleft()
		current = dist[y * width + x]
		if current >= fringe_radius:
			continue
		for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
			if nx < 0 or ny < 0 or nx >= width or ny >= height:
				continue
			nindex = ny * width + nx
			if dist[nindex] > current + 1:
				dist[nindex] = current + 1
				queue.append((nx, ny))

	keyed = []
	for index, pixel in enumerate(pixels):
		red, green, blue, _alpha = pixel
		if bg[index]:
			keyed.append((0, 0, 0, 0))
			continue
		if dist[index] > fringe_radius:
			keyed.append((red, green, blue, 255))
			continue

		# Border matte from cyan amount: pure key → α=0, no cyan → α=1.
		key_weight = max(0.0, min(1.0, cyan_amount(pixel) / key_cyan))
		# Also treat very near-key colors as mostly background even if ca is noisy.
		near = channel_distance(pixel, key) / max(1.0, float(t_hard + 80))
		key_weight = max(key_weight, max(0.0, 1.0 - near))
		alpha = 1.0 - key_weight
		alpha = max(0.0, min(1.0, alpha))
		# Smoothstep for softer silhouettes.
		alpha = alpha * alpha * (3.0 - 2.0 * alpha)
		if alpha < 0.04:
			keyed.append((0, 0, 0, 0))
			continue

		fr, fg, fb = unmix_foreground(pixel, key, alpha)
		fr = max(0.0, min(255.0, fr))
		fg = max(0.0, min(255.0, fg))
		fb = max(0.0, min(255.0, fb))
		fr, fg, fb = despill_cyan(fr, fg, fb, alpha)
		# If unmix still looks like keying cyan, drop the pixel.
		still_cyan = cyan_amount((fr, fg, fb))
		if still_cyan > 40 and (alpha < 0.55 or channel_distance((fr, fg, fb), key) <= t_hard + 60):
			keyed.append((0, 0, 0, 0))
			continue
		if still_cyan > key_cyan * 0.45 and channel_distance((fr, fg, fb), key) <= t_hard + 40:
			keyed.append((0, 0, 0, 0))
			continue
		keyed.append((int(round(fr)), int(round(fg)), int(round(fb)), int(round(alpha * 255.0))))

	out = Image.new("RGBA", (width, height))
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
		if alpha < 200:
			continue
		total_r += red
		total_g += green
		total_b += blue
		count += 1
	if count == 0:
		return (128, 128, 128)
	return (total_r // count, total_g // count, total_b // count)


def resize_premultiplied(image: Image.Image, size: tuple[int, int]) -> Image.Image:
	"""Resize in premultiplied alpha so transparent RGB cannot bleed chroma."""
	premultiplied = []
	for red, green, blue, alpha in iter_pixels(image):
		if alpha <= 0:
			premultiplied.append((0, 0, 0, 0))
		elif alpha >= 255:
			premultiplied.append((red, green, blue, 255))
		else:
			premultiplied.append((red * alpha // 255, green * alpha // 255, blue * alpha // 255, alpha))
	buffer = Image.new("RGBA", image.size)
	buffer.putdata(premultiplied)
	scaled = buffer.resize(size, Image.Resampling.LANCZOS)
	straight = []
	for red, green, blue, alpha in iter_pixels(scaled):
		if alpha < 8:
			straight.append((0, 0, 0, 0))
		elif alpha >= 255:
			straight.append((red, green, blue, 255))
		else:
			straight.append(
				(
					min(255, (red * 255) // alpha),
					min(255, (green * 255) // alpha),
					min(255, (blue * 255) // alpha),
					alpha,
				)
			)
	out = Image.new("RGBA", size)
	out.putdata(straight)
	return out


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
	return resize_premultiplied(image, new_size)


def fit_on_canvas(image: Image.Image, canvas_width: int, canvas_height: int) -> Image.Image:
	"""Center the sprite on a fixed transparent canvas, preserving aspect ratio."""
	width, height = image.size
	if width <= 0 or height <= 0:
		return Image.new("RGBA", (canvas_width, canvas_height), (0, 0, 0, 0))
	scale = min(canvas_width / float(width), canvas_height / float(height))
	fit_size = (
		max(1, int(round(width * scale))),
		max(1, int(round(height * scale))),
	)
	fitted = resize_premultiplied(image, fit_size) if fit_size != (width, height) else image
	canvas = Image.new("RGBA", (canvas_width, canvas_height), (0, 0, 0, 0))
	offset = ((canvas_width - fitted.size[0]) // 2, (canvas_height - fitted.size[1]) // 2)
	canvas.paste(fitted, offset, fitted)
	return canvas


def cleanup_cyan_fringe(image: Image.Image, key: tuple[int, int, int], t_hard: int) -> Image.Image:
	"""Remove rescale/key leftovers that are still cyan-ish, preserving soft alpha."""
	key_cyan = max(1.0, cyan_amount(key))
	width, height = image.size
	pixels = list(iter_pixels(image))

	def at(x: int, y: int) -> int:
		return y * width + x

	def edge_band(source_pixels, radius: int) -> list[bool]:
		band = [False] * len(source_pixels)
		for y in range(height):
			for x in range(width):
				index = at(x, y)
				if source_pixels[index][3] == 0:
					continue
				for ny in range(max(0, y - radius), min(height, y + radius + 1)):
					for nx in range(max(0, x - radius), min(width, x + radius + 1)):
						if source_pixels[at(nx, ny)][3] == 0:
							band[index] = True
							break
					if band[index]:
						break
				if x < radius or y < radius or x >= width - radius or y >= height - radius:
					band[index] = True
		return band

	def clean_once(source_pixels):
		near_transparent = edge_band(source_pixels, 2)
		cleaned = []
		for index, (red, green, blue, alpha) in enumerate(source_pixels):
			if alpha == 0:
				cleaned.append((0, 0, 0, 0))
				continue
			pixel = (red, green, blue)
			amount = cyan_amount(pixel)
			distance = channel_distance(pixel, key)
			# Pure / near-pure cyan artifacts (common after filtering).
			if (red < 50 and green > 180 and blue > 180) or (distance <= t_hard + 25 and amount > key_cyan * 0.65):
				cleaned.append((0, 0, 0, 0))
				continue
			# Edge pixels with cyan tint (incl. muted teal bumper AA).
			edge_cyan = (
				near_transparent[index]
				and amount > 25
				and green >= red + 15
				and blue >= red + 15
				and green >= 70
				and blue >= 70
			)
			if edge_cyan:
				key_weight = max(0.0, min(1.0, amount / key_cyan))
				alpha_f = (alpha / 255.0) * (1.0 - max(key_weight, 0.4))
				fr, fg, fb = unmix_foreground(pixel, key, max(alpha_f, 1e-3))
				fr = max(0.0, min(255.0, fr))
				fg = max(0.0, min(255.0, fg))
				fb = max(0.0, min(255.0, fb))
				fr, fg, fb = despill_cyan(fr, fg, fb, min(alpha_f, 0.3))
				if alpha_f < 0.12 or cyan_amount((fr, fg, fb)) > 18:
					cleaned.append((0, 0, 0, 0))
				else:
					cleaned.append((int(round(fr)), int(round(fg)), int(round(fb)), int(round(alpha_f * 255.0))))
				continue
			# Soft pixels with leftover cyan tint (silver/white AA mixed with key).
			if alpha < 230 and amount > 35 and green > 160 and blue > 160:
				key_weight = max(0.0, min(1.0, amount / key_cyan))
				alpha_f = (alpha / 255.0) * (1.0 - key_weight)
				if alpha_f < 0.08 or amount > 50:
					cleaned.append((0, 0, 0, 0))
					continue
				fr, fg, fb = unmix_foreground(pixel, key, max(alpha_f, 1e-3))
				fr = max(0.0, min(255.0, fr))
				fg = max(0.0, min(255.0, fg))
				fb = max(0.0, min(255.0, fb))
				fr, fg, fb = despill_cyan(fr, fg, fb, alpha_f)
				cleaned.append((int(round(fr)), int(round(fg)), int(round(fb)), int(round(alpha_f * 255.0))))
				continue
			if amount > 70 and (alpha < 230 or distance <= t_hard + 50):
				key_weight = max(0.0, min(1.0, amount / key_cyan))
				alpha_f = (alpha / 255.0) * (1.0 - key_weight)
				if alpha_f < 0.06:
					cleaned.append((0, 0, 0, 0))
					continue
				fr, fg, fb = unmix_foreground(pixel, key, max(alpha_f, 1e-3))
				fr = max(0.0, min(255.0, fr))
				fg = max(0.0, min(255.0, fg))
				fb = max(0.0, min(255.0, fb))
				fr, fg, fb = despill_cyan(fr, fg, fb, alpha_f)
				if cyan_amount((fr, fg, fb)) > 40:
					cleaned.append((0, 0, 0, 0))
				else:
					cleaned.append((int(round(fr)), int(round(fg)), int(round(fb)), int(round(alpha_f * 255.0))))
			else:
				cleaned.append((red, green, blue, alpha))
		return cleaned

	# Second pass catches cyan that becomes edge-adjacent after the first drop.
	cleaned = clean_once(clean_once(pixels))
	out = Image.new("RGBA", image.size)
	out.putdata(cleaned)
	return out


def cell_bounds(sheet_size: int, index: int) -> tuple[int, int]:
	start = (index * sheet_size) // grid_size
	end = ((index + 1) * sheet_size) // grid_size
	return start, end


def save_sprite(image: Image.Image, file_name: str) -> None:
	image.save(out_dir / file_name, format="PNG")


sheet = Image.open(source_sheet).convert("RGBA")
sheet_w, sheet_h = sheet.size
key = dominant_background(sheet)
print(
	f"Using cyan key color RGB{key} "
	f"(hard≤{chroma_hard}, fringe={chroma_fringe}px, race={race_length}px, "
	f"menu={menu_length}px on {menu_canvas_width}x{menu_canvas_height})"
)

config_lines = [
	"# Generated by scripts/prepare-car-sprites.sh from src/main/resources/sprites/cars.png",
	"# Do not edit by hand - re-run the sprite preparation step instead.",
	"# Fields: index, number, name, mean color, race sprite size, opaque pixel",
	"# count, and stats (acceleration m/s², max speed m/s, handling index).",
	"# Menu sprites are car_XX_menu.png (larger); race sprites are car_XX.png.",
	"# Mass uses cars.kilogramsPerOpaquePixel from the green reference car.",
	"",
]
names = []
opaque_pixel_counts = []
# Classic Green Formula (index 1) and Purple Retro GP are ~500 kg vintage racers.
REFERENCE_MASS_KG = 500.0
REFERENCE_MODEL_INDEX = 1

for index in range(car_count):
	row, col = divmod(index, grid_size)
	x0, x1 = cell_bounds(sheet_w, col)
	y0, y1 = cell_bounds(sheet_h, row)
	cell = sheet.crop((x0, y0, x1, y1))
	keyed = soft_key_cyan(cell, key, chroma_hard, chroma_fringe)
	trimmed = trim_transparent(keyed)
	if trimmed.getbbox() is None:
		raise SystemExit(f"Car cell {index} is empty after chroma key / trim")

	# Sheet cars face up; the game expects angle 0 = facing right.
	facing_right = trimmed.rotate(-90, expand=True, resample=Image.Resampling.BICUBIC)
	facing_right = cleanup_cyan_fringe(facing_right, key, chroma_hard)
	facing_right = trim_transparent(facing_right)
	mean_color = mean_opaque_color(facing_right)

	race = scale_to_length(facing_right, race_length)
	race = cleanup_cyan_fringe(race, key, chroma_hard)
	race = trim_transparent(race)
	opaque_pixels = sum(1 for _red, _green, _blue, alpha in iter_pixels(race) if alpha > 0)
	opaque_pixel_counts.append(opaque_pixels)

	menu = scale_to_length(facing_right, menu_length)
	menu = cleanup_cyan_fringe(menu, key, chroma_hard)
	menu = trim_transparent(menu)
	# Fixed canvas so race-setup preview slots never change height per model.
	menu = fit_on_canvas(menu, menu_canvas_width, menu_canvas_height)

	race_name = f"car_{index:02d}.png"
	menu_name = f"car_{index:02d}_menu.png"
	save_sprite(race, race_name)
	save_sprite(menu, menu_name)

	number, name, acceleration, max_speed, handling = CAR_META[index]
	names.append(name)
	width, height = race.size
	config_lines.extend(
		[
			f"car.{index}.index={index}",
			f"car.{index}.number={number}",
			f"car.{index}.name={name}",
			f"car.{index}.color={mean_color[0]},{mean_color[1]},{mean_color[2]}",
			f"car.{index}.width={width}",
			f"car.{index}.height={height}",
			f"car.{index}.menuWidth={menu_canvas_width}",
			f"car.{index}.menuHeight={menu_canvas_height}",
			f"car.{index}.opaquePixels={opaque_pixels}",
			f"car.{index}.acceleration={acceleration}",
			f"car.{index}.maxSpeed={max_speed}",
			f"car.{index}.handling={handling}",
			"",
		]
	)
	print(
		f"Prepared {race_name} + {menu_name}: #{number} {name} "
		f"mean=RGB{mean_color} race={width}x{height} opaque={opaque_pixels} "
		f"menu={menu_canvas_width}x{menu_canvas_height}"
	)

reference_pixels = opaque_pixel_counts[REFERENCE_MODEL_INDEX]
if reference_pixels <= 0:
	raise SystemExit(f"Reference car {REFERENCE_MODEL_INDEX} has no opaque pixels")
kilograms_per_opaque_pixel = REFERENCE_MASS_KG / float(reference_pixels)
config_lines.extend(
	[
		f"cars.referenceMassKg={REFERENCE_MASS_KG:g}",
		f"cars.referenceModelIndex={REFERENCE_MODEL_INDEX}",
		f"cars.kilogramsPerOpaquePixel={kilograms_per_opaque_pixel:.8f}",
		"",
	]
)
print(
	f"Mass scale: {REFERENCE_MASS_KG:g} kg / {reference_pixels} green opaque px "
	f"= {kilograms_per_opaque_pixel:.6f} kg/px"
)

config_lines.append("catalog.car.names=" + ",".join(names))
config_lines.append("")
config_text = "\n".join(config_lines)
config_path = config_out_dir / "cars.properties"
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
