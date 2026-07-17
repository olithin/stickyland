#!/usr/bin/env bash
# Build a macOS DMG installer for Stickyland.
# Must be run on a Mac (or via GitHub Actions macos runner).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "==> Checking JDK 25..."
if ! /usr/libexec/java_home -v 25 >/dev/null 2>&1; then
  echo "JDK 25 not found. Install with:"
  echo "  brew install --cask temurin@25"
  exit 1
fi

JAVA_HOME_25="$(/usr/libexec/java_home -v 25)"
export JAVA_HOME="$JAVA_HOME_25"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME"

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
