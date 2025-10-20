#!/usr/bin/env bash
set -euo pipefail

MODEL=${1:-base.en}
DEST_DIR="$(dirname "$0")/models"
mkdir -p "$DEST_DIR"

BASE_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
FILE="ggml-${MODEL}.bin"
OUT_PATH="$DEST_DIR/$FILE"

if [[ -f "$OUT_PATH" ]]; then
  echo "Model already exists at $OUT_PATH"
  exit 0
fi

echo "Downloading $FILE ..."
 curl -L "$BASE_URL/$FILE" -o "$OUT_PATH"

echo "Saved model to $OUT_PATH"
