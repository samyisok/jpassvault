# Design

## Context

See `proposal.md` — Why. Current state that shapes the approach:

- `ViewLoader.loadScene` builds every scene at the view's designed size, paints the frame with `APP_BACKGROUND = #beb`, and sets `scene.setFill(#beb)`. Before the Java 25/JavaFX 27 modernization the scene used JavaFX's default white fill, which is why the Options stage "lost" its white background: the options view never had a background of its own, it was showing the default scene fill.
- `options.fxml` is a fixed-size (1000x600) `BorderPane` whose centre `AnchorPane` is transparent; nothing paints the form area.
- All four views (`main.fxml`, `setup.fxml`, `options.fxml`, `vault.fxml`) pin `<Font name="Segoe UI"/>` per control. `vault.css` pins `.list-cell { -fx-font: 20pt "Segoe UIl"; }` — a misspelling, so that rule silently does nothing.
- Views are loaded through `FXMLLoader` in `ViewLoader`; each FXML declares its own `stylesheets="@buttons.css"`. There is no shared application stylesheet loaded at scene level.
- The app must keep running without Spring (see `desktop-runtime`) and packaged images must stay self-contained (see `portable-executables`).

## Goals / Non-Goals

**Goals:**

- One bundled typeface used by every view, rendering consistently on Linux, Windows, and macOS.
- A single place to change the UI font family; no platform-font names left in views.
- An explicit white background on the Options stage.
- Bundled font travels into the shaded jar and every `jpackage` image.

**Non-Goals:**

- Redesigning layouts, spacing, colours, or controls beyond the font family and the Options background.
- A full theming system or user-selectable fonts.
- Changing the resizable-window rules or the `APP_BACKGROUND` frame colour.
- Bundling a full colour-emoji font (see Risks).

## Decisions

### Bundle Open Sans rather than rely on system fonts

Add `OpenSans-Regular.ttf` and `OpenSans-Bold.ttf` (SIL Open Font License, with the `OFL.txt` license file) under `src/main/resources/com/samyisok/jpassvaultclient/fonts/`. Open Sans is a close, neutral substitute for Segoe UI, covers Latin/Cyrillic/Greek, and is small enough (~0.5 MB) to ship.

- *Alternatives considered:* a CSS font stack (`Segoe UI` → `system-ui` → `Helvetica Neue` → `DejaVu Sans`) is zero-download but renders differently per OS, which is exactly the problem being fixed; Noto Sans and Inter are equally valid but larger / less Segoe-like respectively. The family is recorded here so it can be swapped by editing the resource and the stylesheet, not the views.

### Register the font once at startup and set the family in a shared stylesheet

Load the bundled font in the application composition root (`AppFactory` / `MainApplication` startup) with `Font.loadFont(...)` and expose the resulting family name. Add a shared stylesheet (for example `app.css`) that sets `-fx-font-family` on the button/input/check control classes (JavaFX does not inherit the family from `.root` to those controls), and load it for every scene in `ViewLoader`. Remove the per-control `<font>` blocks from the FXML files entirely and fix `vault.css`; see the next decision for where the sizes go.

- *Alternatives considered:* injecting a `Font` object into each control from Java is more code and touches controllers; a controller-provided stylesheet is unnecessary. Loading at scene level in `ViewLoader` keeps one seam and works for all four views.

### Remove per-control font blocks and move sizes into the stylesheet

A size-only FXML font (`<Font size="18.0"/>`) still pins the family to the toolkit default (`System`), which defeats the shared `-fx-font-family` and leaves controls outside the bundled typeface. All four views therefore drop their `<font>` blocks entirely, and the designed sizes move to the stylesheets: `.button` 20px, `.smallButton` 15px (the two small Options buttons), `.iconButton` 18px (the generate-password glyph button), `.text-field`/`.password-field` 18px, and `.list-cell` 20pt. This keeps the family in exactly one stylesheet, as the spec's "one-place change" scenario requires.

- *Alternatives considered:* pinning `"Open Sans"` per control in FXML keeps sizes local but spreads the family across four files and breaks the single-source rule; a Java font-injection pass touches every controller and every view.

### Give the Options view its own white background

Set `-fx-background-color: #ffffff;` on the `options.fxml` root `BorderPane`. Because the options view is pinned to its designed size, the root fills the scene, so the whole Options stage is white and independent of the scene fill.

- *Alternatives considered:* changing `scene.setFill` to white for the options scene only would special-case `ViewLoader`; a `light panel` card treatment is deferred per the proposal's chosen plain-white look.

## Risks / Trade-offs

- [Bundled font does not cover the `🔒` emoji on the generate-password button] → JavaFX falls back to a platform emoji font; on headless or minimal Linux this may differ. Mitigation: treat emoji uniformity as out of scope, and if it proves distracting replace the glyph with an ASCII/vector marker in a follow-up. Tracked as an open question.
- [`Font.loadFont` returns `null` or a different family name than expected] → add a small registration helper that fails fast (or logs the family) and a unit test asserting the resource loads; use the returned font's family rather than a hard-coded string where possible.
- [Removing per-control `<font>` blocks could change control sizing] → the designed sizes are re-expressed in the stylesheet (see Decisions) and a rendered-scene integration test asserts the family; run the app to eyeball the sizes.
- [Font loading touches the JavaFX toolkit, which is headless-fragile in CI] → font registration is pure `Font.loadFont` (no stage); the CI already runs the toolkit-backed tests under `xvfb-run`.
- [Resource not included in packaged images] → resources live in `src/main/resources`, which both `maven-shade-plugin` and `jpackage` package; a task verifies the font inside the produced jar.

## Migration Plan

No data or settings migration: the change is presentation-only. Rollback is reverting the commit. The relevant guard is that the bundled font must be present in the jar/image, verified in tasks.

## Open Questions

- Should the `🔒` generate-password glyph be replaced with a uniform marker, or is platform emoji fallback acceptable? Default if unanswered: keep the emoji and accept platform fallback.
