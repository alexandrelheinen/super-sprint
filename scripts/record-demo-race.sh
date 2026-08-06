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

echo "Starting demo race on ${DISPLAY_NUM} (track=${TRACK}, cars=${CARS}, laps=${LAPS})..."
java -cp "${JAR_FILE}" view.DemoRaceCapture "$TRACK" "$CARS" "$LAPS" &
GAME_PID=$!

# Wait until a Super Sprint window owned by this Java process is mapped with a
# real non-zero size. Prefer --pid so a leftover title match from a previous
# demo in the same Xvfb session cannot steal the grab. Finding the window too
# early yields WIDTH=0 HEIGHT=0 on Xvfb/CI.
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
	candidates="$(xdotool search --pid "${GAME_PID}" --class 'sun-awt-X11' 2>/dev/null || true)"
	if [[ -z "${candidates}" ]]; then
		candidates="$(xdotool search --pid "${GAME_PID}" --name 'Super Sprint' 2>/dev/null || true)"
	fi
	if [[ -z "${candidates}" ]]; then
		candidates="$(xdotool search --name 'Super Sprint' 2>/dev/null || true)"
	fi
	while read -r candidate; do
		[[ -z "${candidate}" ]] && continue
		WIDTH=0
		HEIGHT=0
		X=0
		Y=0
		eval "$(xdotool getwindowgeometry --shell "${candidate}" 2>/dev/null || true)"
		if (( WIDTH >= 2 && HEIGHT >= 2 )); then
			WINDOW_ID="${candidate}"
			break 2
		fi
	done <<< "${candidates}"
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
sleep 0.4
WIDTH=0
HEIGHT=0
X=0
Y=0
eval "$(xdotool getwindowgeometry --shell "${WINDOW_ID}")"

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
