#!/usr/bin/env bash
set -euo pipefail

PKG="com.vjaykrsna.nanoai"
DEFAULT_OUT_DIR="$(pwd)/specs/002-disclaimer-and-fixes/qa/screenshots"
OUT_DIR="${1:-$DEFAULT_OUT_DIR}"

mkdir -p "$OUT_DIR"

echo "Launching $PKG main activity to prepare for screenshots..."
adb shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1 || true

echo "Navigate within the emulator/device to each requested screen. Press Enter when ready to capture."

capture_screen() {
    local screen_name="$1"
    local file_name="$2"
    read -r -p "Ready to capture $screen_name? Press Enter to continue..." _
    local destination="$OUT_DIR/$file_name"
    adb exec-out screencap -p > "$destination"
    echo "Saved screenshot to $destination"
}

capture_screen "Chat screen" "chat.png"
capture_screen "Settings screen" "settings.png"
capture_screen "Model library screen" "model-library.png"

echo "All screenshots captured in $OUT_DIR"
