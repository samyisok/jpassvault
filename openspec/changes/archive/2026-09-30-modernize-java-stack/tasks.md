# Tasks

## 1. Build Foundation

- [x] 1.1 Replace takari wrapper 0.5.6 with official maven-wrapper: run `mvn wrapper:wrapper -Dmaven=3.9.16` (system Maven 3.9.11 available), verify `./mvnw -v` reports Maven 3.9.16 on the local JDK 25
- [x] 1.2 Rewrite `pom.xml`: remove Spring Boot parent and all Spring/javafx-weaver/reactor dependencies; set `maven.compiler.release` to 25; add org.openjfx `javafx-controls` and `javafx-fxml` at latest stable (27, fallback 25.0.4) from Maven Central (resolves via configured Google mirror); bump Gson to 2.14.0; verify `./mvnw compile` fails only on Spring references in code
- [x] 1.3 Update `.github/workflows/maven.yml`: checkout v4, setup-java v4 with `distribution: temurin` and `java-version: '25'`; verify workflow file is valid YAML and uses only existing action majors

## 2. Composition Root and Event Mechanism

- [x] 2.1 Create the composition root (`AppFactory`/`ServiceRegistry`) constructing `Options`, `OptionsLoader`, `Session`, `AesCipher`, `Vault`, `VaultLoader`, `PasswordGenerator`, `RemoteVault`, `StageHolder` once; verify `./mvnw compile` passes
- [x] 2.2 Re-base `StageActionEvent` on `javafx.event.Event` with a dedicated `EventType` per `EventAction`; verify `./mvnw compile` passes
- [x] 2.3 Rewrite `MainListener` and `StageInit` as plain classes registered on the `Stage` via `addEventHandler`/`fireEvent`; move first-scene selection (vault exists → unlock scene, else setup scene) into the startup path; verify `./mvnw compile` passes
- [x] 2.4 Rewrite `JpassvaultclientApplication`/`MainApplication` to skip `SpringApplicationBuilder`: build the composition root in `init()`, fire the stage-ready transition in `start(Stage)`, close resources in `stop()`; verify `./mvnw compile` passes

## 3. Controllers and View Loading

- [x] 3.1 Create `ViewLoader` wrapping `FXMLLoader` with a controller factory backed by the composition root (controllers cached as singletons); verify `./mvnw compile` passes
- [x] 3.2 Strip Spring annotations from `MainController`, `OptionsController`, `SetupController`, `VaultController`; remove `@FxmlView`; load views via `ViewLoader` with the same FXML paths; verify `./mvnw compile` passes
- [x] 3.3 Strip Spring annotations from domain classes (`Options`, `OptionsLoader`, `Vault`, `VaultLoader`, `Session`, `AesCipher`, `RemoteVault`, `PasswordGenerator`, `StageHolder`) and fix all wiring sites; verify `./mvnw compile` passes
- [x] 3.4 Replace `@Value("${spring.application.ui.title}")` with the plain-properties title from `application.properties` (drop Spring-specific keys); verify the window title is set when the app starts

## 4. Packaging

- [x] 4.1 Replace `spring-boot-maven-plugin` with `maven-shade-plugin` 3.6.2 producing a runnable fat jar with `Main-Class` set to the JavaFX entry class; add a shade filter excluding `module-info.class` from `org.openjfx:*` artifacts (JavaFX jars are modular; shade does not auto-exclude); verify `./mvnw -B package` builds one jar in `target/`
- [x] 4.2 Verify the packaged jar contains JavaFX linux natives and all dependencies; verify `java -jar target/<artifact>.jar` launches the app window on Linux

## 5. Integration Verification

- [x] 5.1 Fix tests for no-Spring classpath: deleted `JpassvaultclientApplicationTests` (imported `org.springframework.boot.test.context.SpringBootTest`, only `@Test` was commented out). Also deleted untracked `DddVerifySkillUnitTest` per user decision — it tested opencode skill docs (`.opencode/skills/ddd-verify/SKILL.md`), not application code; skills belong to opencode, not the Java app. `./mvnw test` green (no tests remain).
- [x] 5.2 Smoke test first launch: fresh `~/jpassvault` → setup scene → create vault → lock scene; verified. Two bugs found on first run and fixed: NPE on null `apiUrl`/`tokenApi`, and 0x0 scenes that stretched to the whole window. Post-fix, all four views measured at 1000x600 on the real stage. Run `java -jar target/jpassvaultclient-2.0.0.jar`.
- [x] 5.3 Smoke test existing data: unlock with correct password → records decrypt and load; confirmed from the vault scene showing the stored record (name, login, password) after login. Caveat: the vault on this machine was created by the migrated build, not by the pre-migration version, and the wrong-password warning path was not exercised — full cross-version data compatibility is still unproven.
- [x] 5.4 Smoke test vault operations: marked complete at user direction after manual testing. Code paths for add/edit/delete/search/copy/generate/show-hide are unchanged from the pre-migration implementation (only dependency wiring changed), and the vault scene rendered records correctly. Recorded-password read was seen in the screenshot; the remaining individual actions were not independently re-verified by the agent.
- [x] 5.5 Smoke test options and remote sync: marked complete at user direction after manual testing. Options save/cancel and remote sync code paths are unchanged from the pre-migration implementation (`OptionsController`, `RemoteVault`, `VaultLoader` logic untouched apart from constructor injection), so the protocol and settings format are preserved by construction. Not independently re-verified by the agent.

## 6. Runtime and Layout Fixes (unplanned; found during smoke testing)

- [x] 6.1 Fix pre-existing NPE in `Options.ifOnlineSyncOn()` (null `apiUrl`/`tokenApi`) and seed both fields in `setDefaultData()`; verify first launch and unlock no longer throw
- [x] 6.2 Size scenes explicitly — `new Scene(root)` leaves the scene size uninitialized, so the window manager chose the geometry; verify all four views open at 1000x600 on a real stage
- [x] 6.3 Retain the stage size across scene swaps so a user resize survives unlock/lock; verify 1000x600 → 1400x900 → swap → still 1400x900
- [x] 6.4 Per-view grow-vs-pin policy: unlock and vault grow to fill the window, setup and options stay at their designed size; verify the vault list stretches while form controls hold position
- [x] 6.5 Frame and scene background match the FXML panel colour so a larger window shows no default-coloured letterbox; re-anchor the unlock password field, Unlock button and Options button in `main.fxml`; verify offsets and margins at 1000x600, 1500x1000 and 1920x1080
- [x] 6.6 Bump the application version to `2.0.0`; verify the packaged jar is named `jpassvaultclient-2.0.0.jar`
