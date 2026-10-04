# Proposal

## Why

Today the only distributable artifact is a shaded jar that requires users to install a Java 25 runtime before they can run the app. Users who just want the password vault must install a JDK/JRE first, which is a barrier for non-technical users. The project needs double-clickable executables for Linux, Windows, and macOS that run with no Java installed on the machine.

## What Changes

- Add a `jpackage`-based packaging step (already included in the JDK 25 toolchain) that produces a portable, self-contained application image per OS: a folder containing a native launcher (`jpassvaultclient` / `jpassvaultclient.exe`), the application jar (which already embeds the JavaFX classes and platform natives), and a bundled Java 25 runtime trimmed via `jlink` to the modules the application needs.
- Emit one portable archive per OS: `.zip` for Windows, `.tar.gz` for Linux and macOS (`.app` bundle) — unpack and run, no installer, no admin rights.
- Extend the existing Release workflow (tag-triggered, matrix already builds on ubuntu-latest, windows-latest, and macos-latest) to produce and upload those archives alongside the existing shaded jars, and to fail fast when the pushed tag does not match the pom version.
- Keep the existing shaded jar and its Linux run contract untouched — the jar remains a supported way to run the app.
- GraalVM `native-image` was researched and deliberately **not** chosen for this change (see design.md); it stays out of scope.

## Capabilities

### New Capabilities

- `portable-executables`: the build and release pipeline produce per-OS portable application images (Linux, Windows, macOS) that launch the vault without any Java installation on the user's machine.

### Modified Capabilities

<!-- none: desktop-runtime's existing requirements (JDK 25 baseline, runnable jar, framework-free wiring) keep holding; this change adds packaging on top, it does not alter those requirements -->

## Impact

- Build: no `pom.xml` change; packaging lives in repo scripts (`packaging/package.sh`, `packaging/package.ps1`) invoked by the release job and runnable locally; default `./mvnw package` behavior stays unchanged.
- CI/CD: `.github/workflows/release.yml` gains jpackage steps on the Linux, Windows, and macOS matrix legs and uploads the archives to the GitHub release.
- Dependencies: none added at runtime; `jpackage` and `jlink` ship with JDK 25.
- Output size: each archive carries a trimmed JRE (~60–100 MB) instead of a ~few-MB jar.
- Users: previously published jar-based instructions keep working; README gains download/run instructions for the executables.
