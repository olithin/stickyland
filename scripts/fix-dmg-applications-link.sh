#!/usr/bin/env bash
# jpackage embeds a Finder alias for Applications that points at the build Mac,
# plus a .DS_Store whose icon positions reference that alias. On other machines
# drag-and-drop onto the Applications icon does nothing.
#
# Fix: replace the alias with a portable /Applications symlink, drop the stale
# .DS_Store (so Finder shows real icons again), and add a one-click installer.
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
  local mountpoint="$work/mnt"
  local attached=0

  cleanup() {
    if [[ "$attached" -eq 1 ]]; then
      hdiutil detach "$mountpoint" -quiet -force 2>/dev/null || true
      attached=0
    fi
    # Any leftover /Volumes/Stickyland from Finder auto-mount.
    if [[ -d /Volumes/Stickyland ]]; then
      hdiutil detach /Volumes/Stickyland -quiet -force 2>/dev/null || true
    fi
    rm -rf "$work"
  }
  trap cleanup EXIT

  echo "==> Fixing Applications link in: $abs"

  # Avoid colliding with a Finder-mounted read-only copy of the same volume name.
  if [[ -d /Volumes/Stickyland ]]; then
    hdiutil detach /Volumes/Stickyland -quiet -force 2>/dev/null || true
  fi

  hdiutil convert "$abs" -format UDRW -o "$rw_dmg" >/dev/null
  # jpackage images are packed tight; give a little headroom for the install script.
  hdiutil resize -size 350m "$rw_dmg" >/dev/null 2>&1 || true

  mkdir -p "$mountpoint"
  hdiutil attach "$rw_dmg" -readwrite -nobrowse -mountpoint "$mountpoint" >/dev/null
  attached=1

  if ! touch "$mountpoint/.stickyland-write-test" 2>/dev/null; then
    echo "Mount is not writable: $mountpoint" >&2
    return 1
  fi
  rm -f "$mountpoint/.stickyland-write-test"

  # IMPORTANT: if Applications is already a symlink to /Applications, never use
  # a follow-the-link delete. Unlink the name only.
  if [[ -L "$mountpoint/Applications" ]]; then
    rm -f "$mountpoint/Applications"
  elif [[ -e "$mountpoint/Applications" ]]; then
    # Finder alias is a file-like object, not a real directory.
    rm -rf "$mountpoint/Applications"
  fi

  # Stale layout points at build-Mac paths; without it Finder shows real icons.
  rm -f "$mountpoint/.DS_Store"
  rm -rf "$mountpoint/.fseventsd" "$mountpoint/.Trashes" "$mountpoint/.Spotlight-V100"

  ln -s /Applications "$mountpoint/Applications"

  cat > "$mountpoint/Install Stickyland.command" <<'INSTALL'
#!/bin/bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/Stickyland.app"
DST="/Applications/Stickyland.app"
if [[ ! -d "$SRC" ]]; then
  osascript -e 'display alert "Stickyland.app not found next to this installer." as critical' || true
  exit 1
fi
rm -rf "$DST"
ditto "$SRC" "$DST"
xattr -cr "$DST" 2>/dev/null || true
osascript -e 'display notification "Stickyland installed to Applications" with title "Stickyland"' || true
open -R "$DST"
INSTALL
  chmod +x "$mountpoint/Install Stickyland.command"

  sync
  hdiutil detach "$mountpoint" -quiet
  attached=0

  rm -f "$fixed_dmg"
  hdiutil convert "$rw_dmg" -format UDZO -imagekey zlib-level=9 -o "$fixed_dmg" >/dev/null
  mv -f "$fixed_dmg" "$abs"

  echo "    OK: Applications -> /Applications (+ Install Stickyland.command)"
  trap - EXIT
  rm -rf "$work"
}

for dmg in "$@"; do
  fix_one "$dmg"
done
