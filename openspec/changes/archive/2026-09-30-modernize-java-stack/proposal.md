# Proposal

## Why

The application stack is end-of-life and outdated: Java 11, Spring Boot 2.4.5 (EOL), JavaFX 11, and `javafx-weaver` 1.3.0. Spring Boot is used only as a dependency-injection container and event bus for a desktop app — a heavyweight, unmaintained-in-this-role framework that drags in a large dependency tree. Modernizing to Java 25 LTS with a current JavaFX removes EOL software, shrinks the dependency footprint, and aligns the project with a supported LTS baseline while keeping user-visible behavior identical.

## What Changes

- **BREAKING**: Remove Spring Boot entirely (`spring-boot-starter`, `spring-boot-autoconfigure`, `spring-boot-maven-plugin`) and `javafx-weaver-spring-boot-starter`. Replace with a hand-rolled composition root (manual DI), `FXMLLoader` with a controller factory, and a minimal event bus.
- **BREAKING**: `StageActionEvent` no longer extends Spring's `ApplicationEvent`; scene-switching events move to a JavaFX-native event mechanism.
- Migrate JDK 11 → Java 25 (pom.xml, CI workflow, Maven tooling assumptions).
- Migrate JavaFX 11.0.1 → latest stable JavaFX from Maven Central (27 as of Sept 2026; 25.0.4 LTS as fallback), Linux classifier for builds.
- Replace Spring `@Value` configuration injection with a plain properties/constant for the UI title.
- Replace `spring-boot-maven-plugin` packaging with `maven-shade-plugin` producing a runnable fat jar (Linux).
- Drop the unused `io.projectreactor:reactor-core` dependency.
- Bump Gson to a current release.
- Update CI (`.github/workflows/maven.yml`) to JDK 25.
- Bump the application version to `2.0.0` to reflect the breaking runtime-stack change.
- Fix two pre-existing bugs found while smoke testing: a NullPointerException on first launch and on unlock (`Options.ifOnlineSyncOn()` dereferenced `apiUrl`/`tokenApi` that were never initialised, so `config.json` was written without them), and scenes opening 0x0 because `new Scene(root)` leaves the size uninitialised and lets the window manager choose the geometry.
- Make the window resizable with fixed-size controls: the app opens at 1000x600 and holds that size until the user resizes it, scene changes preserve a user resize, the unlock and vault views grow with the window while setup and options stay fixed, controls keep their designed size, and the unlock view centres its controls and anchors its Options button to equal bottom and right margins.
- User-visible behavior otherwise unchanged: same scenes and flows, same encrypted vault file format (`jpassdb.xdb`), same AES/GCM + SHA3-256 crypto, same `config.json` settings format, same remote sync protocol.

## Capabilities

### New Capabilities

- `desktop-runtime`: runtime and platform requirements for the desktop application — JDK baseline, UI toolkit sourcing, dependency-injection approach, packaging, and supported build/run platform.

### Modified Capabilities

None. The project has no existing specs; this change introduces the first capability spec.

## Impact

- `pom.xml`: Spring Boot parent removed; dependencies, properties, and build plugins replaced.
- `JpassvaultclientApplication` / `MainApplication`: no `SpringApplicationBuilder`; composition root initialized manually before the first scene loads.
- All classes under `com.samyisok.jpassvaultclient`: Spring annotations (`@Component`, `@Autowired`, `@Value`) removed; wiring moved to the composition root.
- Controllers (`MainController`, `OptionsController`, `SetupController`, `VaultController`): plain classes instantiated through an `FXMLLoader` controller factory backed by the composition root; `@FxmlView` annotations removed.
- `StageActionEvent`: re-based on JavaFX events instead of Spring `ApplicationEvent`.
- `application.properties`: Spring-specific keys removed; UI title sourced otherwise.
- CI workflow: `actions/setup-java` bumped to JDK 25.
- Tests: the vacuous Spring-based test class is deleted; a pure unit-test suite for the sizing logic (`ViewLoaderUnitTest`, Mockito, no toolkit or display needed) is added; `./mvnw compile` / `package` / `test` stay green.
- `main.fxml`: restructured to use anchors instead of absolute positions, so the unlock controls can be centred and the Options button anchored to the window corner. No other FXML and no CSS was changed.
- Environment, outside the repository: `~/.m2/settings.xml` had invalid XML that broke every Maven run (reworded comment); the wrapper `distributionUrl` now uses the Google Maven Central mirror because `wget` hangs against Central on this host.
- Unchanged: vault file format, crypto, settings file format, remote sync protocol, CSS, and the other three FXML views.
