# desktop-runtime Specification

## Purpose

Defines the runtime and platform contract for the jpassvault desktop application: which JDK it runs on, where the UI toolkit comes from, how components are wired without a framework, and how the app is built and packaged for Linux.

## Requirements

### Requirement: JDK 25 runtime baseline

The application SHALL run on Java 25 (LTS). The build SHALL target Java 25 bytecode, and continuous integration SHALL build and package the application with a JDK 25 toolchain.

#### Scenario: Build and run on JDK 25

- **WHEN** the application is built with `./mvnw -B package` on a JDK 25 toolchain
- **THEN** compilation succeeds and a runnable artifact is produced

#### Scenario: CI uses JDK 25

- **WHEN** a push or pull request triggers the CI workflow
- **THEN** the workflow sets up a JDK 25 toolchain and runs `xvfb-run -a ./mvnw -B package`, so the toolkit-backed integration tests get a virtual display on a headless runner

### Requirement: JavaFX UI toolkit from Maven Central

The user interface SHALL be rendered with JavaFX artifacts resolved from Maven Central (org.openjfx), at the latest stable version line compatible with JDK 25. The build SHALL resolve the Linux-native JavaFX modules so the application runs on Linux.

#### Scenario: Linux build resolves JavaFX natives

- **WHEN** the project is built on Linux
- **THEN** the JavaFX controls and FXML modules, including Linux-native libraries, are resolved from Maven Central and packaged into the runnable artifact

#### Scenario: UI renders with JavaFX

- **WHEN** the packaged application starts on Linux
- **THEN** the unlock/setup/options/vault scenes render using JavaFX without any Spring framework on the classpath

### Requirement: Framework-free component wiring

The application SHALL start and operate without Spring Boot, Spring Context, or javafx-weaver on the classpath. Shared components (vault, options, session, crypto, remote sync) SHALL be instantiated once and injected through an application-owned composition root. FXML views SHALL be loaded through the standard JavaFX `FXMLLoader` mechanism backed by that composition root.

#### Scenario: Application starts without Spring

- **WHEN** the application main class is launched
- **THEN** the composition root constructs all shared components, the first scene is selected based on vault existence, and no Spring classes are required at runtime

#### Scenario: Scene switching works without Spring events

- **WHEN** a user triggers a scene transition (unlock, lock, options, cancel-from-options)
- **THEN** the active scene is replaced through the JavaFX event mechanism and the corresponding FXML view is displayed

### Requirement: Runnable Linux packaging

The build SHALL produce a single runnable jar for Linux containing the application classes, the JavaFX modules, and all other dependencies. The jar SHALL declare the JavaFX application entry point as its main class.

#### Scenario: Packaged jar launches the app

- **WHEN** the produced jar is executed with `java -jar` on Linux with JDK 25
- **THEN** the application window opens and shows the initial scene

### Requirement: Resizable window behaviour

The application SHALL open at a default size of 1000x600 pixels and SHALL hold that size until the user resizes the window. The unlock and vault views SHALL grow to fill the window; the setup and options views SHALL remain at their designed size. Individual controls SHALL keep their designed size regardless of window size, and controls in growing views SHALL stay anchored to the window rather than to the view's original position. Changing scenes SHALL NOT change the window size. Any area exposed by a window larger than its view SHALL be filled with the application background colour.

#### Scenario: Default size on launch

- **WHEN** the application starts and the window manager does not resize the window
- **THEN** the window opens at 1000x600 pixels

#### Scenario: Unlock controls stay centred at any size

- **WHEN** the unlock view is shown in a window larger or smaller than 1000x600
- **THEN** the password field and Unlock button remain centred, and the Options button remains an equal distance from the bottom and right edges

#### Scenario: Vault list stretches while form controls hold position

- **WHEN** the vault view is shown in a window larger than 1000x600
- **THEN** the password list fills the additional space while the form fields and buttons keep their designed size and position

#### Scenario: Scene change preserves a user resize

- **WHEN** the user resizes the window and then triggers a scene transition
- **THEN** the window keeps the size the user chose

#### Scenario: Exposed area uses the application background

- **WHEN** the window is larger than the view it displays
- **THEN** the area around the view is filled with the application background colour rather than a default colour

### Requirement: Backward-compatible data and protocol formats

The migration SHALL NOT change user data or external interfaces: the encrypted vault file format (`jpassdb.xdb`), the AES/GCM encryption with SHA3-256-derived keys, the `config.json` settings file, and the remote sync HTTP protocol SHALL remain byte- and behavior-compatible with the previous version.

This requirement rests on the crypto, serialization and protocol code being left untouched by the migration, not on an executed test: no vault file created by the pre-migration version was available to verify against, so cross-version loading is unproven by test.

#### Scenario: Existing vault opens after migration

- **WHEN** a user inspects a vault file and settings file created by the previous version
- **THEN** the file format, AES/GCM parameters, and SHA3-256 key derivation are identical, so the existing data loads without migration

#### Scenario: Remote sync interoperates with existing server

- **WHEN** online sync is enabled with a previously configured API URL and token
- **THEN** checksum checks, downloads, uploads, and merges behave identically to the previous version
