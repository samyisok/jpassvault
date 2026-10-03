# Tasks

## 1. Headless decision logic

- [x] 1.1 Write failing tests in `src/test/java/com/samyisok/jpassvaultclient/domains/vault/RecordSearchUnitTest.java` (JUnit 5, `@DisplayName` on each, each under 10 lines): a filter matches names containing it regardless of case, and verify they fail because the type does not exist yet
- [x] 1.2 Add failing tests to the same class for the edges: an empty filter returns every name, a filter matching nothing returns no names, and a null filter behaves as empty, and verify they fail
- [x] 1.3 Add a failing test that matched names come back in case-insensitive alphabetical order (design D4), and verify it fails
- [x] 1.4 Add a failing test for the post-create state factory (design D1): given the created name and the vault's names it returns that name as the filter text, the matching names as visible records, and that name as the selection, and verify it fails
- [x] 1.5 Implement `RecordSearch` under `domains/vault/` with no JavaFX import, and verify all four tests pass with `./mvnw -B test -Dtest=RecordSearchUnitTest`

## 2. Controller seam and integration test

- [ ] 2.1 Extract the dialog seam (design D6): make `warning(...)` protected and pull the inline confirmation alerts of `save()` and `delete()` into a protected `confirm(...)` returning the user's answer, and verify `./mvnw -B package` still passes and the dialogs behave identically in a manual run
- [x] 2.2 Write a failing `src/test/java/com/samyisok/jpassvaultclient/controllers/VaultControllerIntegrationTest.java`: boot the JavaFX toolkit once (static setup, `Platform.setImplicitExit(false)`, no `Platform.exit()`), configure a `FXMLLoader` for `vault.fxml` with the controller factory `ViewLoader.loadRoot` uses, supply an in-memory `Vault` plus a stub `VaultLoader` (design D7), override the dialog methods from 2.1, and run all assertions on the JavaFX Application Thread (design D5)
- [x] 2.3 In that test assert the create flow (design D3): enter a name and password, call `create()`, then assert the Search box holds the name, the list shows it, the row is selected and the record view carries the login and password, and verify it fails for the missing behavior
- [x] 2.4 Extract the record-loading step so `onClick()` and `create()` share it (design D2), add a test case that clicking a row still loads its record, and verify `./mvnw -B compile` plus the click case pass
- [x] 2.5 Apply the collaborator's post-create state in `create()` in the order put → set Search text → refresh list → select, leaving `vaultLoader.save()` where it is (design D3), and verify the test from 2.3 passes
- [x] 2.6 Make `updateSelector()` delegate matching and ordering to `RecordSearch` (design D1, D4), and verify both the unit tests and the integration test pass
- [x] 2.7 Add integration test cases: a creation rejected for an empty name or empty password records a warning and leaves the Search box, list, selection and record view unchanged; `save()` keeps the Search text; `delete()` keeps the Search text; a duplicate name leaves one record holding the new password and selected, and verify all pass

## 3. CI display

- [ ] 3.1 Wrap the build step in `.github/workflows/maven.yml` with `xvfb-run` so the integration test has a display (design D5), and verify a push runs the workflow green end to end

## 4. Documentation and full verification

- [x] 4.1 Add bullets under the existing `## [Unreleased]` heading in `CHANGELOG.md` covering the after-create behavior and the now-alphabetical record list, and verify it renders under the existing Keep a Changelog headings
- [x] 4.2 Run `./mvnw -B package` and verify the whole suite is green and the jar builds
- [ ] 4.3 Smoke test the running app: create a record while a stale filter is active, create one with a duplicate name, submit one with an empty name and one with an empty password, type into Search and watch the list refresh, then edit and delete a record — confirm each matches its spec scenario on screen
- [ ] 4.4 Run the DDD, GRASP, tests and openspec verification checks for the diff and record any findings needing follow-up tasks
