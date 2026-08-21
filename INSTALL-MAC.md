# Install Stickyland on macOS

**Language:** English.

Local notes app. On first launch Stickyland creates a **local SQLite database** on your Mac.  
You do **not** install any separate database software.

Full guide for all platforms: **[INSTALL.md](INSTALL.md)**

---

## From GitHub Releases (recommended)

Requires **macOS 11 (Big Sur)** or later.

1. Open **Releases** on the Stickyland GitHub repo  
2. Pick a version (e.g. **v1.0.0**)  
3. Download the **`.dmg` for your Mac**:
   - Apple Silicon (M1/M2/M3/M4): `*-mac-arm64.dmg`
   - Intel: `*-mac-x64.dmg`  
   Apple menu → About This Mac shows the chip.
4. Open it → drag **Stickyland** into **Applications**  
5. Launch Stickyland — the local database (`notes.db`) is created automatically  

If macOS blocks the app: right-click → **Open** → **Open**.

An Apple Silicon DMG will not launch on an Intel Mac.

---

## What is created on first launch

```
~/Library/Application Support/Stickyland/
├── notes.db      ← SQLite database (auto-created)
└── images/       ← screenshots (auto-created)
```

---

## Build the DMG from source (developers)

```bash
chmod +x install-mac.sh
./install-mac.sh
open build/compose/binaries/main/dmg/*.dmg
```

Requires **JDK 21** (`brew install --cask temurin@21`).  
Comment out the Windows `org.gradle.java.home=...` line in `gradle.properties` if present.

A local DMG only runs on the same CPU as the Mac you built it on. GitHub Releases ship both `arm64` and `x64`.

Publisher / version tags: [RELEASES.md](RELEASES.md)
