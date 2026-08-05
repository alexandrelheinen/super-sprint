#!/usr/bin/env python3
"""Generate wide Super Sprint track tiles in a Kenney-inspired style.

Lane geometry matches Circuit.INNER_RADIUS / OUTER_RADIUS / TILE_SIZE so
physics stays valid. Visual language borrows cool asphalt banding from
Kenney's Top-down Tanks Redux roads (CC0 inspiration only — tiles are
original, not cropped from that pack).

Future idea: replace flora + track + cars with one unified sprite set
(or original art) instead of mixing packs / generated tiles.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

TILE = 219
INNER = 26
OUTER = 191

# Cool Kenney-like asphalt palette (teal-gray), not pure Super Sprint charcoal.
ASPHALT = np.array([142, 164, 170], dtype=np.float32)
ASPHALT_DARK = np.array([108, 128, 134], dtype=np.float32)
ASPHALT_MID = np.array([128, 150, 156], dtype=np.float32)
ASPHALT_LIGHT = np.array([168, 188, 194], dtype=np.float32)
EDGE_SHADOW = np.array([78, 96, 102], dtype=np.float32)
EDGE_HIGHLIGHT = np.array([196, 214, 218], dtype=np.float32)
# Softer curb accents than classic arcade red/white.
CURB_A = np.array([214, 120, 96], dtype=np.float32)
CURB_B = np.array([236, 232, 220], dtype=np.float32)

OUT_DIR = Path(__file__).resolve().parents[1] / "src" / "sprites"


def blank() -> np.ndarray:
	return np.zeros((TILE, TILE, 4), dtype=np.float32)


def smoothstep(edge0: float, edge1: float, value: np.ndarray) -> np.ndarray:
	t = np.clip((value - edge0) / max(1e-6, edge1 - edge0), 0.0, 1.0)
	return t * t * (3.0 - 2.0 * t)


def lane_band(distance_from_inner: np.ndarray) -> np.ndarray:
	"""Kenney-like longitudinal / concentric asphalt banding."""
	width = float(OUTER - INNER)
	u = np.clip(distance_from_inner / width, 0.0, 1.0)
	# Soft bands across the lane.
	wave = 0.5 + 0.5 * np.sin(u * np.pi * 5.0)
	center = 1.0 - np.abs(u - 0.5) * 2.0
	color = (
		ASPHALT * (0.55 + 0.25 * (1.0 - wave))[..., None]
		+ ASPHALT_MID * (0.25 * wave)[..., None]
		+ ASPHALT_LIGHT * (0.10 * center)[..., None]
	)
	# Edge darkening near inner / outer bounds.
	edge = smoothstep(0.0, 0.08, u) * smoothstep(0.0, 0.08, 1.0 - u)
	color = EDGE_SHADOW * (1.0 - edge)[..., None] + color * edge[..., None]
	# Thin highlight just inside each edge.
	inner_hi = smoothstep(0.02, 0.05, u) * (1.0 - smoothstep(0.05, 0.09, u))
	outer_hi = smoothstep(0.02, 0.05, 1.0 - u) * (1.0 - smoothstep(0.05, 0.09, 1.0 - u))
	hi = np.maximum(inner_hi, outer_hi)[..., None]
	color = color * (1.0 - 0.55 * hi) + EDGE_HIGHLIGHT * (0.55 * hi)
	return color


def paint_straight_vertical_band(img: np.ndarray) -> None:
	"""Asphalt strip with x in [INNER, OUTER) — track1 / TILE_STRAIGHT_HORIZONTAL."""
	xs = np.arange(TILE, dtype=np.float32)[None, :]
	x = np.broadcast_to(xs, (TILE, TILE))
	dist = x - float(INNER)
	aa = smoothstep(INNER - 1.2, INNER + 0.2, x) * (1.0 - smoothstep(OUTER - 0.2, OUTER + 1.2, x))
	color = lane_band(np.clip(dist, 0.0, OUTER - INNER))
	for c in range(3):
		img[..., c] = np.where(aa > 0, color[..., c] * aa + img[..., c] * (1.0 - aa), img[..., c])
	img[..., 3] = np.maximum(img[..., 3], aa * 255.0)


def paint_straight_horizontal_band(img: np.ndarray) -> None:
	"""Asphalt strip with y in [INNER, OUTER) — track2 / TILE_STRAIGHT_VERTICAL."""
	ys = np.arange(TILE, dtype=np.float32)[:, None]
	y = np.broadcast_to(ys, (TILE, TILE))
	dist = y - float(INNER)
	aa = smoothstep(INNER - 1.2, INNER + 0.2, y) * (1.0 - smoothstep(OUTER - 0.2, OUTER + 1.2, y))
	color = lane_band(np.clip(dist, 0.0, OUTER - INNER))
	for c in range(3):
		img[..., c] = np.where(aa > 0, color[..., c] * aa + img[..., c] * (1.0 - aa), img[..., c])
	img[..., 3] = np.maximum(img[..., 3], aa * 255.0)


def paint_corner(img: np.ndarray, center_x: float, center_y: float) -> None:
	ys = np.arange(TILE, dtype=np.float32)[:, None]
	xs = np.arange(TILE, dtype=np.float32)[None, :]
	dx = xs - center_x
	dy = ys - center_y
	radius = np.sqrt(dx * dx + dy * dy)
	dist = radius - float(INNER)
	aa = smoothstep(INNER - 1.2, INNER + 0.2, radius) * (
		1.0 - smoothstep(OUTER - 0.2, OUTER + 1.2, radius)
	)
	color = lane_band(np.clip(dist, 0.0, OUTER - INNER))

	# Soft Kenney-inspired curb on the outer shoulder (replaces loud arcade hatch).
	outer_shoulder = smoothstep(OUTER - 14.0, OUTER - 10.0, radius) * (
		1.0 - smoothstep(OUTER - 1.5, OUTER + 1.0, radius)
	)
	angle = np.arctan2(dy, dx)
	stripe = 0.5 + 0.5 * np.sin(angle * 18.0)
	curb = CURB_A * stripe[..., None] + CURB_B * (1.0 - stripe)[..., None]
	color = color * (1.0 - 0.85 * outer_shoulder[..., None]) + curb * (0.85 * outer_shoulder[..., None])

	for c in range(3):
		img[..., c] = np.where(aa > 0, color[..., c] * aa + img[..., c] * (1.0 - aa), img[..., c])
	img[..., 3] = np.maximum(img[..., 3], aa * 255.0)


def save(img: np.ndarray, name: str) -> None:
	out = OUT_DIR / name
	Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(out, optimize=True)
	print(f"Wrote {out}")


def main() -> None:
	OUT_DIR.mkdir(parents=True, exist_ok=True)

	t1 = blank()
	paint_straight_vertical_band(t1)
	save(t1, "track1.png")

	t2 = blank()
	paint_straight_horizontal_band(t2)
	save(t2, "track2.png")

	# Corner centers match Circuit.enforceTrackBoundaries.
	corners = {
		3: (0.0, float(TILE)),          # BOTTOM_RIGHT
		4: (float(TILE), float(TILE)),  # TOP_RIGHT
		5: (float(TILE), 0.0),          # TOP_LEFT
		6: (0.0, 0.0),                  # BOTTOM_LEFT
	}
	for index, center in corners.items():
		tile = blank()
		paint_corner(tile, center[0], center[1])
		save(tile, f"track{index}.png")

	# Open / grass cell — fully transparent (scenery shows through).
	save(blank(), "track7.png")


if __name__ == "__main__":
	main()
