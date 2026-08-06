#!/usr/bin/env bash
# Record an all-AI exhibition race to MP4 for release assets.
# Builds the runnable jar via Gradle, then launches view.DemoRaceCapture.
# Usage: record-demo-race.sh [output.mp4] <trackId> <carIds> [laps]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ "${1:-}" == *.mp4 || "${1:-}" == */* ]]; then
	OUTPUT="$1"
	TRACK="${2:?trackId required}"
	CARS="${3:?carIds required}"
	LAPS="${4:-3}"
else
	TRACK="${1:?trackId required}"
	CARS="${2:?carIds required}"
	LAPS="${3:-3}"
	OUTPUT="${ROOT}/artifacts/demo/ai-demo-track${TRACK}.mp4"
fi

DISPLAY_NUM="${DISPLAY:-:1}"
FPS="${DEMO_FPS:-30}"
# Reject near-empty captures (e.g. unmapped Swing window on a black Xvfb root).
MIN_CONTENT_RATIO="${DEMO_MIN_CONTENT_RATIO:-0.15}"

mkdir -p "$(dirname "$OUTPUT")"
cd "$ROOT"

echo "Resolving game jar..."
if [[ -n "${RECORD_JAR:-}" && -f "${RECORD_JAR}" ]]; then
	JAR_FILE="${RECORD_JAR}"
	echo "Using prebuilt jar from Gradle: ${JAR_FILE}"
else
	echo "Building game jar with Gradle..."
	./gradlew --quiet jar
	JAR_FILE="$(ls -1 build/libs/super-sprint-supelec-*.jar 2>/dev/null | head -n 1 || true)"
fi
if [[ -z "${JAR_FILE}" || ! -f "${JAR_FILE}" ]]; then
	echo "Gradle jar not found under build/libs (RECORD_JAR=${RECORD_JAR:-unset})" >&2
	exit 1
fi

if ! command -v ffmpeg >/dev/null 2>&1; then
	echo "ffmpeg is required to record the demo" >&2
	exit 1
fi
if ! command -v xdotool >/dev/null 2>&1; then
	echo "xdotool is required to locate the game window" >&2
	exit 1
fi

RAW_OUTPUT="$(mktemp --suffix=.mp4)"
SAMPLE_FRAME="$(mktemp --suffix=.png)"
cleanup() {
	rm -f "$RAW_OUTPUT" "$SAMPLE_FRAME"
	if [[ -n "${GAME_PID:-}" ]] && kill -0 "$GAME_PID" 2>/dev/null; then
		kill "$GAME_PID" 2>/dev/null || true
		wait "$GAME_PID" 2>/dev/null || true
	fi
	if [[ -n "${FFMPEG_PID:-}" ]] && kill -0 "$FFMPEG_PID" 2>/dev/null; then
		kill -INT "$FFMPEG_PID" 2>/dev/null || true
		wait "$FFMPEG_PID" 2>/dev/null || true
	fi
}
trap cleanup EXIT

# Resolve framebuffer size without tripping set -e / pipefail when xdpyinfo is
# missing or the display is not ready yet.
screen_size() {
	local dims=""
	if command -v xdpyinfo >/dev/null 2>&1; then
		dims="$( { xdpyinfo 2>/dev/null || true; } | awk '/dimensions:/ {print $2; exit}' || true)"
	fi
	if [[ "${dims}" =~ ^([0-9]+)x([0-9]+)$ ]]; then
		echo "${BASH_REMATCH[1]} ${BASH_REMATCH[2]}"
	else
		echo "1280 1024"
	fi
}

read_geometry() {
	# Sets GEOM_W GEOM_H GEOM_X GEOM_Y for the given window id.
	local window_id="$1"
	local geom
	GEOM_W=0
	GEOM_H=0
	GEOM_X=0
	GEOM_Y=0
	geom="$(xdotool getwindowgeometry --shell "${window_id}" 2>/dev/null || true)"
	[[ -z "${geom}" ]] && return 1
	GEOM_W="$(printf '%s\n' "${geom}" | awk -F= '/^WIDTH=/ {print $2; exit}')"
	GEOM_H="$(printf '%s\n' "${geom}" | awk -F= '/^HEIGHT=/ {print $2; exit}')"
	GEOM_X="$(printf '%s\n' "${geom}" | awk -F= '/^X=/ {print $2; exit}')"
	GEOM_Y="$(printf '%s\n' "${geom}" | awk -F= '/^Y=/ {print $2; exit}')"
	GEOM_W="${GEOM_W:-0}"
	GEOM_H="${GEOM_H:-0}"
	GEOM_X="${GEOM_X:-0}"
	GEOM_Y="${GEOM_Y:-0}"
}

assert_video_has_content() {
	local video="$1"
	local sample_t="${2:-8}"
	ffmpeg -y -hide_banner -loglevel error -ss "${sample_t}" -i "${video}" -frames:v 1 "${SAMPLE_FRAME}"
	python3 - "$SAMPLE_FRAME" "$MIN_CONTENT_RATIO" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
min_ratio = float(sys.argv[2])
data = path.read_bytes()
# Prefer Pillow when present; otherwise decode via a tiny RGB sampling with ffmpeg raw.
try:
	from PIL import Image
	image = Image.open(path).convert("RGB")
	width, height = image.size
	pixels = image.getdata()
	non_dark = sum(1 for r, g, b in pixels if (r + g + b) > 45)
	total = width * height
except Exception:
	import subprocess
	meta = subprocess.check_output(
		[
			"ffprobe", "-v", "error", "-select_streams", "v:0",
			"-show_entries", "stream=width,height", "-of", "csv=p=0", str(path),
		],
		text=True,
	).strip()
	width, height = map(int, meta.split(","))
	raw = subprocess.check_output(
		[
			"ffmpeg", "-hide_banner", "-loglevel", "error",
			"-i", str(path), "-f", "rawvideo", "-pix_fmt", "rgb24", "-",
		]
	)
	non_dark = 0
	step = 3 * 8
	for i in range(0, len(raw) - 2, step):
		if raw[i] + raw[i + 1] + raw[i + 2] > 45:
			non_dark += 1
	total = max(1, (len(raw) // step))
ratio = non_dark / total
print(f"content_ratio={ratio:.4f} min={min_ratio:.4f} size={width}x{height}")
if ratio < min_ratio:
	raise SystemExit(
		f"Demo capture looks empty/black (content_ratio={ratio:.4f} < {min_ratio:.4f}). "
		"The Swing window was probably not mapped into the grab rectangle."
	)
PY
}

read -r SCREEN_WIDTH SCREEN_HEIGHT < <(screen_size)

# Force 1x UI scale so AppShell stays close to the race canvas size on Xvfb.
echo "Starting demo race on ${DISPLAY_NUM} (track=${TRACK}, cars=${CARS}, laps=${LAPS}, screen=${SCREEN_WIDTH}x${SCREEN_HEIGHT})..."
GDK_SCALE=1 QT_SCALE_FACTOR=1 \
	java -Dsun.java2d.uiScale=1 -Dsun.java2d.uiScale.enabled=true \
	-Dawt.useSystemAAFontSettings=on \
	-cp "${JAR_FILE}" view.DemoRaceCapture "$TRACK" "$CARS" "$LAPS" &
GAME_PID=$!

# Wait until a Super Sprint window is mapped with a real non-zero size.
# Finding the window too early yields WIDTH=0 HEIGHT=0 on Xvfb/CI.
# Prefer the largest candidate so we do not grab an openbox stub / icon window.
WINDOW_ID=""
WIDTH=0
HEIGHT=0
X=0
Y=0
for _ in $(seq 1 250); do
	if ! kill -0 "${GAME_PID}" 2>/dev/null; then
		echo "Demo race process exited before a recordable window appeared" >&2
		wait "${GAME_PID}" || true
		exit 1
	fi

	# Title match first (client window). Fall back to any mapped window for the
	# JVM pid - openbox reparenting can hide the title on the frame.
	candidates="$(xdotool search --name 'Super Sprint' 2>/dev/null || true)"
	if [[ -z "${candidates}" ]]; then
		candidates="$(xdotool search --pid "${GAME_PID}" 2>/dev/null || true)"
	fi

	BEST_AREA=0
	WINDOW_ID=""
	while read -r candidate; do
		[[ -z "${candidate}" ]] && continue
		if ! read_geometry "${candidate}"; then
			continue
		fi
		if (( GEOM_W < 64 || GEOM_H < 64 )); then
			continue
		fi
		area=$(( GEOM_W * GEOM_H ))
		if (( area > BEST_AREA )); then
			BEST_AREA=${area}
			WINDOW_ID="${candidate}"
			WIDTH=${GEOM_W}
			HEIGHT=${GEOM_H}
			X=${GEOM_X}
			Y=${GEOM_Y}
		fi
	done <<< "${candidates}"

	if [[ -n "${WINDOW_ID}" ]]; then
		break
	fi
	sleep 0.2
done
if [[ -z "${WINDOW_ID}" ]]; then
	echo "Could not find a mapped Super Sprint window with non-zero size" >&2
	echo "xdotool search --name:" >&2
	xdotool search --name 'Super Sprint' 2>&1 || true
	echo "xdotool search --pid ${GAME_PID}:" >&2
	xdotool search --pid "${GAME_PID}" 2>&1 || true
	exit 1
fi

xdotool windowmap "${WINDOW_ID}" >/dev/null 2>&1 || true
xdotool windowactivate --sync "${WINDOW_ID}" >/dev/null 2>&1 || true
xdotool windowmove "${WINDOW_ID}" 0 0 >/dev/null 2>&1 || true

# If Java/openbox still produced a shell larger than the framebuffer, shrink it
# so x11grab has a legal rectangle. Swing will letterbox the race canvas.
MAX_W=$(( SCREEN_WIDTH - SCREEN_WIDTH % 2 ))
MAX_H=$(( SCREEN_HEIGHT - SCREEN_HEIGHT % 2 ))
if (( WIDTH > MAX_W || HEIGHT > MAX_H )); then
	TARGET_W=${WIDTH}
	TARGET_H=${HEIGHT}
	if (( TARGET_W > MAX_W )); then TARGET_W=${MAX_W}; fi
	if (( TARGET_H > MAX_H )); then TARGET_H=${MAX_H}; fi
	echo "Resizing oversized window ${WIDTH}x${HEIGHT} -> ${TARGET_W}x${TARGET_H} to fit screen"
	xdotool windowsize "${WINDOW_ID}" "${TARGET_W}" "${TARGET_H}" >/dev/null 2>&1 || true
	sleep 0.2
fi

sleep 0.3
read_geometry "${WINDOW_ID}" || true
WIDTH=${GEOM_W}
HEIGHT=${GEOM_H}
X=${GEOM_X}
Y=${GEOM_Y}

# Clamp the grab rectangle into the framebuffer (x11grab rejects overflow).
if (( X < 0 )); then X=0; fi
if (( Y < 0 )); then Y=0; fi
if (( X >= SCREEN_WIDTH )); then X=0; fi
if (( Y >= SCREEN_HEIGHT )); then Y=0; fi
if (( X + WIDTH > SCREEN_WIDTH )); then WIDTH=$(( SCREEN_WIDTH - X )); fi
if (( Y + HEIGHT > SCREEN_HEIGHT )); then HEIGHT=$(( SCREEN_HEIGHT - Y )); fi

# Keep even dimensions for yuv420p
WIDTH=$(( WIDTH - WIDTH % 2 ))
HEIGHT=$(( HEIGHT - HEIGHT % 2 ))
if (( WIDTH < 64 || HEIGHT < 64 )); then
	echo "Invalid capture size ${WIDTH}x${HEIGHT} on screen ${SCREEN_WIDTH}x${SCREEN_HEIGHT}" >&2
	exit 1
fi

echo "Recording window ${WINDOW_ID} at ${WIDTH}x${HEIGHT}+${X},${Y} (screen ${SCREEN_WIDTH}x${SCREEN_HEIGHT}) -> ${OUTPUT}"
ffmpeg -y -hide_banner -loglevel error \
	-f x11grab -video_size "${WIDTH}x${HEIGHT}" -framerate "$FPS" \
	-draw_mouse 0 \
	-i "${DISPLAY_NUM}.0+${X},${Y}" \
	-c:v libx264 -pix_fmt yuv420p -preset veryfast -crf 18 \
	"$RAW_OUTPUT" &
FFMPEG_PID=$!

# ffmpeg may exit immediately if the display grab fails - surface that early.
sleep 0.5
if ! kill -0 "${FFMPEG_PID}" 2>/dev/null; then
	wait "${FFMPEG_PID}" || true
	echo "ffmpeg failed to start x11grab ${WIDTH}x${HEIGHT}+${X},${Y} on ${DISPLAY_NUM}" >&2
	exit 1
fi

wait "$GAME_PID"
GAME_STATUS=$?

# Let ffmpeg flush a couple of trailing frames, then stop cleanly.
sleep 0.5
kill -INT "$FFMPEG_PID" 2>/dev/null || true
wait "$FFMPEG_PID" 2>/dev/null || true
FFMPEG_PID=""

if [[ "$GAME_STATUS" -ne 0 ]]; then
	echo "Demo race process exited with status ${GAME_STATUS}" >&2
	exit "$GAME_STATUS"
fi

ffmpeg -y -hide_banner -loglevel error -i "$RAW_OUTPUT" \
	-c:v libx264 -pix_fmt yuv420p -movflags +faststart \
	"$OUTPUT"

assert_video_has_content "$OUTPUT" 8

echo "Wrote ${OUTPUT}"
ls -lh "$OUTPUT"
