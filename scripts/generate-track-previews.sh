#!/usr/bin/env bash
# Generates track preview PNGs from Game.TRACK_MAPS tile data.
set -eu

BUILD_DIR="${1:-build}"
OUT_DIR="${BUILD_DIR}/sprites"

mkdir -p "${OUT_DIR}"
java -cp "${BUILD_DIR}" view.TrackPreviewGenerator "${OUT_DIR}"
touch "${OUT_DIR}/.track-previews-stamp"
