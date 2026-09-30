# Design

## Context

See proposal.md — Why. Current state: Java 11, Spring Boot 2.4.5 (non-web) used only as DI + event bus, JavaFX 11.0.1, `javafx-weaver` 1.3.0 for FXML view loading, `spring-boot-maven-plugin` for packaging, Maven wrapper 3.8.1, CI on JDK 11. The app is small (~20 classes) with a fixed singleton component graph: `Options`, `OptionsLoader`, `Vault`, `VaultLoader`, `Session`, `AesCipher`, `RemoteVault`, `PasswordGenerator`, `StageHolder`, four controllers, and two event listeners. Local toolchain already runs JDK 25. Constraints: behavior must not change (vault format, crypto, settings format, remote protocol are user-visible contracts); `target/` is committed to git; verification is `./mvnw compile` / `package`.

## Goals / Non-Goals

**Goals:**

- Run on JDK 25, build and package with JDK 25 in CI.
- Zero Spring dependencies; zero `javafx-weaver`.
- JavaFX from Maven Central at the latest stable version line for JDK 25, Linux natives packaged.
- Single runnable fat jar via `java -jar`.
- Preserve all user-visible behavior and data formats.
- The window resizes while controls keep their designed size and views follow the window.

**Non-Goals:**

- No new features or workflow changes.
- No jlink/custom runtime image, no jpackage installer, no modularization (JPMS).
- No changes to vault crypto, settings format, or remote sync protocol.

**Scope added during implementation:** smoke testing surfaced two pre-existing bugs and a set of window-sizing defects that were not anticipated when this change was planned. Fixing them widened the change in two ways, both deliberate:

- `main.fxml` was restructured to use anchors instead of absolute positions. This was unavoidable: a control that must stay centred, or sit an equal distance from two edges, cannot be expressed with fixed `layoutX`/`layoutY`. No other FXML or any CSS was touched.
- A small pure unit-test suite (`ViewLoaderUnitTest`, Mockito) was added for the sizing logic. It needs no JavaFX toolkit and no display, so CI stays green; layout behaviour that genuinely needs a live stage is verified with throwaway probes rather than committed tests.

## Decisions

### D1: Hand-rolled composition root instead of a DI framework

**Decision:** A single `AppFactory`/`ServiceRegistry` class constructs the full object graph once at startup and hands controllers their dependencies. Controllers are plain classes (no annotations), instantiated once and cached by the composition root.

**Rationale:** The graph is ~10 singletons with no cycles, no scoping needs, and no optional dependencies. A DI framework (Dagger 2, Spring) would replace one framework with another for zero gain; manual wiring is ~50 lines and fully explicit.

**Alternatives considered:** Dagger 2 (compile-time DI) — adds annotation processing and a new dependency for a static graph; keeping Spring just for context — rejected by the migration goal.

### D2: JavaFX-native event mechanism for scene switching

**Decision:** `StageActionEvent` is re-based on `javafx.event.Event` (a dedicated `EventType` per action, registered as a child of a shared parent type so one handler receives all actions). Listeners register directly on the `Stage` via `stage.addEventHandler(...)`; firing uses `Event.fireEvent(stage, event)`, because `Stage` extends `Window` rather than `Node` and has no `fireEvent` of its own. `MainListener` and `StageInit` become plain classes registered by the composition root instead of Spring `ApplicationListener`s.

**Rationale:** Preserves the current decoupled, stage-driven scene switching with pure JavaFX — no new event-bus dependency, no behavioral change. `StageInit`'s first-scene logic moves into the startup path (composition root → stage ready → pick scene).

**Alternatives considered:** Hand-rolled pub/sub event bus — unnecessary; `Stage` is already the natural event target and lives for the app's lifetime. Direct method calls — would couple the composition root to scene-switching logic.

### D3: FXMLLoader with a composition-root-backed controller factory

**Decision:** A small `ViewLoader` utility wraps `FXMLLoader`, setting a `Function<Class<?>, Object>` controller provider backed by the composition root so each controller class is instantiated once and reused. `@FxmlView` annotations are removed; the FXML path is passed explicitly (same paths as today). `Initializable.initialize` is invoked by `FXMLLoader` as before. The loader depends on the provider rather than on `AppFactory` itself, so it stays decoupled and testable.

**Rationale:** This is exactly what `FxWeaver.loadView` did, minus Spring. Controllers stay singletons, so shared state (`Vault`, `Options`) is consistent across scenes.

**Alternatives considered:** Constructing controllers per-load — breaks singleton semantics of `Vault`/`Session` and would duplicate state.

### D4: Plain properties file for configuration

**Decision:** Keep `application.properties` as a plain properties file (drop the Spring-specific `spring.main.web-application-type` key, keep `spring.application.ui.title` renamed to `app.ui.title` or read as a constant). The composition root loads it once via `java.util.Properties` and exposes the title.

**Rationale:** Minimal change; the file already exists and the only injected value is the window title.

### D5: maven-shade-plugin fat jar

**Decision:** Replace `spring-boot-maven-plugin` with `maven-shade-plugin` 3.6.2 producing one runnable jar with `Main-Class` set to the JavaFX entry class. JavaFX's org.openjfx artifacts activate the correct platform classifier (linux) via OS-based Maven profiles — verified in the `org.openjfx:javafx` parent pom (`javafx.platform` property set by os-activated profiles: linux, linux-aarch64, mac, win) — so building on Linux pulls the linux natives into the shade. Because JavaFX jars are modular (contain `module-info.class`) and shade has no automatic module-info exclusion, the shade config MUST add a filter excluding `module-info.class` from `org.openjfx:*` artifacts; the app runs on the classpath where the JVM ignores module-info, so exclusion is safe.

**Rationale:** `java -jar` is the simplest distribution for a desktop app; shade is the standard way to build it with Maven. JavaFX works fine on the classpath.

**Alternatives considered:** jlink/jpackage — heavier, changes install UX, out of scope; keeping spring-boot-maven-plugin — impossible without Spring Boot.

### D6: JavaFX version — latest stable (27), fallback 25.0.4

**Decision:** Use the latest stable JavaFX release on Maven Central at implementation time — currently **27** (27.0.0, released 2026-09-15; 26.0.2 and 25.0.4 are the previous stable patches). Per OpenJFX docs, JavaFX 24+ requires **JDK 21 or later** (LTS: at least JDK 21) — JavaFX is NOT locked to the matching JDK version — so JavaFX 27 runs on JDK 25. Pin exactly in `pom.xml`; if a runtime incompatibility surfaces, fall back to 25.0.4 (LTS, JDK 21+).

**Rationale:** User asked for "the latest". Verified against Maven Central metadata (org.openjfx:javafx-controls maven-metadata.xml, last updated 2026-09-25) and OpenJFX system requirements.

**Outcome:** JavaFX **27** was used and the 25.0.4 fallback was not needed.

### D7: Maven wrapper — migrate to official maven-wrapper, Maven 3.9.16

**Decision:** Replace the EOL takari wrapper 0.5.6 with the official Apache `maven-wrapper` 3.3.x, distributionUrl = Maven **3.9.16** (latest 3.9.x). Install via `mvn wrapper:wrapper -Dmaven=3.9.16` (a system Maven 3.9.11 on JDK 25 is available locally for the one-time install).

**Rationale:** Maven 3.8.1 (2021) predates JDK 25 and is unsupported on it; 3.9.x is the current stable line and runs on JDK 25 (verified: system Maven 3.9.11 runs on local JDK 25.0.2). Takari wrapper is unmaintained; the official wrapper supports any Maven 3.x+ distribution.

### D8: CI workflow update

**Decision:** Update `.github/workflows/maven.yml`: `actions/setup-java` v2 → v4, distribution `adopt` → `temurin`, `java-version: '11'` → `'25'`, checkout v2 → v4.

**Rationale:** setup-java v2 and the AdoptOpenJDK distribution are deprecated/removed; v4 + temurin is the current standard and supports JDK 25.

### D9: Dependency sourcing — Maven mirror, verified 2026-09-28

**Decision:** Resolve all new dependencies through the Google Maven Central mirror already configured in `~/.m2/settings.xml` (`mirrorOf=central`). Fedora repos carry no openjfx packages, so Maven is the only JavaFX source. Maven Central itself also responded 200 at verification time; the mirror config is a harmless fallback, not a blocker.

**Verified fetchable (HTTP 200) on 2026-09-28:** org.openjfx JavaFX 27 set (controls, fxml, graphics, base) + `javafx-graphics-27-linux.jar` natives, Gson 2.14.0, maven-shade-plugin 3.6.2, maven-wrapper-plugin 3.3.2, apache-maven-3.9.16 distro zip.

**Rationale:** User reported low confidence in Central reachability; verification shows both routes work, so no extra mirror setup is needed.

### D10: Dependency cleanup

**Decision:** Remove `io.projectreactor:reactor-core` (zero usages in src), remove all Spring and javafx-weaver artifacts, bump Gson to **2.14.0** (latest release, April 2026).

**Rationale:** Unused dependency is dead weight; Gson 2.8.6 is from 2020.

### D11: Window sizing and per-view resize policy

**Decision:** `ViewLoader` owns all sizing in one place. It creates the scene at the view's preferred size (1000x600), and on every later scene swap it passes the live stage size so the window keeps the user's chosen dimensions. Views split into two policies: the unlock and vault views **grow** to fill the window (`min` cleared, `max` set to unbounded, `pref` kept as the starting size), while setup and options views are **pinned** to their designed size. Growing views are framed in a `StackPane` aligned `TOP_LEFT`; pinned views are centred. The frame carries `-fx-background-color: #beb` and the scene fill the same colour, so the area exposed by a larger window matches the FXML panels.

**Rationale:** `new Scene(root)` delegates with width/height `-1`, which leaves the scene size uninitialized and lets the window manager choose the geometry — that is what produced the original full-window stretch. Separately, a `Scene` carries its own size and a stage adopts it on `setScene`, so passing the designed size on every swap undid user resizes after login. Both are handled centrally rather than per view.

Setup and options are pinned because their controls are absolutely positioned: growing them would stretch empty background without moving anything. The unlock view is anchored in FXML because a control that must stay centred, or an equal distance from two edges, cannot be expressed with fixed `layoutX`/`layoutY`.

**Alternatives considered:** one policy for all views — rejected, it froze the vault list that should usefully fill extra space. Fixing the `-Infinity` max alone — tried and disproved by probe; the scene was still 0x0. `StackPane` + transparent `AnchorPane` for the unlock view — tried and rejected, a `StackPane` child keeps its own preferred size instead of filling.

### Risks / Trade-offs

- [Maven 3.8.1 fails on JDK 25] → Bump wrapper to 3.9.x first (D7); verify `./mvnw -v` works before touching code.
- [JavaFX natives not included in fat jar] → org.openjfx artifacts resolve the linux classifier via OS-activated profiles; verify the packaged jar contains `libprism*.so`/`libjavafx*.so` equivalents after first package run; fall back to an explicit `<classifier>linux</classifier>` dependency if profile activation misbehaves.
- [Latest JavaFX version has API changes vs 11] → The app uses stable, long-lived APIs (controls, fxml, collections); compile step will surface any breakage immediately. Risk assessed low.
- [Shade plugin chokes on JavaFX module-info.class] → JavaFX jars are modular (each contains module-info.class); shade has no automatic exclusion for them. Mitigation: filter excluding `module-info.class` from `org.openjfx:*` in the shade config (D5); also exclude `META-INF/versions` if multi-release metadata appears. If shade still fails, switch to `maven-assembly-plugin` with the same excludes.
- [Controller singleton semantics subtly change] → Composition root caches controllers exactly once, matching Spring singleton scope; smoke-test each scene transition after migration.
- [Fat jar size] → measured at 9.5 MB including Linux natives, not the ~100 MB originally estimated.
- [Pinned views clip in a window smaller than 1000x600] → the content is clipped rather than scaled; accepted, since shrinking the controls would contradict the fixed-control requirement.

## Migration Plan

Single-step swap: no data migration, no config migration, no rollback window needed. Rollback is a git revert of the merge. Deployment is replacing the jar. First implementation milestone: `./mvnw compile` green on JDK 25; second: `./mvnw package` produces jar that launches and passes a manual smoke test (setup → create vault → unlock → add/edit/delete record → lock → options → remote sync if configured).

Environment prerequisites discovered during implementation, outside the repository:

- `~/.m2/settings.xml` had invalid XML (`--` inside an XML comment) which made every Maven invocation fail. Fixed by rewording the comment.
- The wrapper `distributionUrl` points at the Google Maven Central mirror: `wget` hangs against `repo.maven.apache.org` on this host, while `curl` succeeds. A one-time `mvn wrapper:wrapper` needs a system Maven (3.9.11 was present).

Two pre-existing bugs were fixed during smoke testing and are not part of the migration itself: `Options.ifOnlineSyncOn()` dereferenced `apiUrl`/`tokenApi` that `setDefaultData()` never initialised, and scenes opened 0x0 because `new Scene(root)` leaves the size uninitialized. Both details are in `review.md`.

The app version was bumped `1.0.1` → `2.0.0` to reflect the breaking runtime-stack change.

## Open Questions

None. Exact JavaFX and Gson versions were pinned at implementation time per D6/D10 and did not change the approach.
