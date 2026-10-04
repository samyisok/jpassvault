# Design

## Context

- App: JavaFX 25 desktop vault, framework-free (no Spring since the earlier migration), shaded jar via `maven-shade-plugin`, main class `com.samyisok.jpassvaultclient.JpassvaultclientApplication`.
- JavaFX comes from Maven Central as classpath dependencies (controls + fxml, version 27) with platform natives inside the jars; the app runs in JavaFX classpath mode, not module path.
- Toolchain: JDK 25 (Temurin) locally and in both workflows; `jpackage`/`jlink` ship with it — no new dependency.
- Release workflow (`.github/workflows/release.yml`, tag `v*`) already runs a `ubuntu-latest` / `windows-latest` / `macos-latest` matrix, builds the jar, and a `release` job uploads assets with `softprops/action-gh-release`.
- Motivation and scope: see proposal.md. GraalVM `native-image` was researched (see Decisions) and rejected for this change.

## Goals / Non-Goals

**Goals:**

- One unpack-and-run portable image per OS: Linux x64, Windows x64, macOS arm64.
- Zero Java requirement on the end user's machine; no installer, no admin rights, no registry changes.
- Reproducible locally with the same command CI uses.
- Default `./mvnw -B package` behavior byte-for-byte unchanged (jar only).

**Non-Goals:**

- macOS Intel (x64) image — only arm64 (the `macos-latest` runner arch) is built; an Intel leg can be added later.
- GraalVM `native-image` / GluonFX single-binary output — research outcome recorded in Decisions, deferred.
- Installers (`.msi`/`.exe` setup, `.deb`/`.rpm`) — portable archive only, per user decision.
- Custom app icon, code signing, notarization, auto-update.

## Decisions

### D1: `jpackage --type app-image` over GraalVM `native-image`

Research summary:

| | jpackage app-image | GraalVM native-image (GluonFX) |
|---|---|---|
| Java on user machine | not needed (bundled runtime) | not needed |
| Output | folder + native launcher | single native binary |
| Cold start | seconds (JVM warmup) | ~100 ms |
| Toolchain | in JDK 25 already | GraalVM JDK 25 + gcc (Linux) / MSVC 17+ (Windows) |
| JavaFX support | official, classpath mode just works | via Gluon Substrate only; must track JavaFX 27 |
| Extra config | none | reachability metadata for Gson, FXML resources, reflection |
| Failure risk | low | medium-high: substrate/JDK 25/JavaFX 27 matrix unproven |

GraalVM *can* build a Windows `.exe` (confirmed: `native-image` ships for Windows and needs only MSVC Build Tools), but for JavaFX the actual work is done by Gluon Substrate, and JavaFX 27-on-JDK-25 support there is unproven. With a user decision for **jpackage only**, D1 picks the low-risk official path. Alternatives considered: GraalVM native-image (rejected — higher risk, deferred), `jlink` + shell-script launcher (rejected — `.sh`/`.bat` launchers are not the "double-clickable executable" asked for, and `jpackage` wraps the same jlink step anyway).

### D2: Packaging driven by repo scripts, not a Maven profile

`jpackage` has no first-class Maven plugin in the standard toolchain; wiring it through `exec-maven-plugin` + copy steps inside `pom.xml` couples the default build to packaging and risks perturbing `./mvnw -B package`. Instead: `packaging/package.sh` (bash, Linux) and `packaging/package.ps1` (PowerShell, Windows) that (1) run `./mvnw -B package` if the jar is missing, (2) assemble `target/jpackage-input/` with the renamed `jpassvaultclient.jar`, (3) invoke `jpackage`, (4) create the archive. CI and a local developer run the identical script — same commands, no drift. Alternatives considered: Maven `dist` profile (rejected — build coupling, cross-plugin ordering fragility), `jpackage-maven-plugin` third-party (rejected — unmaintained, extra dependency for a step one script does).

### D3: `jpackage` invocation

```
jpackage \
  --type app-image \
  --name jpassvaultclient \
  --app-version <pom version, e.g. 2.1.0> \
  --input  target/jpackage-input \
  --main-jar jpassvaultclient.jar \
  --main-class com.samyisok.jpassvaultclient.JpassvaultclientApplication \
  --dest target/app-image
```

- `--type app-image` needs no WiX (Windows) and no `fakeroot` (Linux) — both are installer-only requirements.
- `--app-version` must be `x.y.z` for Windows version resources; the pom version (`2.1.0`) already conforms. Version source of truth: the script reads `JPASSVAULT_VERSION` when set, otherwise derives it from the pom — CI sets it from the release tag (`${GITHUB_REF_NAME#v}`), exactly like the existing jar-naming step, so archives and jars can never disagree; local runs default to the pom version.
- Module set: explicit `--add-modules` list rather than the default `ALL-MODULES`, targeting the modules a classpath JavaFX app needs: `java.base,java.desktop,java.logging,java.naming,java.net.http,java.prefs,java.scripting,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs` (JavaFX's own natives ship inside the shaded jar and load from the classpath, so they need no jlink modules; `java.scripting` is required because FXML's namespace class implements `javax.script.Bindings`). If the smoke test shows a missing module, add it to the list; falling back to `ALL-MODULES` is the documented escape hatch at ~2× image size.
- `--jlink-options` must restate jpackage's defaults (`--strip-native-commands --strip-debug --no-man-pages --no-header-files`) because passing the option replaces them. On distro-patched JDKs (Fedora rewrites `conf/security/java.security` for crypto policies) jlink aborts with "has been modified"; the scripts probe for `--ignore-modified-runtime` and add it only where jlink supports it, so unmodified Temurin CI is unaffected.
- Platform layouts differ: Linux image = `bin/jpassvaultclient` + `lib/app/` + `lib/runtime/`; Windows image = `jpassvaultclient.exe` + `app/` + `runtime/` at the image root; macOS image = `jpassvaultclient.app/Contents/{MacOS/jpassvaultclient, app/, runtime/}`.
- Distro-patched `java.security` references files jlink does not copy (`conf/security/redhat/*` on Fedora) and crashes the bundled app at startup (`Unable to include 'redhat//crypto-policies.properties'`). The scripts swap in the packaging JDK's `java.security.upstream` (Fedora's documented stock file for linked images) after jpackage; where that file does not exist (Temurin CI) the step is a no-op.

### D4: CI wiring in the existing Release workflow

- Keep the current jar jobs untouched (spec: default build stays jar-only).
- Add a packaging step to all three matrix legs: run `packaging/package.sh` on Linux and macOS (the script detects its own platform via `uname`), `packaging/package.ps1` on Windows (the workflow branches only on `RUNNER_OS` for shell availability), upload `dist/*.tar.gz` / `dist/*.zip` as matrix artifacts (named per-OS to survive `merge-multiple`).
- macOS runner caveat: GitHub-hosted macOS runners have no GUI session, so the macOS leg verifies packaging and archive structure only — the GUI launch scenario is checked manually on a real Mac.
- `release` job: download the new artifacts alongside jars, pass `dist/*.tar.gz` and `dist/*.zip` to `softprops/action-gh-release` `files:` with `fail_on_unmatched_files: true` so a missing archive fails the release instead of publishing a partial asset set; `if-no-files-found: error` on `upload-artifact` guards the earlier step.
- Version safety: a first step in the build job asserts `${GITHUB_REF_NAME#v}` equals the pom's `<version>` and fails fast on mismatch — no auto-bump, no commit-back; the maintainer bumps the pom (e.g. `mvn versions:set`) before tagging. One version source end to end: pom = tag = asset names.

### D5: Archive layout

- Linux: `tar -czf jpassvaultclient-<version>-linux.tar.gz -C target/app-image jpassvaultclient`
- macOS: `tar -czf jpassvaultclient-<version>-macos.tar.gz -C target/app-image jpassvaultclient.app` (tar preserves the bundle structure and executable bits; a `.dmg` would need no extra tooling either but adds nothing for a portable image)
- Windows: `Compress-Archive -Path target/app-image/jpassvaultclient -DestinationPath dist/jpassvaultclient-<version>-windows.zip`
- Extracting yields a single directory (`jpassvaultclient/` or `jpassvaultclient.app/`) containing the launcher — the spec's "extract and run" contract.

## Risks / Trade-offs

- [Explicit jlink module list misses a runtime-need module → app crashes at startup] → smoke test in tasks runs the packaged image (xvfb on Linux CI); `ALL-MODULES` fallback documented in D3.
- [JavaFX classpath-mode natives don't resolve from the bundled runtime → toolkit fails to load] → the shaded jar already contains natives and is proven by the existing `java -jar` contract; smoke test re-verifies from the image.
- [Archive size grows from a few MB to ~60–100 MB] → accepted; trimmed runtime is still far smaller than a full JDK. README should set expectations.
- [Windows Defender / SmartScreen warnings on unsigned portable exe] → accepted (code signing is a non-goal); note in README.
- [Version string drift between pom, tag, archive name, and `--app-version`] → one source: scripts prefer `JPASSVAULT_VERSION` (CI sets it from the release tag, matching the existing jar-naming step) and fall back to the pom version locally; they never hardcode.
- [Windows packaging script differs from Linux shell script → behavior drift] → both scripts implement the same 4 steps and are exercised together by the same release workflow run.
- [Unsigned macOS app trips Gatekeeper ("cannot be opened because the developer cannot be verified") → user friction] → accepted; code signing/notarization is a non-goal, README notes the right-click → Open first-launch step.
- [GitHub macOS runner is headless → GUI launch scenario unverifiable in CI] → CI verifies packaging and `.app` structure; GUI launch is a manual check on a real Mac (spec scenario stands, verification is manual).
- [BSD userland on macOS (`readlink` has no `-f` in older versions, bash 3.2) → script breaks on the runner] → package.sh avoids GNU-only constructs: portable `dirname` chaining for JDK home, `uname` for platform detection, no arrays.

## Migration Plan

No data migration: same vault/config files (spec requirement "Packaged application preserves data and settings locations"). Rollback: delete the packaging scripts and workflow steps; jar release path is untouched throughout. Deployment = tag push as today.

## Open Questions

- App icon / branded `.exe` resource: deferred (non-goal), can be added later without spec changes.
- Whether to publish an Intel (x64) macOS image later: deferred, does not affect this change's tasks.
- Verification deferred by user decision until after this openspec change: Windows image GUI launch (2.2), macOS `.app` GUI launch and bundle structure on CI (5.2), and a real tag-push release run exercising the archives, `fail_on_unmatched_files`, and the tag/pom version check (3.3).
