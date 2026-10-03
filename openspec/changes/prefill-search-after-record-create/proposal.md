# Proposal

## Why

After a record is created, the vault screen gives no feedback about where the new record went. If a search filter is already active, the freshly created record is filtered out of the list entirely; with an empty filter the record lands somewhere in an unsorted list, unselected, with the right-hand view pane still showing whatever was there before. The user then has to retype the record name into Search and click the row before they can see or copy the password they just set. Prefilling Search with the created record's name and selecting that record makes the result of the action immediately visible and usable.

## What Changes

- On successful record creation, the Search field is set to the created record's name, replacing whatever filter text was there.
- The filtered list is refreshed after that text is set, so the new record is the visible match rather than being hidden by a stale filter.
- The new record is selected in the list, which loads its login and password into the view pane, ready to copy.
- A rejected creation (empty name or empty password) leaves the Search field, the list, and the view pane untouched.
- Creating a record whose name duplicates an existing record keeps today's behavior: the existing record is overwritten and the Search field ends up holding that name.
- Editing an existing record via Save, deleting a record, and the manual Search box are unchanged.
- The filtering rule itself does not change: still a case-insensitive substring match on the record name. Matched records are now listed in case-insensitive alphabetical order instead of an arbitrary hash order, so the list does not reshuffle on every keystroke.
- Tests are added in two tiers: headless unit tests for the matching and post-create decisions, and one toolkit-backed integration test that loads the real `vault.fxml` and proves the wiring. The CI build step gains `xvfb-run` so that integration test has a display.

## Capabilities

### New Capabilities

- `vault-records`: behavior of the vault screen's record list and Search box — how records are filtered, what the screen shows immediately after a record is created, and what remains unchanged for edit and delete.

### Modified Capabilities

None. `desktop-runtime` covers runtime, packaging, and window behavior; nothing in its requirements changes here.

## Impact

- `VaultController.create()`: after a successful `vault.put`, set the Search field text, refresh the filtered list, and select the created record. Currently it calls `updateSelector()` before clearing the create fields and never touches `searchViewByName` or the list selection.
- New headless-testable collaborator under `domains/vault/` holding the post-create screen state — the filter text to show, the names that match it, and the name to select — so every spec scenario is unit-testable without a display. `VaultController.updateSelector()` delegates to the same collaborator for filtering; matching semantics stay identical (case-insensitive `contains`), with added alphabetical ordering.
- `VaultController`: the record-loading step (`onClick()`'s three assignments) is extracted so the click and create paths share it, and `create()` applies the collaborator's post-create state.
- `.github/workflows/maven.yml`: the build step runs under `xvfb-run`, because the new integration test starts a JavaFX toolkit.
- `VaultController`: the modal-dialog calls become overridable — `warning(...)` goes from package-private to `protected`, and the confirmation dialog that `save()` and `delete()` open inline is extracted into a protected `confirm(...)` returning the user's answer. Production behavior is unchanged; without the seam a test blocks forever in `showAndWait()`.
- Tests construct the controller with an in-memory `Vault` and a stub `VaultLoader`, so no test opens or writes the real `~/jpassvault/jpassdb.xdb` (`VaultLoader.save` truncates the file before encrypting).
- `vault.fxml`: unchanged — `searchViewByName` and `listVault` already exist and are already wired.
- Untouched: `Vault`, `VaultContainer`, `VaultLoader`, the encrypted vault file format (`jpassdb.xdb`), crypto, `config.json`, remote sync, CSS, and the other FXML views.
- Tests: new unit tests for the collaborator (no toolkit, no display, run anywhere) plus one `*IntegrationTest` that starts a toolkit and asserts the create flow against real controls. Measured constraint that shaped this split: instantiating a JavaFX `Control` headless throws from `Control.<clinit>`, and Mockito cannot instrument `TextField` either ("it or one of its supertypes could not be initialized"), so without a display no test can reach a control — hence the logic lives in the collaborator.
