# Review

Planning-artifact review (pre-implementation). Scope: `openspec/changes/prefill-search-after-record-create/` against `src/`, `pom.xml`, `.github/workflows/maven.yml`. No production code touched by this review.

Findings: 1 blocking, 3 consistency, 2 verification gaps, 4 notes. Blocking + consistency + verification gaps resolved by editing the artifacts in this change.

## Blocking

- [x] **`design.md` D5 and tasks 3.1–3.5 were infeasible as written.** The design claimed that removing the inline `@FXML` initializers makes `VaultController` constructible and therefore testable with Mockito-mocked controls. It does not: `mock(TextField.class)` fails headless with `Mockito cannot mock this class: class javafx.scene.control.TextField ... Underlying exception: Cannot instrument class javafx.scene.control.TextField because it or one of its supertypes could not be initialized`. Every `Control` subclass runs `Control.<clinit>`, which calls `PlatformImpl.setDefaultPlatformUserAgentStylesheet` and needs a running toolkit. Measured on this machine (JDK 25, JavaFX 27, `DISPLAY=:0`):

  | approach | result |
  |---|---|
  | `mock(Region.class)` | works headless — `ViewLoaderUnitTest` already relies on this |
  | `mock(TextField.class)`, toolkit down | fails: supertypes could not be initialized |
  | `Platform.startup()` with `DISPLAY=:0` | works; then both real and mocked controls work |
  | `Platform.startup()` without `DISPLAY` | fails: `Unable to open DISPLAY` |

  **Resolved:** test strategy split into (a) a pure collaborator carrying all post-create logic as data, testable with no toolkit and no display, and (b) one toolkit-backed `*IntegrationTest` loading the real `vault.fxml` to prove controller wiring. CI's test step gains `xvfb-run`. Approach chosen by the user. The design's original D5 (drop the `@FXML` inline initializers) was dropped entirely: it only existed to enable mocked-control tests, which are impossible here, so the initializers stay.

## Consistency

- [x] **`proposal.md` did not promise two things the design and tasks depend on.** Design D4 (sort matched names case-insensitively) and D5 (drop the redundant `@FXML` inline initializers) appear nowhere in the proposal's *What Changes* or *Impact*, yet tasks scheduled them. **Resolved:** D4 (sorting) added to the proposal; D5 dropped from the design instead of being promised, since its only purpose was enabling mocked-control tests.

- [x] **`specs/vault-records/spec.md` stated the same rule twice.** "Records whose names merely contain the created name stay visible" (with its scenario "Containing names are not filtered away") restates the scenario "Only records matching the created name stay visible" in "Created record becomes the visible, selected result". Two normative requirements for one behavior risks them drifting apart at archive time. **Resolved:** merged into a single requirement.

- [x] **`proposal.md` *Impact* still said "the controller itself stays untested by construction".** True for the original plan, wrong once tests exist. **Resolved:** rewritten to describe the two-tier test strategy.

## Verification gaps

- [x] **A task's verification was environment-dependent and unverified.** It pointed at `./mvnw javafx:run`, which needs a display (this machine has `DISPLAY=:0`; a headless server has none — `AGENTS.md` already notes Xvfb for this reason). An app run also does not prove the FXML bindings survived. **Resolved:** verification replaced by the toolkit-backed `vault.fxml` load assertion in task 2.1, with the human smoke test kept separate as task 4.3.

- [x] **CI has no display, so the new integration test would fail the build.** `.github/workflows/maven.yml` runs `./mvnw -B package` on `ubuntu-latest` with no X server. **Resolved:** task added to wrap the CI test step in `xvfb-run` (GitHub-hosted Ubuntu images ship Xvfb — to be confirmed when the task lands).

## Notes

- `review` is not an artifact of schema `spec-driven` (`openspec instructions review` returns "Artifact 'review' not found"). This file is a house convention — it follows the format of `openspec/changes/archive/2026-09-30-modernize-java-stack/review.md` and is ignored by `openspec status` and `openspec validate`.
- `AGENTS.md` is stale: it says Java 11 is pinned in `pom.xml` (now 25) and that there are no active tests. Out of scope here; worth a docs follow-up.
- Pre-existing DDD finding from the archived review still open: `VaultController.create/save/delete` hold record validation in the presentation layer. This change deliberately does not move it (proposal *Non-goal*); track separately as `extract-domain-model-from-presentation`.
- Pre-existing: `Vault` is an `HashMap` subclass leaking the full `Map` API. The new collaborator takes the name set as an input rather than a `Vault`, so it stays neutral to that and does not deepen the coupling.
- Design D1 places `RecordSearch` in `domains/vault/`. User-entered filter text is arguably application-layer query behavior; acceptable in this project since no application layer exists, and it is an improvement over filtering inside a JavaFX controller. No action.

## Second pass — independent reviews, 2026-10-03

Two reviewers who had not written the artifacts read them against the source (architecture review of the design; coherence review of proposal/spec/design/tasks). Findings below; all resolved by editing artifacts in this change. Their first-pass resolutions were re-checked and confirmed real.

### Blocking

- [x] **Rejected-creation task would hang the suite.** `create()` with an empty name or password calls `warning()` → `alert.showAndWait()` (`VaultController.java:139,173-180`), which blocks until a human clicks. Off the FX thread it throws instead. The same applies to `save()` and `delete()`, which open confirmation dialogs inline (`VaultController.java:161,199`) — so the "Editing and deleting do not rewrite the Search box" requirement could not be tested either. **Resolved:** new decision D6 (protected `warning(...)`, extracted protected `confirm(...)`), task 2.1, spec wording keeps the "a warning is shown" clause now assertable.

- [x] **The integration test would destroy the developer's real vault.** Loading the controller through the normal composition root wires a real `VaultLoader`, whose `save` truncates `~/jpassvault/jpassdb.xdb` *before* encryption (`VaultLoader.java:52-61`). **Resolved:** new decision D7 — in-memory `Vault` plus a stub `VaultLoader` recording the call, never the default loader (task 2.2).

### Major

- [x] **Sorting promised but not specified.** `proposal.md` promised alphabetical order; the spec had no ordering requirement, so the promise would vanish at archive time. **Resolved:** ordering sentence and scenario added to "Search filters records by name substring"; D4 now explains the spec carries it.

- [x] **Requirement with no task.** "Editing and deleting do not rewrite the Search box" had neither a task nor a design decision. **Resolved:** covered by task 2.7 once D6 makes the dialogs reachable (and by smoke task 4.3).

### Minor

- [x] **FX-thread rule unstated** — assertions must run on the JavaFX Application Thread. → D5 states it; risks list it.
- [x] **JavaFX-import ban for the collaborator unstated** — one `import javafx` would silently kill every headless test. → D1 states it.
- [x] **Null filter contract unspecified.** → D1: null treated as empty; task 1.2 tests it.
- [x] **Proposal promised the record view stays untouched on rejection; spec and task did not.** → spec requirement and both rejection scenarios now include the record view; task 2.7 asserts it.
- [x] **Design claimed the integration test proves the `onKeyTyped` wiring, but no scenario typed anything.** → spec gained a "Typing refreshes the list" scenario; typing moved to smoke test 4.3 (pre-existing behavior, event synthesis not worth asserting); design claim dropped.
- [x] **Click path (D2) had no test** — only `compile` verified it. → click case added in task 2.4.
- [x] **Duplicate-name task weaker than its scenario** — it asserted only "one selected record", not the new password. → task 2.7 asserts the stored password.
- [x] **TDD order** — four tasks were test-after-code. → group 1 now writes all four tests red before implementation (1.5); group 2 orders seam → red test → extraction → behavior.
- [x] **"test step" did not exist in `maven.yml`** — the workflow has one build step. → renamed to build step in proposal, design and tasks.
- [x] **Test package unspecified**; `create()`/`updateSelector()` are package-private, so the root-package test cannot see them. → packages stated in tasks 1.1 and 2.2.
- [x] **`ViewLoader.load` does not expose the controller.** → task 2.2 configures `FXMLLoader` itself the way `ViewLoader.loadRoot` does.

### Nits

- [x] "the only place the view pane gets populated" (`design.md` Context) — false: the view pane fields are editable and the user types into them. → reworded to loading a stored record.
- [x] "inline initializers" plural — only `listVault` has one. → D8 singular.
- [x] D3's save-failure rationale was vacuous: `VaultLoader.save` swallows all exceptions (`VaultLoader.java:63-66`), so no failure is observable. → D3 says so plainly.
- [x] `CHANGELOG.md` already has `## [Unreleased]`; task said "add an entry". → task 4.1 says add bullets under the existing heading.
- [x] `Platform.startup` is once-per-JVM — a future second toolkit test would throw. → D5 guard note.
- [ ] Open (not this change): `openspec/specs/desktop-runtime/spec.md` says CI "runs `mvn -B package`"; prefixing with `xvfb-run` makes that scenario wording imprecise. Semantically still true; confirm at archive time.

### Confirmed sound

Independently verified against source: `create()` order (`VaultController.java:137-151`), `updateSelector()` live-read, `clear()`/`addAll()` and `HashSet` order (`113-121`), `onKeyTyped="#search"` (`vault.fxml:37`), view-pane editability, `save` swallowing exceptions, ~247 lines, D3 ordering being correct given `clear()`/`addAll()`, no layering violation in placing the collaborator in `domains/vault/`, no overlap with `desktop-runtime`, and a proportionate change overall (one collaborator, two test classes, one CI line, zero new dependencies).

## Verification commands used

- `openspec validate "prefill-search-after-record-create" --strict` → valid
- `openspec status --change "prefill-search-after-record-create"` → 4/4 artifacts
- `./mvnw -B test` → existing suite green
- Headless probes (`Probe`, `Probe2`, `Tool`) against `dependency:build-classpath`, in `/tmp/opencode/mocktest`, not committed
