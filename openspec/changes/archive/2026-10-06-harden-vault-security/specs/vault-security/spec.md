# Spec Delta

## Purpose

Protects the stored secrets of the vault: unique-IV encryption with a strong salted key derivation and a versioned file format that old files migrate into, HTTPS-only sync with a keyed integrity checksum, owner-only file permissions, and short-lived secrets while the app runs (session on lock, clipboard copy, token in diagnostics).

## ADDED Requirements

### Requirement: Every encryption uses a fresh unpredictable IV

Each vault encryption SHALL use a new, randomly generated 12-byte initialization vector that is stored with the ciphertext so decryption remains possible. Encrypting the same content twice SHALL produce different output. The system SHALL NOT reuse a fixed or predictable IV for any two encryptions under the same key.

#### Scenario: Two saves of identical content differ

- **WHEN** the same vault content is encrypted twice
- **THEN** the two ciphertexts differ

#### Scenario: Stored ciphertext decrypts with its embedded IV

- **WHEN** a saved vault file is loaded and unlocked with the correct password
- **THEN** the content decrypts successfully because the IV stored with the ciphertext was used

### Requirement: Key derivation uses a salted iterated KDF

The encryption key SHALL be derived from the master password with PBKDF2-HMAC-SHA256 using at least 600 000 iterations and a random 16-byte salt that is unique per vault and stored with the vault data. The stored data SHALL carry the KDF parameters and a format version so a later version can change them without breaking older files. The system SHALL NOT derive the key with a single hash round. Stored parameters and the format version SHALL be validated and bounded before use: an iteration count outside the accepted range, a salt or IV of the wrong length, or an unknown format version SHALL be rejected cleanly rather than driving derivation.

#### Scenario: Two vaults use different salts

- **WHEN** two different vaults are created with the same master password
- **THEN** they store different salts, so their derived keys differ

#### Scenario: Wrong password fails to unlock

- **WHEN** a user enters a password that is not the vault password
- **THEN** unlocking fails and no vault content is revealed

#### Scenario: Stored KDF parameters drive derivation

- **WHEN** a vault file is unlocked
- **THEN** the iteration count and salt read from the stored data are used, so files saved with older parameters remain unlockable

#### Scenario: Absurd or malformed stored parameters are rejected

- **WHEN** a vault body carries an iteration count far outside the accepted range, a wrong-length salt or IV, or an unknown format version
- **THEN** unlocking fails cleanly without hanging and no vault content is revealed

### Requirement: Existing vault files remain openable through migration

A vault file created by an earlier application version SHALL open with its existing password. After the first successful unlock and save, the file SHALL be stored in the current format (version tag, salt, iterated KDF, random IV). A wrong password on an old-format file SHALL be rejected exactly like on a current-format file, with no data rewritten.

#### Scenario: Old-format vault opens after upgrade

- **WHEN** a user upgrades the application and unlocks a vault file written by a previous version with the correct password
- **THEN** the vault opens and the records are intact

#### Scenario: First save upgrades the file format

- **WHEN** an old-format vault is unlocked and a record is saved
- **THEN** the stored file carries the current format with a salt and a random IV

#### Scenario: Wrong password does not rewrite an old-format file

- **WHEN** a user enters a wrong password for an old-format vault file
- **THEN** unlocking fails and the file on disk is unchanged

### Requirement: Sync change detection uses a keyed deterministic checksum

The value used to decide whether the local and remote vaults differ SHALL be deterministic for unchanged content (so repeated checks of the same state match) and SHALL be computed with a key derived from the master password, not from a reused IV or an unkeyed hash of the content. A change to any record SHALL change the value. The unkeyed MD5 of the ciphertext SHALL NOT be used.

#### Scenario: Unchanged vault yields a stable value

- **WHEN** the vault content has not changed and the checksum is computed again with the same password
- **THEN** the value is identical to the previous one

#### Scenario: Changing a record changes the value

- **WHEN** a record is added, edited, or deleted
- **THEN** the computed value differs from the previous one

#### Scenario: Remote and local comparison keeps working

- **WHEN** sync compares the value computed locally with the value stored by the server
- **THEN** equal vault states compare as equal and differing states trigger the existing merge/upload flow

### Requirement: Sync endpoints require HTTPS

The system SHALL accept a sync API URL only when its scheme is `https`, with `http` permitted solely for `localhost` and `127.0.0.1`. Validation SHALL happen when the URL is configured and again before any request is sent, so a hand-edited settings file cannot downgrade a request. A rejected URL SHALL be reported to the user and no request SHALL be sent to it.

#### Scenario: Plain http URL is rejected when configured

- **WHEN** a user enters `http://sync.example.com` as the API URL
- **THEN** the configuration is rejected with a clear message and no sync request is sent to it

#### Scenario: https URL works as before

- **WHEN** the API URL is `https://sync.example.com`
- **THEN** the URL is accepted and sync behaves as before

#### Scenario: localhost http stays usable for development

- **WHEN** the API URL is `http://localhost:8080`
- **THEN** the URL is accepted

#### Scenario: Hand-edited settings cannot downgrade a request

- **WHEN** the settings file contains a plain `http` URL for a non-local host and sync runs
- **THEN** the request is refused before it is sent

### Requirement: Vault and settings files are owner-only

The vault file, its backup, and the settings file SHALL be created with permissions that grant access only to the owning user (`0600`) on systems that support POSIX permissions, and SHALL be owner-only from the moment they exist, with no window in which they are group- or world-readable. A write SHALL replace the file atomically so that a failed write leaves the previous file intact. The vault directory SHALL be owner-only (`0700`) on such systems. On systems without POSIX permissions the application SHALL still write the files successfully.

#### Scenario: Created vault file is owner-only

- **WHEN** the application creates the vault file on a POSIX system
- **THEN** the file permissions are `0600`

#### Scenario: Settings file with the API token is owner-only

- **WHEN** the application writes the settings file containing the API token on a POSIX system
- **THEN** the file permissions are `0600`

#### Scenario: Owner-only from creation, with no readable window

- **WHEN** the application creates or rewrites a vault or settings file on a POSIX system
- **THEN** the file is never observable with group or world read access during the write

#### Scenario: A failed save preserves the previous file

- **WHEN** a save fails before the new content is complete
- **THEN** the previous file on disk is unchanged

#### Scenario: Vault directory is owner-only

- **WHEN** the application creates the vault directory on a POSIX system
- **THEN** the directory permissions are `0700`

### Requirement: Session and derived keys are cleared on lock

When the vault is locked, the master password and any key material derived from it (AES key, HMAC key, salt) SHALL be removed from the application session and from the components that hold them. After locking, no operation SHALL be able to decrypt or checksum the vault until the user unlocks again.

#### Scenario: Lock clears the retained password

- **WHEN** the user locks a vault that was unlocked
- **THEN** the session no longer holds the master password

#### Scenario: Lock removes derived key material

- **WHEN** the user locks an unlocked vault
- **THEN** a later checksum or decrypt attempt without re-unlocking fails and requires the master password again

#### Scenario: Re-unlock requires the password again

- **WHEN** a user locks and then returns to the unlock screen
- **THEN** opening the vault requires entering the master password again

### Requirement: Minimum master password length at creation

Creating a vault SHALL require a master password of at least 8 characters and SHALL reject shorter ones with a warning and no state change. Unlocking an existing vault SHALL accept any password that decrypts it, regardless of length.

#### Scenario: Seven-character password is rejected at creation

- **WHEN** a user tries to create a vault with a 7-character password
- **THEN** a warning is shown, no vault is created, and the setup screen is unchanged

#### Scenario: Eight-character password is accepted

- **WHEN** a user creates a vault with an 8-character password
- **THEN** the vault is created

#### Scenario: Short existing vault password still unlocks

- **WHEN** a user upgrades from a version that allowed shorter passwords and unlocks that vault with its original password
- **THEN** the vault unlocks normally

### Requirement: Copied password is cleared from the clipboard

After a password is copied to the system clipboard, the application SHALL remove it from the clipboard within 30 seconds, and SHALL also clear it when the vault locks or when the application exits. The application SHALL NOT remove clipboard content it did not place, and SHALL NOT keep the copied value in memory after clearing.

#### Scenario: Clipboard is cleared after the timeout

- **WHEN** a user copies a record password and does nothing else with the clipboard
- **THEN** the clipboard no longer contains that password within 30 seconds

#### Scenario: Lock clears a pending copied password

- **WHEN** a copied password is still on the clipboard and the vault is locked
- **THEN** the clipboard no longer contains that password

#### Scenario: Another application's clipboard content is preserved

- **WHEN** the clipboard holds content that the application did not place there
- **THEN** the application does not clear or modify it

### Requirement: Secrets are not exposed in diagnostic output

Diagnostic or string representations produced by the application SHALL NOT contain the API token or any vault record secret; the token SHALL appear only masked, and record passwords SHALL NOT be rendered at all.

#### Scenario: Token is masked in string output

- **WHEN** the application settings are rendered as diagnostic text
- **THEN** the API token appears masked rather than as its plain value

#### Scenario: Record secrets are not printed

- **WHEN** a vault record is rendered as diagnostic text or an exception message
- **THEN** its password does not appear

### Requirement: Malformed sync responses do not break unlocking

A sync response that cannot be interpreted (for example a checksum payload with no hash value) SHALL be treated as a sync failure. It SHALL NOT propagate an unchecked error into the unlock flow or prevent the vault interface from opening.

#### Scenario: Malformed checksum payload is contained

- **WHEN** the sync server returns a response that cannot be interpreted while unlocking
- **THEN** sync reports a failure and the local vault still opens with the correct password
