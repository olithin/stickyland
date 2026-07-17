# Install Stickyland

Download an installer from **GitHub → Releases**.  
No Git, no PowerShell scripts, no JDK.

---

## Windows

1. Open **Releases** → pick a version (e.g. **v1.0.0**)
2. Download the **`.msi`** installer
3. Double-click it and complete the setup wizard
4. Launch **Stickyland** from the Start menu or desktop

The app icon is included in the installer (from `icon.ico` in the project).

On **first launch**, Stickyland creates a **local SQLite database** automatically:

- `D:\Stickyland\notes.db` (if drive D: is available), or  
- `%LOCALAPPDATA%\Stickyland\notes.db`

You do **not** install any separate database software.

---

## macOS

1. Open **Releases** → pick a version
2. Download the **`.dmg`**
3. Open it → drag **Stickyland** into **Applications**
4. Launch Stickyland  
   - If macOS blocks it: right-click → **Open** → **Open**

On **first launch**, the local database is created at:

```
~/Library/Application Support/Stickyland/notes.db
```

---

## What you get

| Included | Notes |
|----------|--------|
| Stickyland app | With icon |
| Bundled Java runtime | No JDK install |
| Local SQLite database | Created on first launch |
| Images folder | For screenshots |

---

## Uninstall

Removing the app does **not** delete your notes.  
To remove all data on Windows, delete the `Stickyland` data folder above.

---

More: [README.md](README.md) · [RELEASES.md](RELEASES.md)
