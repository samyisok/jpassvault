# Design

## Context

See proposal.md - Why.

Constraints that shape the approach, all observed in the current code:

- `VaultController.create()` (`src/main/java/com/samyisok/jpassvaultclient/controllers/VaultController.java:137`) currently does: validate → `vault.put` → `updateSelector()` → `vaultLoader.save(vault)` → clear the three create fields. It never touches `searchViewByName` or the list selection.
- `updateSelector()` (`VaultController.java:113`) filters `vault.keySet()` with `el.toLowerCase().contains(searchViewByName.getText().toLowerCase())` and rebuilds `listVault`'s items from a `HashSet` collected into an `ObservableList`, in two steps: `clear()` then `addAll()`. It reads the Search box text live, so the order of "set text" and "refresh list" is load-bearing, and selection must be applied after the rebuild.
- `searchViewByName` is wired to `onKeyTyped="#search"` in `vault.fxml:37`. A programmatic `setText` does not fire `onKeyTyped`, so the create path must refresh the list itself.
- `onClick()` (`VaultController.java:123`) is the only place a stored record is loaded into the view pane. The view pane's fields are themselves editable (`vault.fxml:67-81`), so this does not stop the user typing there.
- Three failure paths raise modal dialogs that block: `create()` calls `warning()` → `alert.showAndWait()` (`VaultController.java:139,173`), `save()` and `delete()` each open a confirmation dialog (`VaultController.java:161,199`). A test that reaches any of them waits for a human click that will never come.
- Writing to the vault is real filesystem I/O: `VaultLoader.save` truncates `options.getFullPathVaultOrDefault()` — by default `~/jpassvault/jpassdb.xdb` — before encryption runs (`VaultLoader.java:52-61`). A test using the normal composition root overwrites a developer's real vault.
- `ViewLoader.load` does not expose the controller it builds; `getController()` is only available through a `FXMLLoader` configured the way `ViewLoader.loadRoot` configures it (`ViewLoader.java:137-145`).
- Instantiating any JavaFX `Control` headless throws from `Control.<clinit>`, and Mockito cannot instrument them either (`Cannot instrument class javafx.scene.control.TextField because it or one of its supertypes could not be initialized`). Booting the toolkit works only with a display: `Platform.startup()` succeeds under `DISPLAY=:0` on this machine and fails with `Unable to open DISPLAY` otherwise. The only existing test (`ViewLoaderUnitTest`) tests static helpers over mocked `Region`s, which works headless precisely because `Region` is not a `Control`.
- Project rules in `AGENTS.md`: TDD, unit tests ≤10 lines, Mockito, ≤350 lines per Java file, `*UnitTest` / `*IntegrationTest` suffixes.

## Goals / Non-Goals

**Goals:**

- New after-create behavior covered by headless unit tests for the decisions, plus one toolkit-backed test for the wiring, following the project's TDD rule.
- Tests never touch a real vault file or wait on a human click.
- One place that decides which records match a filter, reused by the create path, the search path, and the edit/delete refreshes.
- One place that loads a record into the view pane, reused by mouse click and by the create path.
- No new library dependency, no FXML change, no change to stored data. The only build change is giving CI a display for the integration test.

**Non-Goals:**

- Changing what the Search box matches (stays a case-insensitive substring match on name), or matching on login as well.
- Focus handling, notifications, or any other feedback for the create action.
- Reworking `updateSelector()`'s signature or the `EventPublisher` flow.
- Monocle or TestFX: no new test dependency. A display (this machine's X or CI's Xvfb) is used instead.
- Asserting rendered pixels, window geometry, or screenshot comparisons.

## Decisions

**D1 - Put the post-create screen state in a headless domain collaborator, not in the controller.**
Introduce a small type under `domains/vault/` (for example `RecordSearch`) that takes the vault's names and a filter string and returns the matching names, plus a factory that, given the created record name, returns the full state to display: the filter text to show, the visible names, and the name to select. `VaultController` applies that state; `updateSelector()` calls the same collaborator for filtering. A null filter is treated as empty. The collaborator must not import JavaFX — that import is what would silently break every headless unit test.

Rationale: no test can reach a `Control` without a display (Context) - not by instantiation, and not through Mockito, which refuses to instrument `TextField` because "one of its supertypes could not be initialized". Moving the decision out of the controller makes every spec scenario a plain unit test with no toolkit, no display and no mocks, matching `ViewLoaderUnitTest`'s existing style of testing logic instead of widgets. Encoding the post-create decision as data rather than as a controller sequence also means the "set filter text before refreshing" ordering (D3) cannot drift: the controller applies fields of a value object in one step.

Placement in `domains/vault/` is deliberate: no application layer exists in this project, the package already carries mixed concerns (`VaultLoader` does file I/O), and the collaborator is pure. It is GRASP Pure Fabrication with Information Expert for the match rule, and it takes the name set rather than a `Vault`, so it does not deepen coupling to `Vault`'s open `Map` API.

Alternatives considered: (a) mock `TextField`/`ListView` with Mockito - measured to fail headless (same `Control.<clinit>` path). (b) add Monocle and boot a real toolkit in unit tests - adds a test-only dependency whose JavaFX 27 compatibility is unverified. (c) leave the logic in the controller and test nothing - the ordering risk in D3 would rest on review alone.

**D2 - Extract the "load record into view" step and call it from both the click and create paths.**
Give the controller one private method that, given a record name, fills `nameView`/`loginView`/`passwordView`. `onClick()` calls it with the current selection; `create()` calls it with the name just created.

Rationale: the same three assignments would otherwise be duplicated (DRY, and one place that knows how to show a record - GRASP Information Expert). Behavior on click is unchanged, and the integration test asserts both paths so the extraction cannot regress click.

**D3 - Order inside `create()`: put → set Search text → refresh list → select → clear create fields, with `vaultLoader.save(vault)` left where it is today (after the refresh).**

Rationale: `updateSelector()` reads the Search box live, so setting the text first is what makes the new record visible under a stale filter; selecting before the refresh would be lost when `listVault`'s items are cleared and rebuilt. Leaving `save` where it is means this change alters nothing about when data is written. (This is weaker than it first looks: `VaultLoader.save` swallows every exception at `VaultLoader.java:63-66`, so a save failure cannot surface to the user at all — the ordering is kept for behavioral sameness, not because a failure is observable.)

**D4 - Sort the filtered names case-insensitively before filling the list.**
Today the names come out of a `HashSet`, so list order is arbitrary and can visibly reshuffle between refreshes.

Rationale: a stable alphabetical order makes the created record's position predictable and the UI less jumpy. Because this is now promised in the proposal, the ordering is stated in the `vault-records` spec requirement so it survives archiving rather than being an implementation detail that disappears.

**D5 - Prove the wiring with one toolkit-backed test that loads the real `vault.fxml`, and give CI a display.**
One `*IntegrationTest` boots the JavaFX toolkit once per JVM (static setup, guarded so a second toolkit test in the suite fails loudly rather than twice: `Platform.startup` throws if called again; never call `Platform.exit()` from a test, and keep `Platform.setImplicitExit(false)` on), creates a `FXMLLoader` for `vault.fxml` configured with the same controller factory `ViewLoader.loadRoot` uses — `ViewLoader.load` does not expose the controller, so the test configures the loader itself — and reaches the controller through `loader.getController()`. Every assertion about a control runs on the JavaFX Application Thread (`Platform.runLater` plus a latch), because `setText`/`select` off-thread is unsafe and `showAndWait` off-thread throws.

The test asserts the create flow: enter name and password, call `create()`, then assert the Search box holds the name, the list shows the match, the row is selected, and the view pane carries the login and password; the click path is asserted the same way to cover D2. CI runs the build step under `xvfb-run` so `ubuntu-latest` has the display it needs; this machine already has `DISPLAY=:0`.

Rationale: the ordering in D3 and the field-level wiring in D2 are exactly what unit tests cannot see (Context), and an integration test over the real FXML also proves the `fx:id` bindings survive the change — something no amount of collaborator tests can show.

Alternatives considered: (a) Mockito-mocked controls - impossible headless, and pointless once a display is available since the real controls are there. (b) no wiring test at all - leaves D2/D3 unverified. (c) TestFX robot driving a window - heavier dependency and the same display requirement, for no extra confidence about this flow.

**D6 - Make the dialog calls overridable so tests never block on a human.**
Extract the two dialog shapes already in `VaultController` behind protected methods with identical behavior: `warning(header, message)` (already a method — change it from package-private to `protected`) and a new `confirm(title, content)` returning `boolean` that wraps the `Confirmation` alert `save()` and `delete()` open inline today. The integration test supplies a controller through the factory that overrides both, recording messages instead of showing modals.

Rationale: without this, the rejected-creation, save and delete scenarios hang — `showAndWait()` enters a nested event loop that waits for a click nobody makes, and the suite deadlocks. The seam is a two-method visibility/extract change with no behavior difference in production, versus auto-closing dialogs from test code (fragile, races with `setOnShown`) or covering only the happy path (leaves three spec scenarios untested).

Alternatives considered: (a) auto-dismiss via `alert.setOnShown` / `Platform.runLater` closing the first window - works but races and needs global stage handling. (b) skip the dialog scenarios in tests and cover them only by manual smoke test - leaves the spec's "a warning is shown" clauses unverified. (c) extract a dialog interface/port - more abstraction than two methods need.

**D7 - Isolate the test from the real vault file, and change nothing else about persistence.**
The integration test never uses the default composition root. It constructs the controller with an in-memory `Vault` and a stub `VaultLoader` that records `save` calls instead of writing, so `~/jpassvault/jpassdb.xdb` is never opened. (The alternative of pointing `Options.setPathVault` at a temp directory still exercises the real file path and the real encryption; the stub is smaller and asserts the save call directly.)

Rationale: `VaultLoader.save` truncates the target file before it encrypts, so a test that goes anywhere near the real loader can destroy a developer's vault — a test that destroys user data is unacceptable regardless of convenience.

**D8 - No FXML, dependency, or data-format changes.**
`searchViewByName` and `listVault` already exist and are already wired, and task 2.1 loads the real FXML, so a broken binding fails the test rather than being assumed away. The `@FXML` fields keep their inline initializer (`listVault` is the only one; the rest are bare) — it is redundant once `FXMLLoader` injects the controls, but removing it buys nothing here and would only change how a missing `fx:id` fails.

## Risks / Trade-offs

- **Test hangs on a modal dialog** → Mitigation: D6 removes every `showAndWait` from the test's path; if a new dialog is added without the seam, the suite deadlocks loudly rather than passing silently.
- **Test overwrites a developer's real vault** → Mitigation: D7 forbids the real loader in tests; the stub asserts `save` was called, so persistence is still covered without I/O.
- **Selecting after rebuilding items is easy to get wrong** (select first, then `clear()`/`addAll()` wipes it) → Mitigation: D3 fixes the order, the collaborator returns the selection as data, and the integration test asserts the selection on a real `ListView`.
- **Programmatic `setText` does not fire `onKeyTyped`, so nothing refreshes the list if the explicit refresh is forgotten** → Mitigation: the create path applies the collaborator's state directly and the integration test asserts the list contents after `create()` returns. Typing-to-refresh itself is pre-existing behavior; it is covered by the smoke test rather than by an event-synthesis assertion in the integration test.
- **Assertions must run on the JavaFX Application Thread** → Mitigation: D5 states the rule; running them off-thread may pass by accident and fail in CI.
- **The integration test needs a display, so it fails anywhere without one** → Mitigation: CI wraps the build step in `xvfb-run`; locally `DISPLAY` must exist (this machine has `:0`), otherwise surefire reports `Unable to open DISPLAY` and the same fallback as `AGENTS.md` documents for `javafx:run` applies.
- **Booting the toolkit in a shared JVM can leak into other tests** → Mitigation: start it once, guarded, keep implicit exit off, never call `Platform.exit()`; the suite has a single other class (`ViewLoaderUnitTest`) that never touches the toolkit.
- **`create()` gains the `protected` dialog methods, so the file grows and the seam is visible in production code** → Mitigation: two methods, no behavior change, and `VaultController` stays well under the 350-line limit (currently ~247); D1/D2 keep new logic out of the controller.
- **Sort order changes what users see versus the current arbitrary order** → Mitigation: order was unstable before, the spec now states it, and the changelog entry calls it out.
- **Pixel-level rendering stays unverified** → Accepted: the integration test asserts model and control state (text, items, selection), not what is on screen; the project has no UI harness for that.
