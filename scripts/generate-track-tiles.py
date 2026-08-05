#!/usr/bin/env python3
"""Generate wide Super Sprint track tiles in a Kenney-inspired style.

Lane geometry matches Circuit.INNER_RADIUS / OUTER_RADIUS / TILE_SIZE so
physics stays valid. Visual language borrows cool asphalt banding from
Kenney's Top-down Tanks Redux roads (CC0 inspiration only — tiles are
original, not cropped from that pack).

Corner kerbs use crisp alternating red/white blocks (classic Super Sprint
rumble strips) rather than soft sine blends. Straights get thin white
edge lines so the ribbon reads as a continuous painted track.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

TILE = 219
INNER = 26
OUTER = 191

# Cool Kenney-like asphalt palette (teal-gray), slightly deepened so white
# paint / checkers stay readable on the ribbon.
ASPHALT = np.array([132, 154, 160], dtype=np.float32)
ASPHALT_MID = np.array([118, 140, 146], dtype=np.float32)
ASPHALT_LIGHT = np.array([158, 178, 184], dtype=np.float32)
EDGE_SHADOW = np.array([72, 90, 96], dtype=np.float32)
EDGE_HIGHLIGHT = np.array([188, 206, 210], dtype=np.float32)
# Classic Super Sprint kerb colours — solid blocks, not pastel blends.
CURB_RED = np.array([196, 48, 52], dtype=np.float32)
CURB_WHITE = np.array([236, 236, 236], dtype=np.float32)
EDGE_LINE = np.array([232, 236, 238], dtype=np.float32)

# Outer shoulder kerb width (px) and approximate stripe length along the arc.
CURB_WIDTH = 11.0
# Target stripe length in pixels along the outer radius (~classic arcade look).
STRIPE_ARC_PX = 14.0
EDGE_LINE_WIDTH = 2.2
EDGE_LINE_INSET = 3.0

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
	wave = 0.5 + 0.5 * np.sin(u * np.pi * 5.0)
	center = 1.0 - np.abs(u - 0.5) * 2.0
	color = (
		ASPHALT * (0.55 + 0.25 * (1.0 - wave))[..., None]
		+ ASPHALT_MID * (0.25 * wave)[..., None]
		+ ASPHALT_LIGHT * (0.10 * center)[..., None]
	)
	edge = smoothstep(0.0, 0.08, u) * smoothstep(0.0, 0.08, 1.0 - u)
	color = EDGE_SHADOW * (1.0 - edge)[..., None] + color * edge[..., None]
	inner_hi = smoothstep(0.02, 0.05, u) * (1.0 - smoothstep(0.05, 0.09, u))
	outer_hi = smoothstep(0.02, 0.05, 1.0 - u) * (1.0 - smoothstep(0.05, 0.09, 1.0 - u))
	hi = np.maximum(inner_hi, outer_hi)[..., None]
	color = color * (1.0 - 0.45 * hi) + EDGE_HIGHLIGHT * (0.45 * hi)
	return color


def paint_edge_lines_linear(
	color: np.ndarray,
	coord: np.ndarray,
	inner: float,
	outer: float,
) -> np.ndarray:
	"""Thin white paint lines just inside each lane edge (straights)."""
	inner_line = smoothstep(inner + EDGE_LINE_INSET - 0.6, inner + EDGE_LINE_INSET, coord) * (
		1.0 - smoothstep(inner + EDGE_LINE_INSET + EDGE_LINE_WIDTH - 0.4,
						 inner + EDGE_LINE_INSET + EDGE_LINE_WIDTH + 0.6, coord)
	)
	outer_line = smoothstep(outer - EDGE_LINE_INSET - EDGE_LINE_WIDTH - 0.6,
							outer - EDGE_LINE_INSET - EDGE_LINE_WIDTH + 0.4, coord) * (
		1.0 - smoothstep(outer - EDGE_LINE_INSET, outer - EDGE_LINE_INSET + 0.6, coord)
	)
	line = np.maximum(inner_line, outer_line)[..., None]
	return color * (1.0 - 0.92 * line) + EDGE_LINE * (0.92 * line)


def paint_edge_lines_radial(color: np.ndarray, radius: np.ndarray) -> np.ndarray:
	"""Thin white paint on the inner edge; outer edge is the kerb itself."""
	inner_line = smoothstep(INNER + EDGE_LINE_INSET - 0.6, INNER + EDGE_LINE_INSET, radius) * (
		1.0 - smoothstep(INNER + EDGE_LINE_INSET + EDGE_LINE_WIDTH - 0.4,
						 INNER + EDGE_LINE_INSET + EDGE_LINE_WIDTH + 0.6, radius)
	)
	line = inner_line[..., None]
	return color * (1.0 - 0.88 * line) + EDGE_LINE * (0.88 * line)


def paint_crisp_curb(color: np.ndarray, radius: np.ndarray, angle: np.ndarray) -> np.ndarray:
	"""Classic alternating red/white rumble blocks on the outer shoulder."""
	outer_shoulder = smoothstep(OUTER - CURB_WIDTH - 0.8, OUTER - CURB_WIDTH + 0.6, radius) * (
		1.0 - smoothstep(OUTER - 0.6, OUTER + 1.0, radius)
	)
	# Arc-length stripes so blocks stay even width around the quarter-circle.
	arc = np.abs(angle) * float(OUTER)
	stripe = np.floor(arc / STRIPE_ARC_PX).astype(np.int32)
	is_red = (stripe % 2) == 0
	curb = np.where(is_red[..., None], CURB_RED, CURB_WHITE)
	# Soft seam between blocks (sub-pixel) without washing colours together.
	frac = (arc / STRIPE_ARC_PX) - stripe.astype(np.float32)
	seam = np.minimum(smoothstep(0.0, 0.08, frac), smoothstep(0.0, 0.08, 1.0 - frac))
	curb = curb * (1.0 - 0.18 * seam[..., None]) + CURB_WHITE * (0.09 * seam[..., None])
	return color * (1.0 - outer_shoulder[..., None]) + curb * outer_shoulder[..., None]


def paint_straight_vertical_band(img: np.ndarray) -> None:
	"""Asphalt strip with x in [INNER, OUTER) — track_00 / TILE_STRAIGHT_HORIZONTAL."""
	xs = np.arange(TILE, dtype=np.float32)[None, :]
	x = np.broadcast_to(xs, (TILE, TILE))
	dist = x - float(INNER)
	aa = smoothstep(INNER - 1.2, INNER + 0.2, x) * (1.0 - smoothstep(OUTER - 0.2, OUTER + 1.2, x))
	color = lane_band(np.clip(dist, 0.0, OUTER - INNER))
	color = paint_edge_lines_linear(color, x, float(INNER), float(OUTER))
	for c in range(3):
		img[..., c] = np.where(aa > 0, color[..., c] * aa + img[..., c] * (1.0 - aa), img[..., c])
	img[..., 3] = np.maximum(img[..., 3], aa * 255.0)


def paint_straight_horizontal_band(img: np.ndarray) -> None:
	"""Asphalt strip with y in [INNER, OUTER) — track_01 / TILE_STRAIGHT_VERTICAL."""
	ys = np.arange(TILE, dtype=np.float32)[:, None]
	y = np.broadcast_to(ys, (TILE, TILE))
	dist = y - float(INNER)
	aa = smoothstep(INNER - 1.2, INNER + 0.2, y) * (1.0 - smoothstep(OUTER - 0.2, OUTER + 1.2, y))
	color = lane_band(np.clip(dist, 0.0, OUTER - INNER))
	color = paint_edge_lines_linear(color, y, float(INNER), float(OUTER))
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
	angle = np.arctan2(dy, dx)
	color = paint_crisp_curb(color, radius, angle)
	color = paint_edge_lines_radial(color, radius)

	for c in range(3):
		img[..., c] = np.where(aa > 0, color[..., c] * aa + img[..., c] * (1.0 - aa), img[..., c])
	img[..., 3] = np.maximum(img[..., 3], aa * 255.0)


def save(img: np.ndarray, name: str) -> None:
	out = OUT_DIR / name
	Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(out, optimize=True)
	print(f"Wrote {out}")


def tile_name(zero_based_index: int) -> str:
	return f"track_{zero_based_index:02d}.png"


def main() -> None:
	OUT_DIR.mkdir(parents=True, exist_ok=True)

	# Zero-based tile ids match Circuit.TILE_* and ResourcePaths.trackTileFileName.
	t0 = blank()
	paint_straight_vertical_band(t0)
	save(t0, tile_name(0))

	t1 = blank()
	paint_straight_horizontal_band(t1)
	save(t1, tile_name(1))

	# Corner centers match Circuit.enforceTrackBoundaries.
	corners = {
		2: (0.0, float(TILE)),          # BOTTOM_RIGHT
		3: (float(TILE), float(TILE)),  # TOP_RIGHT
		4: (float(TILE), 0.0),          # TOP_LEFT
		5: (0.0, 0.0),                  # BOTTOM_LEFT
	}
	for index, center in corners.items():
		tile = blank()
		paint_corner(tile, center[0], center[1])
		save(tile, tile_name(index))

	# Open / grass cell — fully transparent (scenery shows through).
	save(blank(), tile_name(6))


if __name__ == "__main__":
	main()
