# Stickyland releases (download installers by version)

This is how apps like VS Code / Notion ship builds: you open **Releases**, pick a version (`v1.0.0`), download the file for your OS. No need to clone the repository.

---

## For users (Mac)

1. Open the GitHub repo → **Releases**  
   (URL looks like `https://github.com/olithin/stickyland/releases`)
2. Open the version you want, e.g. **v1.0.0**
3. Download **`Stickyland-1.0.0.dmg`** (exact name may include the version)
4. Double-click the DMG → drag **Stickyland** into **Applications**
5. Launch the app — a **local SQLite database** is created automatically on first run  
   (no separate database install; see [INSTALL.md](INSTALL.md))
6. If Gatekeeper blocks it: right-click → **Open** → **Open**

Done.

---

## For users (Windows)

Same page → download **`.msi`** (installer) or **`.exe`**.  
On first launch Stickyland creates the **local SQLite database** automatically.  
Details: [INSTALL.md](INSTALL.md).

---

## For you (publisher) — one-time setup

### 1. Put the project on GitHub

```bash
cd My_Notes_Noition   # or rename the folder to Stickyland
git init
git add .
git commit -m "Initial Stickyland release setup"
```

Create an empty repo on GitHub (e.g. `stickyland`), then:

```bash
git remote add origin https://github.com/<YOUR_USER>/stickyland.git
git branch -M main
git push -u origin main
```

### 2. Ship a version

Every time you want a new public build:

```bash
# bump version in build.gradle.kts if needed (packageVersion / version)
git add -A
git commit -m "Release v1.0.0"
git tag v1.0.0
git push origin main
git push origin v1.0.0
```

Pushing the tag **`v1.0.0`** starts **GitHub Actions** (`.github/workflows/release.yml`):

- builds **macOS DMG** on a Mac runner  
- builds **Windows MSI/EXE** on a Windows runner  
- creates a **GitHub Release** with those files attached  

Wait for the green check on the **Actions** tab, then open **Releases**.

### 3. Next versions

```bash
git tag v1.0.1
git push origin v1.0.1
```

Users download `v1.0.1` — they never need the source code.

---

## Version numbering

| Tag | Meaning |
|-----|---------|
| `v1.0.0` | First public release |
| `v1.0.1` | Small fix |
| `v1.1.0` | New features |

Keep `version` / `packageVersion` in `build.gradle.kts` in sync with the tag when you can (e.g. `1.0.0` for tag `v1.0.0`).

---

## Notes

- **Mac DMG is built in the cloud** (GitHub’s Mac machines) — you do not need a Mac to *publish*, only to *test* locally if you want.
- The Mac app is **not notarized** yet. First launch may need right-click → Open.
- Local notes stay on the user’s machine (`D:\Stickyland` / `~/Library/Application Support/Stickyland`). Releases only ship the program, not their data.

---

## Local build without GitHub

| OS | Command |
|----|---------|
| Windows | `powershell -ExecutionPolicy Bypass -File install-desktop-shortcut.ps1` |
| macOS | `./install-mac.sh` |
