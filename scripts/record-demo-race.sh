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
		echo "2560 1600"
	fi
}

read -r SCREEN_WIDTH SCREEN_HEIGHT < <(screen_size)
SCREEN_WIDTH=$(( SCREEN_WIDTH - SCREEN_WIDTH % 2 ))
SCREEN_HEIGHT=$(( SCREEN_HEIGHT - SCREEN_HEIGHT % 2 ))
if (( SCREEN_WIDTH < 2 || SCREEN_HEIGHT < 2 )); then
	echo "Invalid screen size ${SCREEN_WIDTH}x${SCREEN_HEIGHT}" >&2
	exit 1
fi

# Force 1x UI scale so AppShell stays near the race canvas size on Xvfb.
echo "Starting demo race on ${DISPLAY_NUM} (track=${TRACK}, cars=${CARS}, laps=${LAPS}, screen=${SCREEN_WIDTH}x${SCREEN_HEIGHT})..."
GDK_SCALE=1 QT_SCALE_FACTOR=1 \
	java -Dsun.java2d.uiScale=1 -Dsun.java2d.uiScale.enabled=true \
	-cp "${JAR_FILE}" view.DemoRaceCapture "$TRACK" "$CARS" "$LAPS" &
GAME_PID=$!

# Wait until the JVM is alive and, if xdotool is present, until a Super Sprint
# window exists. Recording captures the full framebuffer so oversized / offscreen
# shells cannot break x11grab the way window-rect grabs can.
READY=0
for _ in $(seq 1 150); do
	if ! kill -0 "${GAME_PID}" 2>/dev/null; then
		echo "Demo race process exited before recording could start" >&2
		wait "${GAME_PID}" || true
		exit 1
	fi
	if command -v xdotool >/dev/null 2>&1; then
		if xdotool search --name 'Super Sprint' >/dev/null 2>&1; then
			WINDOW_ID="$(xdotool search --name 'Super Sprint' 2>/dev/null | head -n 1 || true)"
			if [[ -n "${WINDOW_ID}" ]]; then
				xdotool windowmap "${WINDOW_ID}" >/dev/null 2>&1 || true
				xdotool windowactivate --sync "${WINDOW_ID}" >/dev/null 2>&1 || true
				xdotool windowmove "${WINDOW_ID}" 0 0 >/dev/null 2>&1 || true
			fi
			READY=1
			break
		fi
	else
		# No xdotool: give the Swing shell a moment to map, then grab the screen.
		sleep 1.5
		READY=1
		break
	fi
	sleep 0.2
done
if (( READY != 1 )); then
	echo "Timed out waiting for the demo window; recording the full framebuffer anyway" >&2
fi

echo "Recording full framebuffer ${SCREEN_WIDTH}x${SCREEN_HEIGHT} on ${DISPLAY_NUM} -> ${OUTPUT}"
ffmpeg -y -hide_banner -loglevel error \
	-f x11grab -video_size "${SCREEN_WIDTH}x${SCREEN_HEIGHT}" -framerate "$FPS" \
	-draw_mouse 0 \
	-i "${DISPLAY_NUM}.0+0,0" \
	-c:v libx264 -pix_fmt yuv420p -preset veryfast -crf 18 \
	"$RAW_OUTPUT" &
FFMPEG_PID=$!

# ffmpeg may exit immediately if the display grab fails - surface that early.
sleep 0.5
if ! kill -0 "${FFMPEG_PID}" 2>/dev/null; then
	wait "${FFMPEG_PID}" || true
	echo "ffmpeg failed to start x11grab on ${DISPLAY_NUM} (${SCREEN_WIDTH}x${SCREEN_HEIGHT})" >&2
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

echo "Wrote ${OUTPUT}"
ls -lh "$OUTPUT"
