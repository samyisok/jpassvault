# Review

## DDD Verification — 2026-09-29

**Scope:** `git diff HEAD` (working tree vs HEAD)
**Files changed (src):** 22 Java + 1 resource
**Findings:** 6 open (all pre-existing), 1 category resolved

### Problems

- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/session/Session.java:3` — Anemic domain model: single field with getter/setter, no behavior. Pre-existing (this change only removed `@Component`). Move session/credential rules (e.g. clear-on-lock, validity) into `Session`.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/vault/Vault.java:5` — Anemic aggregate root: empty `HashMap` subclass leaking the full `Map` API; callers `put/remove/get` directly, so no invariant can be enforced. Pre-existing. Encapsulate in `Vault` with explicit add/remove/rename operations.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/vault/VaultLoader.java:15` — Persistence in domain package: direct `java.io` + Gson against `Vault`, no port/interface, so the domain depends on its storage format. Pre-existing. Extract a `VaultRepository` port, keep the file/Gson adapter in an infrastructure package.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/options/OptionsLoader.java:10` — Same as above for settings: file + Gson in the domain package, no port. Pre-existing.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/controllers/VaultController.java:143` — Business validation in the presentation layer (`create`, `save`, `delete` guard clauses decide what is persistable). Pre-existing. Move record rules into the `Vault` aggregate.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/controllers/SetupController.java:46` — Domain policy in presentation: password match and minimum-length rule enforced in a JavaFX controller. Pre-existing. Move to a `VaultPolicy`/domain service.

### Resolved by this change

- Infrastructure leaking into domain (`@Component`, `@Autowired`, `@Value` from `org.springframework.*`) was present in 9 domain/core classes (`Options`, `OptionsLoader`, `Vault`, `VaultLoader`, `Session`, `AesCipher`, `RemoteVault`, `PasswordGenerator`, `StageHolder`). All Spring imports are now gone; wiring moved to the `AppFactory` composition root.

### Not a finding

- No god class: largest file is `VaultController.java` (~250 lines), under the 350-line limit.
- No generic names (`*Manager`/`*Helper`/`*Util`) in the `domains/` packages.
- `RemoteVault` correctly sits outside `domains/` behind the `RemotableVault` interface.

### Recommendations

- None for this change: all open findings predate the migration, and the design's non-goals forbid behavior refactoring here. Track them in a separate change (`extract-domain-model-from-presentation`).

## GRASP Verification — 2026-09-29

**Scope:** `git diff HEAD` (working tree vs HEAD)
**Files changed (src):** 22 Java + 1 resource
**Findings:** 6 (2 introduced by this change, 4 pre-existing)

### Problems

- [x] `src/main/java/com/samyisok/jpassvaultclient/AppFactory.java:50` — Missing Polymorphism: `createController` selected controllers with an `if (type == X.class)` chain. **Fixed in this change:** replaced with a `Map<Class<?>, Supplier<Object>>` registry populated in the constructor; `instantiateController` looks up the supplier and fails fast on unknown types.
- [x] `src/main/java/com/samyisok/jpassvaultclient/ViewLoader.java:13` — Missing Indirection: depended on the concrete `AppFactory`. **Fixed in this change:** now takes a `Function<Class<?>, Object>` controller provider, so the loader is decoupled from the composition root and testable with a lambda.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/StageInit.java:52` — Low cohesion: the class mixes first-launch filesystem bootstrap (`prepareFirstLaunch`) with initial-scene selection and error dialogs; it has two unrelated reasons to change. Pre-existing responsibility, re-confirmed by this change. Extract a `FirstLaunchPreparator` (or move the bootstrap into `OptionsLoader`).
- [ ] `src/main/java/com/samyisok/jpassvaultclient/StageInit.java:62` — Information Expert: `StageInit` re-derives default paths (`Options.DEFAULT_FOLDER`, `DEFAULT_SETTINGS_FILE_NAME`) that `Options` owns; path knowledge is duplicated. Pre-existing. Delegate to `Options.getFullDefaultSettingsPath()` and let `OptionsLoader` create/save.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/crypto/AesCipher.java:14` — Missing Protected Variations: algorithm/mode hardcoded (`AES/GCM/NoPadding`) with no strategy interface, so an algorithm change forces edits in both encrypt and decrypt. Pre-existing. Introduce a `CipherStrategy` port with one `AesGcm` implementation.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/AppFactory.java:31` — High coupling: 13+ direct collaborators. Accepted by design: this is the composition root, whose responsibility is exactly to know every collaborator. Noted, no action.

### Not a finding

- `MainListener` centralizes the system event handling (Controller pattern) and `StageInit` owns startup — no scattered event logic in domain classes.
- `RemoteVault` sits behind the `RemotableVault` interface — a real protected variation point.
- `EventPublisher` is not pure fabrication: it lets controllers publish without holding a `Stage`, which is what keeps them decoupled.

### Recommendations

- For the two introduced findings: prefer the smallest fix that keeps the graph explicit — a controller registry map in `AppFactory` and a `Function`-typed dependency in `ViewLoader`.
- For the four pre-existing findings: same follow-up change as the DDD report; do not expand the migration.

## Bug Fixes Found During Smoke Testing — 2026-09-29

Both bugs predate the migration (same logic on Java 11/Spring); they surfaced only once the app was actually run. Both fixed in this change at user request.

### Fixed: NullPointerException on first launch and on unlock

- `domains/options/Options.java:109` — `ifOnlineSyncOn()` called `getApiUrl().isEmpty()`, but `setDefaultData()` only ever set `pathVault`, so `apiUrl`/`tokenApi` stayed `null`. Gson omits null fields, so `config.json` was written without them and every later run NPE'd. Fix: `setDefaultData()` now seeds both fields with `""` and `ifOnlineSyncOn()` null-checks before `isEmpty()`.
- Symptom was a silent failure: `createDb()` had already written the vault before `LOCK` threw, so the next run behaved as if creation had succeeded.

### Fixed: scenes opened 0x0, then stretched to the whole window

- `ViewLoader.java` — `new Scene(root)` delegates with width/height `-1`, which leaves the scene size uninitialized (verified in `javafx.scene.Scene` bytecode: `Scene(Parent)` → `init(-1.0, -1.0)` → `setWidth(-1)`). The window manager then chose the window geometry, and `main.fxml`'s solid-color `AnchorPane` with a four-anchored `PasswordField` smeared across it. `vault.fxml` looked fine only because its ListView/grid are designed to fill.
- Second contributing factor: all four FXML roots carry Scene Builder `maxWidth/maxHeight="-Infinity"`, which prevents the region from growing with the window.
- Fix: `ViewLoader.loadScene(fxmlPath)` passes an explicit size derived from the root's preferred size (1000x600 for all four views, verified), and `normalizeSize()` clears the bogus `-Infinity` max. Chosen over editing the FXML files to respect the design's "no FXML changes" non-goal.

### Fixed: content stretched when the window was resized

- `ViewLoader.java` — pinning the view to its designed size also stops the window from resizing the content. Two changes together give the requested behaviour: the view root is pinned on all four bounds (`pinToPreferred`), and the scene root is a resizable `StackPane` frame that centres the view (`Pos.CENTER`). The window therefore resizes freely while fields, buttons and lists keep the geometry the FXML designed.
- Verified against the packaged jar on a real stage: with the stage forced to 1500x1000, all four views keep their content at 1000x600, and each still opens at 1000x600 by default.
- Deliberately still no FXML edits: the anchoring that made the password field stretch (`AnchorPane` top/bottom/left/right anchors) is honoured again as soon as the container is no longer allowed to grow, so the FXML stays untouched.

### Fixed: window snapped back to 1000x600 on every scene change

- `MainListener.java` — a `Scene` carries its own size and a stage adopts it on `setScene`, so passing the designed size on every swap undid any resize the user had made. Every swap now passes the live stage geometry (`ViewLoader.of(stage)`), and the designed size is used only for the first scene. An earlier probe appeared to show the size was already preserved; that probe was wrong because it called `setWidth` first, which sets JavaFX's user-resize flag and suppresses the snap-back.
- Verified: stage at 1000x600 -> user resizes to 1400x900 -> unlock swaps scene -> still 1400x900 -> lock swaps back -> still 1400x900.

### Fixed: per-view resize behaviour

- `ViewLoader.java` — the single pin applied to all views. Now `grows(fxmlPath)` decides: the unlock and vault views grow with the window, the setup and options views stay at their designed size because their controls are absolutely positioned and growing would only stretch empty background.
- `main.fxml` — the Options button moved from fixed `layoutX/layoutY` to `bottomAnchor`/`rightAnchor`, so it stays in the window's bottom-right corner as the window grows. The password field lost its `bottomAnchor`/`rightAnchor` and gained explicit `prefHeight`/`prefWidth`, so it keeps its size instead of stretching.
- Verified against the packaged jar: unlock Options button moves `837,520` -> `837,920` when the scene goes 1000x600 -> 1500x1000; vault list grows `400x600` -> `900x1000`; setup and options content stays `1000x600`.

### Fixed: unlock view controls not centred, Options button margins uneven

- `main.fxml` — the password field and Unlock button were positioned with absolute `layoutX/layoutY` (388,250 and 434,296), so they sat in the top-middle of the 1000x600 design area and stayed there as the window grew. They now live in a `VBox` filling the view, so they are centred in the window at any size.
- The Options button used `bottomAnchor=25` but `rightAnchor=30`; both are now 30, so the gaps match.
- Structural note: the view root is a plain `AnchorPane` (it was a `BorderPane` wrapping an `AnchorPane` in `left`). Two intermediate attempts — `StackPane` + `VBox`, then `StackPane` + `VBox` + a transparent `AnchorPane` — were wrong: a `StackPane` child keeps its own preferred size instead of filling, so the green pane never grew and both the controls and the Options button were mispositioned. Reverted to a single `AnchorPane` with anchored children, which sizes and positions correctly.
- Verified at 1000x600, 1500x1000 and 1920x1080: horizontal offset of the password field from the window centre is 0 at every size, and the Options button gap is 30px on both the right and the bottom. All four views still lay out with non-zero content at both sizes.

### Fixed: white letterbox around the pinned view

- `ViewLoader.java` — once the view is pinned, a window larger than the view exposed the new frame in JavaFX's default white, so the content appeared to float in a white box. The frame now carries `-fx-background-color: #beb` and the scene fill is set to the same colour, so the exposed area matches the panels the FXML already uses.
- `#beb` is taken from the existing FXML palette (all four views use it for their panels, and `vault.css` uses it for list selection), so the app's look is unchanged and the colour lives in one place instead of being duplicated per view. Verified against the packaged jar: scene fill and frame background both resolve to `#bbeebb` for all four views.

### Tests

- `ViewLoaderUnitTest` (3 tests, Mockito) covers the size derivation and the four-bound pinning. These are pure unit tests: no JavaFX toolkit, no display, so CI stays green. The resize behaviour was verified with a throwaway probe against the packaged jar, not by a committed test, because asserting it needs a live stage.

### Also

- App version bumped `1.0.1` → `2.0.0` at user request (the migration is a breaking change to the runtime stack).
