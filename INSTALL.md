# Install Stickyland

**Language:** English (app UI and install docs).

Stickyland is a **local** notes app. Everything stays on your computer — no cloud, no account, no internet required after download.

---

## What gets installed

| Component | What it is |
|-----------|------------|
| **Stickyland app** | The program (window, editor, search) |
| **Local SQLite database** | Created **automatically on first launch** — stores your notes and suites |
| **Images folder** | Created automatically — stores pasted/attached screenshots |
| **Java runtime** | Bundled inside the app — you do **not** install JDK yourself |

You do **not** install PostgreSQL, MySQL, or any separate database server.  
The database is a single file (`notes.db`) managed by Stickyland.

### Where the database is created

| OS | Folder |
|----|--------|
| **macOS** | `~/Library/Application Support/Stickyland/` |
| **Windows** | `D:\Stickyland\` (if drive D: exists), otherwise `%LOCALAPPDATA%\Stickyland\` |
| **Linux** | `~/.stickyland/` |

Contents after first launch:

```
Stickyland/
├── notes.db      ← SQLite database (created automatically)
└── images/       ← screenshots (created automatically)
```

---

## macOS (from GitHub Releases)

1. Open the repository → **Releases**
2. Choose a version (e.g. **v1.0.0**)
3. Download the **`.dmg`** file
4. Double-click the DMG
5. Drag **Stickyland** into **Applications**
6. Open **Stickyland** from Applications  
   - On first launch the **local database** (`notes.db`) is created automatically  
   - If macOS blocks the app: right-click → **Open** → **Open**

No other software is required.

---

## Windows (from GitHub Releases)

1. Open the repository → **Releases**
2. Choose a version (e.g. **v1.0.0**)
3. Download the **`.msi`** (recommended) or **`.exe`**
4. Run the installer and follow the steps
5. Launch **Stickyland**  
   - On first launch the **local database** is created automatically

No other software is required.

---

## After install — first notes

1. Start Stickyland  
2. A default suite (**General**) is created if needed  
3. Create or edit notes — they are saved to the local SQLite database (auto-save)

---

## Uninstall / remove your data

- **Uninstalling the app** removes the program only.  
- **Your notes database** stays in the folders above until you delete that folder yourself.

To delete all notes on macOS:

```bash
rm -rf ~/Library/Application\ Support/Stickyland
```

---

## More detail

- Releases / version tags: [RELEASES.md](RELEASES.md)  
- macOS build from source: [INSTALL-MAC.md](INSTALL-MAC.md)  
- Full project docs: [README.md](README.md)
