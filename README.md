# Stickyland

Local desktop notes app (Notion-style).  
**UI language: English.** Data stays **only on your computer** — no cloud, no accounts.

---

## Install (end users)

**Full install guide (English):** **[INSTALL.md](INSTALL.md)**

### Quick steps

1. GitHub → **Releases** → open version **v1.0.0** (or newer)
2. **Mac:** download **`.dmg`** → drag Stickyland to **Applications**
3. **Windows:** download **`.msi`** or **`.exe`** → run installer
4. Launch Stickyland

### What is created automatically

On **first launch**, Stickyland creates a **local SQLite database** and an images folder.  
You do **not** install a separate database server.

| OS | Data folder |
|----|-------------|
| macOS | `~/Library/Application Support/Stickyland/` (`notes.db` + `images/`) |
| Windows | `D:\Stickyland\` or `%LOCALAPPDATA%\Stickyland\` |

See [INSTALL.md](INSTALL.md) for details.

---

## Download by version

No Git clone needed. See **[RELEASES.md](RELEASES.md)**.

---

## Build from source (developers)

### Windows

```powershell
powershell -ExecutionPolicy Bypass -File install-desktop-shortcut.ps1
```

Dev run: `.\gradlew.bat run`

### macOS

Prefer a Release **`.dmg`**, or build with `./install-mac.sh` — [INSTALL-MAC.md](INSTALL-MAC.md).

---

## Requirements (building from source only)

| Item | Version |
|------|---------|
| JDK | **25** (LTS) |
| Gradle | 9.2.1+ (wrapper included) |

End users who install from **Releases** do **not** need JDK.

Local Windows JDK path in `gradle.properties` (developers):

```
org.gradle.java.home=C\:\\Users\\qaosi\\.jdks\\temurin-25.0.3+9
```

Change or comment out on other machines / CI.  
Download: [Temurin 25](https://adoptium.net/temurin/releases/?version=25).

---

## Features

| Feature | Description |
|---------|-------------|
| Notes | Create, edit, delete |
| Suites | Group notes by topic |
| Nested notes | Drag-and-drop reorder / nest |
| Screenshots | Paste or attach, fullscreen view |
| Search | Title and content |
| Auto dates | Created / modified |
| Auto-save | ~0.4s after typing |
| Local database | SQLite file created on first launch |

| Action | How |
|--------|-----|
| Paste screenshot | `Ctrl+V` (Windows) / try `Cmd+V` (Mac) |
| Add image file | **File** button in editor |
| Drag note | Hold **⠿** |

---

## Data & backup

Override data folder: environment variable `STICKYLAND_DATA_DIR`.

Windows backup example:

```powershell
Copy-Item "D:\Stickyland" "$env:USERPROFILE\Desktop\Stickyland-backup" -Recurse
```

---

## Publish a new version (maintainers)

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions builds DMG + Windows installers and attaches them to the release.  
Details: [RELEASES.md](RELEASES.md).

---

## Stack

| Part | Tech |
|------|------|
| UI | Kotlin 2.3 + Compose Desktop 1.10 |
| Database | SQLite (local file) + Exposed |
| Build | Gradle 9.2.1 |
| JDK | 25 LTS (bundled in packaged app) |

---

## License

MIT — see [LICENSE](LICENSE).
