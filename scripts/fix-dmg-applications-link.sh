#!/usr/bin/env bash
# jpackage embeds a Finder alias for Applications that points at the build Mac.
# On other machines drag-and-drop into that icon does nothing. Replace it with a
# portable symlink to /Applications.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <path-to.dmg> [more.dmg...]" >&2
  exit 1
fi

fix_one() {
  local src_dmg="$1"
  if [[ ! -f "$src_dmg" ]]; then
    echo "Not a file: $src_dmg" >&2
    return 1
  fi

  local abs
  abs="$(cd "$(dirname "$src_dmg")" && pwd)/$(basename "$src_dmg")"
  local work
  work="$(mktemp -d "${TMPDIR:-/tmp}/stickyland-dmg.XXXXXX")"
  local rw_dmg="$work/editable.dmg"
  local fixed_dmg="$work/fixed.dmg"
  local mountpoint=""

  cleanup() {
    if [[ -n "$mountpoint" && -d "$mountpoint" ]]; then
      hdiutil detach "$mountpoint" -quiet -force 2>/dev/null || true
    fi
    rm -rf "$work"
  }
  trap cleanup EXIT

  echo "==> Fixing Applications link in: $abs"

  # Compressed DMGs are read-only; convert to read-write first.
  hdiutil convert "$abs" -format UDRW -o "$rw_dmg" >/dev/null

  # Attach without mounting into /Volumes by default path — use a private mount.
  mountpoint="$work/mnt"
  mkdir -p "$mountpoint"
  hdiutil attach "$rw_dmg" -readwrite -nobrowse -mountpoint "$mountpoint" >/dev/null

  # Remove Finder alias / broken link, then create a real symlink.
  rm -rf "$mountpoint/Applications"
  ln -s /Applications "$mountpoint/Applications"

  # Flush and detach before recompressing.
  sync
  hdiutil detach "$mountpoint" -quiet
  mountpoint=""

  hdiutil convert "$rw_dmg" -format UDZO -imagekey zlib-level=9 -o "$fixed_dmg" >/dev/null
  mv -f "$fixed_dmg" "$abs"

  echo "    OK: Applications -> /Applications"
  trap - EXIT
  rm -rf "$work"
}

for dmg in "$@"; do
  fix_one "$dmg"
done
