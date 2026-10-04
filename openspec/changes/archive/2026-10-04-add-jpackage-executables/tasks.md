# Tasks

## 1. Packaging scripts (Linux)

- [x] 1.1 Create `packaging/package.sh`: assemble `target/jpackage-input/` with the shaded jar renamed `jpassvaultclient.jar` (explicitly excluding the `original-*.jar` that maven-shade leaves beside it), run `jpackage --type app-image --name jpassvaultclient --main-class com.samyisok.jpassvaultclient.JpassvaultclientApplication` with the explicit `--add-modules` list from design D3
- [x] 1.2 Make the script take the version from `JPASSVAULT_VERSION` when set, else from the pom (no hardcoded version), and create `dist/jpassvaultclient-<version>-linux.tar.gz` with the archive layout from design D5
- [x] 1.3 Smoke-test the Linux image: extract the archive to a temp dir and run the launcher in a stripped environment (`env -i`, no `JAVA_HOME`, no `java` on `PATH`); add any missing module to the `--add-modules` list (found: `java.scripting`), use `ALL-MODULES` only as documented fallback
- [x] 1.4 Verify the default build is untouched: run `rm -rf target/app-image dist && ./mvnw -B package` and confirm the shaded jar is produced while `target/app-image/` and `dist/` stay absent

## 2. Packaging script (Windows)

- [x] 2.1 Create `packaging/package.ps1` implementing the same four steps as `package.sh` (input assembly excluding `original-*.jar`, `jpackage --type app-image`, version from `JPASSVAULT_VERSION` else pom, `Compress-Archive` to `dist/jpassvaultclient-<version>-windows.zip`)

## 3. Release workflow wiring

- [x] 3.1 Add a packaging step to all three legs (`ubuntu-latest`, `windows-latest`, `macos-latest`) of `.github/workflows/release.yml` that runs `packaging/package.sh` / `packaging/package.ps1` after the jar build with `JPASSVAULT_VERSION` set from the tag (`${GITHUB_REF_NAME#v}`), and uploads the archive as a per-OS matrix artifact (prefix `dist-`, distinct from the existing `jar-*` pattern) with `if-no-files-found: error`
- [x] 3.2 Extend the `release` job to download the archive artifacts and pass `dist/*.tar.gz` and `dist/*.zip` to `softprops/action-gh-release` `files:` with `fail_on_unmatched_files: true`
- [x] 3.3 Add a fail-fast step at the start of the build job asserting the tag version (`${GITHUB_REF_NAME#v}`) equals the `pom.xml` `<version>`, so a release can never publish assets under a version the pom does not carry

## 4. Documentation

- [x] 4.1 Add a README "Download and run (no Java needed)" section covering Linux (`tar -xzf` then `./jpassvaultclient/bin/jpassvaultclient`), Windows (unzip then `jpassvaultclient\jpassvaultclient.exe`), and macOS (`tar -xzf` then `open jpassvaultclient.app`), noting archive size (~60–100 MB), the Gatekeeper first-launch step on macOS, and that jar-based runs remain supported

## 5. macOS

- [x] 5.1 Extend `packaging/package.sh` for macOS: platform detection via `uname`, `.app` bundle paths (`Contents/MacOS` launcher, `Contents/app`, `Contents/runtime`), `-macos.tar.gz` archive suffix, portable JDK-home resolution without GNU `readlink -f`

<!-- Verification deferred until after this openspec change (by user decision): Windows GUI launch, macOS .app GUI launch, and a real tag-push release run (archives attached, fail_on_unmatched_files, tag/pom check). Tracked in design.md Open Questions. -->
