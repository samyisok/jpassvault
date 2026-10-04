# portable-executables Specification

## Purpose

Defines the per-OS portable executable artifacts for the jpassvault desktop app: the build and release pipeline produce unpack-and-run application images for Linux, Windows, and macOS that start the vault with no Java installation on the user's machine.

## Requirements

### Requirement: Portable application image per supported OS

The build SHALL produce a self-contained application image for Linux x64, Windows x64, and macOS arm64 using the JDK 25 `jpackage` tool. Each image SHALL contain a native launcher (`jpassvaultclient` under `bin/` on Linux, `jpassvaultclient.exe` on Windows, `jpassvaultclient` under `Contents/MacOS/` on macOS), the application jar with JavaFX classes and natives, and a bundled Java runtime that includes every module the application needs at runtime. The default build command `./mvnw -B package` SHALL NOT change: it keeps producing the shaded jar only, and image packaging SHALL be an explicit, separate step.

#### Scenario: Linux image is produced

- **WHEN** the packaging step runs on Linux with JDK 25
- **THEN** a `jpassvaultclient` application image directory is created containing the launcher `bin/jpassvaultclient`, the jar at `lib/app/jpassvaultclient.jar`, and the bundled Java runtime at `lib/runtime/`

#### Scenario: Windows image is produced

- **WHEN** the packaging step runs on Windows with JDK 25
- **THEN** a `jpassvaultclient` application image directory is created containing `jpassvaultclient.exe`, `app/jpassvaultclient.jar`, and a bundled `runtime/`

#### Scenario: macOS image is produced

- **WHEN** the packaging step runs on macOS (arm64) with JDK 25
- **THEN** a `jpassvaultclient.app` application bundle is created containing the launcher `Contents/MacOS/jpassvaultclient`, the jar at `Contents/app/jpassvaultclient.jar`, and the bundled Java runtime at `Contents/runtime/`

#### Scenario: Default build stays jar-only

- **WHEN** a developer runs `./mvnw -B package` without the packaging step
- **THEN** only the shaded jar is produced; no application image is created and no extra plugin runs

### Requirement: Application launches without an installed Java

A packaged application image SHALL start the vault on a machine where no Java runtime is installed, using only the bundled runtime. Launching the image SHALL open the same initial scene (setup or unlock) as running the shaded jar, and SHALL NOT require administrator rights or an installer.

#### Scenario: Linux image runs with no Java installed

- **WHEN** a user copies the unpacked Linux image to a machine with no `java` on `PATH` and executes `./bin/jpassvaultclient` from inside the extracted `jpassvaultclient/` directory
- **THEN** the application window opens at the initial scene

#### Scenario: Windows image runs with no Java installed

- **WHEN** a user unpacks the Windows image on a machine with no JRE installed and double-clicks `jpassvaultclient.exe`
- **THEN** the application window opens at the initial scene

#### Scenario: macOS image runs with no Java installed

- **WHEN** a user extracts the macOS archive on a machine with no `java` on `PATH` and launches `jpassvaultclient.app`
- **THEN** the application window opens at the initial scene

### Requirement: Packaged application preserves data and settings locations

Launching from a packaged image SHALL read and write the same user data locations as the jar run: the vault file `~/jpassvault/jpassdb.xdb` and the settings file `~/jpassvault/config.json`. An existing vault created with the jar version SHALL open unchanged from the packaged application.

#### Scenario: Existing vault opens from packaged app

- **WHEN** a user who already has `~/jpassvault/jpassdb.xdb` launches the packaged application
- **THEN** the unlock scene appears and the vault opens after unlocking with the same password

### Requirement: Release publishes portable archives

The tag-triggered release workflow SHALL attach one portable archive per supported OS to the GitHub release alongside the existing shaded jars: `jpassvaultclient-<version>-linux.tar.gz`, `jpassvaultclient-<version>-windows.zip`, and `jpassvaultclient-<version>-macos.tar.gz`. Each archive SHALL contain the unpacked application image so that extracting it produces a runnable directory. A release failure of the packaging step SHALL NOT upload a partial or empty archive.

#### Scenario: Tag release includes all archives

- **WHEN** a version tag (for example `v2.1.0`) is pushed and the release workflow completes
- **THEN** the release assets contain the shaded jars, `jpassvaultclient-2.1.0-linux.tar.gz`, `jpassvaultclient-2.1.0-windows.zip`, and `jpassvaultclient-2.1.0-macos.tar.gz`

#### Scenario: Tag and pom version mismatch fails the release

- **WHEN** a tag whose version differs from the `pom.xml` `<version>` (for example tag `v2.1.1` with pom `2.1.0`) is pushed
- **THEN** the release workflow fails before building or publishing any assets

#### Scenario: Extracted archive runs

- **WHEN** a user extracts the downloaded archive on its target OS and runs the launcher inside
- **THEN** the application starts without any Java installation
