# vault-records Specification

## Purpose

Defines how the vault screen lists and finds password records: what the Search box matches, and what the screen shows the moment a record is created so the user can see and copy it without repeating work.

## Requirements

### Requirement: Search filters records by name substring

The vault screen SHALL treat the Search box text as a case-insensitive substring filter over record names. An empty Search box SHALL show every record. Typing in the Search box SHALL refresh the visible records as the user types. The visible records SHALL be listed in case-insensitive alphabetical order so the list does not reorder itself between refreshes.

#### Scenario: Partial name match ignores case

- **WHEN** the vault holds records named "GitHub", "github backup" and "Bank" and the user types "github"
- **THEN** the list shows "GitHub" and "github backup" and hides "Bank"

#### Scenario: Empty search shows every record

- **WHEN** the Search box is empty
- **THEN** every record in the vault is visible in the list

#### Scenario: Search that matches nothing shows an empty list

- **WHEN** the user types text that matches no record name
- **THEN** the list is empty

#### Scenario: Typing refreshes the list

- **WHEN** the user types a character into the Search box
- **THEN** the visible records are refreshed to match the text typed so far

#### Scenario: Visible records are listed alphabetically

- **WHEN** the vault holds records named "bank", "GitHub" and "apple"
- **THEN** the list shows them in the order "apple", "bank", "GitHub"

### Requirement: Created record becomes the visible, selected result

After a record is created successfully, the vault screen SHALL put the created record's name into the Search box, replacing any text that was there, and SHALL refresh the list using that text. The created record SHALL be selected, and its login and password SHALL be displayed in the record view.

#### Scenario: Search box is filled with the created record's name

- **WHEN** a user creates a record named "GitHub"
- **THEN** the Search box contains "GitHub"

#### Scenario: A stale filter no longer hides the new record

- **WHEN** the Search box contained "Bank" and a user creates a record named "GitHub"
- **THEN** the Search box contains "GitHub" instead of "Bank" and "GitHub" is visible in the list

#### Scenario: Created record is selected and shown

- **WHEN** a user creates a record named "GitHub" with a login and a password
- **THEN** that record is selected in the list and the record view shows the login and password just entered

#### Scenario: Only records matching the created name stay visible

- **WHEN** a user creates a record named "GitHub" while records named "GitHub backup" and "Bank" already exist
- **THEN** the list shows "GitHub" and "GitHub backup" and hides "Bank"

#### Scenario: Containing names are not filtered away

- **WHEN** a user creates a record named "git" while a record named "GitHub" already exists
- **THEN** the list shows "GitHub" alongside "git", because the filter is still a substring match

### Requirement: Rejected creation leaves the screen untouched

When a creation is rejected because the name or the password is empty, or because a record with that name already exists, the vault screen SHALL NOT store the submitted login or password, SHALL NOT change the Search box text, the visible records, the selected record, or the record view, and SHALL show a warning explaining the rejection.

#### Scenario: Empty name rejects creation without touching the screen

- **WHEN** the user submits a creation with an empty name
- **THEN** a warning is shown and the Search box, the visible records, the selected record and the record view are unchanged

#### Scenario: Empty password rejects creation without touching the screen

- **WHEN** the user submits a creation with an empty password
- **THEN** a warning is shown and the Search box, the visible records, the selected record and the record view are unchanged

#### Scenario: Duplicate name warns and keeps the stored record

- **WHEN** a record named "GitHub" holds password "old-password" and the user creates a record also named "GitHub" with password "new-password"
- **THEN** a warning is shown, the stored "GitHub" still holds "old-password", and the Search box, the visible records, the selected record and the record view are unchanged

### Requirement: Editing and deleting do not rewrite the Search box

Saving changes to an existing record and deleting a record SHALL NOT change the Search box text. The visible records SHALL be refreshed after either action, using the Search box text that is already there.

#### Scenario: Saving a record keeps the current search

- **WHEN** the Search box contains "Git" and the user saves changes to a record named "GitHub"
- **THEN** the Search box still contains "Git" and the visible records are re-filtered by it

#### Scenario: Deleting a record keeps the current search

- **WHEN** the Search box contains "Git" and the user deletes a record
- **THEN** the Search box still contains "Git" and the deleted record is no longer visible
