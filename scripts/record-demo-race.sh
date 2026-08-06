#!/usr/bin/env bash
# Record an all-AI Dune Horseshoe exhibition race to MP4 for release assets.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="${ROOT}/build"
OUTPUT="${1:-${ROOT}/artifacts/demo/dune-horseshoe-ai-demo.mp4}"
LAPS="${2:-3}"
DISPLAY_NUM="${DISPLAY:-:1}"
FPS="${DEMO_FPS:-30}"

mkdir -p "$(dirname "$OUTPUT")"
cd "$ROOT"
make compile

if ! command -v ffmpeg >/dev/null 2>&1; then
	echo "ffmpeg is required to record the demo" >&2
	exit 1
fi
if ! command -v xdotool >/dev/null 2>&1; then
	echo "xdotool is required to locate the game window" >&2
	exit 1
fi

RAW_OUTPUT="$(mktemp --suffix=.mp4)"
cleanup() {
	rm -f "$RAW_OUTPUT"
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

echo "Starting demo race on ${DISPLAY_NUM}..."
java -cp "$BUILD_DIR" view.DemoRaceCapture "$LAPS" &
GAME_PID=$!

WINDOW_ID=""
for _ in $(seq 1 50); do
	WINDOW_ID="$(xdotool search --name 'Super Sprint' 2>/dev/null | head -n 1 || true)"
	if [[ -n "$WINDOW_ID" ]]; then
		break
	fi
	sleep 0.2
done
if [[ -z "$WINDOW_ID" ]]; then
	echo "Could not find Super Sprint window" >&2
	exit 1
fi

xdotool windowactivate --sync "$WINDOW_ID" >/dev/null 2>&1 || true
sleep 0.4

eval "$(xdotool getwindowgeometry --shell "$WINDOW_ID")"
# Keep even dimensions for yuv420p
WIDTH=$(( WIDTH - WIDTH % 2 ))
HEIGHT=$(( HEIGHT - HEIGHT % 2 ))
if (( WIDTH < 2 || HEIGHT < 2 )); then
	echo "Invalid window size ${WIDTH}x${HEIGHT}" >&2
	exit 1
fi

echo "Recording window ${WINDOW_ID} at ${WIDTH}x${HEIGHT}+${X},${Y} -> ${OUTPUT}"
ffmpeg -y -hide_banner -loglevel error \
	-f x11grab -video_size "${WIDTH}x${HEIGHT}" -framerate "$FPS" \
	-draw_mouse 0 \
	-i "${DISPLAY_NUM}.0+${X},${Y}" \
	-c:v libx264 -pix_fmt yuv420p -preset veryfast -crf 18 \
	"$RAW_OUTPUT" &
FFMPEG_PID=$!

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

echo "Wrote ${OUTPUT}"
ls -lh "$OUTPUT"
