# Proposal

## Why

The desktop UI has never been stabilized for cross-platform look and feel. Every FXML view pins the typeface to `Segoe UI`, a Windows-only font, so on Linux and macOS the app silently falls back to whatever the toolkit picks; the vault list stylesheet even names a font that does not exist (`Segoe UIl`). The result is that the app renders in inconsistent, OS-dependent typography, and there is no guarantee any two installs look alike. Separately, the Options stage lost its light background when the app adopted a global green-grey scene fill, so the options form now blends into the application background instead of reading as a distinct zone.

## What Changes

- **Bundle one open-source typeface** (Open Sans, SIL Open Font License) as an application resource and apply it to every view, so the UI renders identically on Linux, Windows, and macOS regardless of installed fonts.
- **Centralise typography**: define fonts in stylesheets (with per-control sizing preserved) and remove per-control `Segoe UI` pins and the `Segoe UIl` typo, so there is a single source of truth for the UI font.
- **Restore an explicit white background on the Options stage** so the options zone is visually distinct from the application background again.
- **Small cleanup**: remove dead/commented stylesheet rules touched while centralising typography.

Not breaking. No data, settings, encryption, or remote sync behaviour changes.

## Capabilities

### New Capabilities

- `desktop-ui`: the visual contract for the desktop application — bundled typeface and cross-platform rendering, and the distinct background treatment of the Options stage.

### Modified Capabilities

- (none)

## Impact

- Resources: `src/main/resources/com/samyisok/jpassvaultclient/*.fxml`, `buttons.css`, `vault.css`, plus a new bundled `.ttf` and its license file under `src/main/resources`.
- Code: font registration at application startup (`MainApplication`/`AppFactory` composition root).
- Build/packaging: the font resource must be included in the shaded jar and in every `jpackage` application image, so the packaged executables share the same rendering guarantee.
- No vault file, settings file, encryption, or network protocol impact.
