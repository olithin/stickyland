# UI testing

Stickyland is a **Compose Desktop** app (JVM), not a web page.

## Why not Playwright?

[Playwright](https://playwright.dev) drives browsers (Chromium / Firefox / WebKit).
It cannot attach to a Compose/Skia window the way it attaches to a DOM.

For this stack the official equivalent is **Compose UI testing**
(`createComposeRule` / `testTag` / click / type / assert) — same ideas as Playwright,
different host.

## Run

```bash
./gradlew test --tests 'com.mynotes.ui.StickylandUiTest'
```

On Linux CI a virtual display is required (`xvfb-run`).

## Selectors

Stable tags live in `TestTags` (e.g. `btn_new_suite`, `note_title`, `mode_schema`).
Prefer tags over brittle layout coordinates — same discipline as Playwright `getByTestId`.
