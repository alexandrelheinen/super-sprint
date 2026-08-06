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

# Force 1x UI scale so the shell fits the Xvfb framebuffer. Without this,
# some CI JVMs report a 2x desktop and AppShell becomes larger than the screen,
# which makes x11grab reject the capture rectangle.
echo "Starting demo race on ${DISPLAY_NUM} (track=${TRACK}, cars=${CARS}, laps=${LAPS})..."
GDK_SCALE=1 QT_SCALE_FACTOR=1 \
	java -Dsun.java2d.uiScale=1 -Dsun.java2d.uiScale.enabled=true \
	-cp "${JAR_FILE}" view.DemoRaceCapture "$TRACK" "$CARS" "$LAPS" &
GAME_PID=$!

screen_size() {
	# Outputs: SCREEN_WIDTH SCREEN_HEIGHT
	if command -v xdpyinfo >/dev/null 2>&1; then
		local dims
		dims="$(xdpyinfo 2>/dev/null | awk '/dimensions:/ {print $2; exit}')"
		if [[ "${dims}" =~ ^([0-9]+)x([0-9]+)$ ]]; then
			echo "${BASH_REMATCH[1]} ${BASH_REMATCH[2]}"
			return
		fi
	fi
	echo "1280 1024"
}

# Wait until a Super Sprint window owned by this Java process is mapped with a
# real non-zero size. Prefer title+pid so we grab the client window rather than
# a WM frame / helper AWT peer. Finding the window too early yields WIDTH=0.
WINDOW_ID=""
WIDTH=0
HEIGHT=0
X=0
Y=0
BEST_AREA=0
read -r SCREEN_WIDTH SCREEN_HEIGHT < <(screen_size)
for _ in $(seq 1 250); do
	if ! kill -0 "${GAME_PID}" 2>/dev/null; then
		echo "Demo race process exited before a recordable window appeared" >&2
		wait "${GAME_PID}" || true
		exit 1
	fi
	candidates="$(xdotool search --pid "${GAME_PID}" --name 'Super Sprint' 2>/dev/null || true)"
	if [[ -z "${candidates}" ]]; then
		candidates="$(xdotool search --name 'Super Sprint' 2>/dev/null || true)"
	fi
	BEST_AREA=0
	WINDOW_ID=""
	while read -r candidate; do
		[[ -z "${candidate}" ]] && continue
		local_w=0
		local_h=0
		local_x=0
		local_y=0
		eval "$(xdotool getwindowgeometry --shell "${candidate}" 2>/dev/null | sed 's/^WIDTH=/local_w=/; s/^HEIGHT=/local_h=/; s/^X=/local_x=/; s/^Y=/local_y=/')"
		if (( local_w < 2 || local_h < 2 )); then
			continue
		fi
		# Prefer the largest window that still fits on the framebuffer.
		fits=0
		if (( local_w <= SCREEN_WIDTH && local_h <= SCREEN_HEIGHT )); then
			fits=1
		fi
		area=$(( local_w * local_h ))
		if (( fits == 1 && area > BEST_AREA )); then
			BEST_AREA=${area}
			WINDOW_ID="${candidate}"
			WIDTH=${local_w}
			HEIGHT=${local_h}
			X=${local_x}
			Y=${local_y}
		fi
	done <<< "${candidates}"
	if [[ -n "${WINDOW_ID}" ]]; then
		break
	fi
	sleep 0.2
done
if [[ -z "${WINDOW_ID}" ]]; then
	echo "Could not find a mapped Super Sprint window that fits the screen ${SCREEN_WIDTH}x${SCREEN_HEIGHT}" >&2
	echo "xdotool search --name:" >&2
	xdotool search --name 'Super Sprint' 2>&1 || true
	echo "xdotool search --pid ${GAME_PID}:" >&2
	xdotool search --pid "${GAME_PID}" 2>&1 || true
	exit 1
fi

xdotool windowmap "${WINDOW_ID}" >/dev/null 2>&1 || true
xdotool windowactivate --sync "${WINDOW_ID}" >/dev/null 2>&1 || true
# Keep the window on-screen; openbox can place oversized frames off the origin.
xdotool windowmove "${WINDOW_ID}" 0 0 >/dev/null 2>&1 || true
sleep 0.4
WIDTH=0
HEIGHT=0
X=0
Y=0
eval "$(xdotool getwindowgeometry --shell "${WINDOW_ID}")"

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
if (( WIDTH < 2 || HEIGHT < 2 )); then
	echo "Invalid window size ${WIDTH}x${HEIGHT} on screen ${SCREEN_WIDTH}x${SCREEN_HEIGHT}" >&2
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
