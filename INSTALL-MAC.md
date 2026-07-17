# Install Stickyland on macOS

**Language:** English.

Local notes app. On first launch Stickyland creates a **local SQLite database** on your Mac.  
You do **not** install any separate database software.

Full guide for all platforms: **[INSTALL.md](INSTALL.md)**

---

## From GitHub Releases (recommended)

1. Open **Releases** on the Stickyland GitHub repo  
2. Pick a version (e.g. **v1.0.0**)  
3. Download the **`.dmg`**  
4. Open it → drag **Stickyland** into **Applications**  
5. Launch Stickyland — the local database (`notes.db`) is created automatically  

If macOS blocks the app: right-click → **Open** → **Open**.

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

Requires **JDK 25** (`brew install --cask temurin@25`).  
Comment out the Windows `org.gradle.java.home=...` line in `gradle.properties` if present.

Publisher / version tags: [RELEASES.md](RELEASES.md)
