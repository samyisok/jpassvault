# Spec Delta

## ADDED Requirements

<!-- none: only the existing compatibility requirement is re-framed below -->

## MODIFIED Requirements

### Requirement: Backward-compatible data and protocol formats

Data and protocol changes SHALL preserve compatibility through versioned formats and migration rather than by freezing the format: existing user files (the encrypted vault `jpassdb.xdb` and the `config.json` settings file) SHALL keep opening after an application upgrade, and any change to encryption parameters SHALL be stored behind a format version so older files can be read and rewritten in the current format after a successful unlock. The remote sync HTTP protocol SHALL keep its existing paths, headers, and JSON shapes; the only permitted protocol-side change is requiring an `https` scheme for non-local API URLs. Rolling back to a previous application version may lose writes made in the newer format, but SHALL NOT make the data unreadable in the newer version.

#### Scenario: Existing vault opens after migration

- **WHEN** a user upgrades the application and unlocks a vault file and settings file created by a previous version with the correct password
- **THEN** the files load successfully, the records are intact, and the vault is rewritten in the current format only after a subsequent save

#### Scenario: Remote sync interoperates with existing server

- **WHEN** online sync is enabled with a previously configured `https` API URL and token
- **THEN** checksum checks, downloads, uploads, and merges keep their existing request and response shapes

#### Scenario: Newer-format file is recognized, not garbled

- **WHEN** a previous application version attempts to read a vault file written in the current format
- **THEN** it fails cleanly without corrupting the file, and the newer version still opens it normally
