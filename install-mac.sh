#!/usr/bin/env bash
# Build a macOS DMG installer for Stickyland.
# Must be run on a Mac (or via GitHub Actions macos runner).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "==> Checking JDK 21..."
if ! /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
  echo "JDK 21 not found. Install with:"
  echo "  brew install --cask temurin@21"
  exit 1
fi

JAVA_HOME_21="$(/usr/libexec/java_home -v 21)"
export JAVA_HOME="$JAVA_HOME_21"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME"
echo "Build machine arch: $(uname -m) (this DMG will only run on the same CPU architecture)"

if grep -qE '^org\.gradle\.java\.home=.*(C:|\\\\)' gradle.properties 2>/dev/null; then
  echo "==> Commenting out Windows org.gradle.java.home in gradle.properties"
  sed -i.bak 's/^org.gradle.java.home=/# org.gradle.java.home=/' gradle.properties
fi

chmod +x gradlew

echo "==> Building DMG installer..."
./gradlew clean packageDmg --no-daemon

DMG_DIR="$ROOT/build/compose/binaries/main/dmg"
DMG="$(find "$DMG_DIR" -name '*.dmg' | head -n 1 || true)"

if [[ -z "$DMG" ]]; then
  echo "DMG not found under $DMG_DIR"
  ls -la "$DMG_DIR" || true
  exit 1
fi

echo ""
echo "Done!"
echo "Installer: $DMG"
echo ""
echo "Open it:"
echo "  open \"$DMG\""
echo "Then drag Stickyland into Applications."
echo ""
if [[ "$(uname -m)" == "arm64" ]]; then
  echo "This local DMG is Apple Silicon only. Intel Macs need *-mac-x64.dmg from GitHub Releases."
else
  echo "This local DMG is Intel only. Apple Silicon Macs should use *-mac-arm64.dmg from GitHub Releases."
fi
