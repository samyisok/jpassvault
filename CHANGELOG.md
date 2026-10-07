# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.1.0] - 2026-10-08

### Added

- The app now ships with its own typeface and uses it on every screen, so the interface looks the same on Linux, Windows and macOS without depending on fonts installed on the computer.

### Changed

- Buttons, text fields and the record list now share that one bundled font family instead of asking for a Windows-only font that other systems silently replaced with something different. Font and text sizing are now defined in one place, so the unlock, setup, options and vault screens stay consistent.

### Fixed

- The Options screen shows its white form background again, so the settings area stands out clearly instead of blending into the rest of the window.

## [3.0.0] - 2026-10-06

### Security

- Every save now uses a fresh random initialization vector, so two copies of the vault can no longer be compared to work out what changed.
- The master key is now derived with a slow, salted PBKDF2-HMAC-SHA256 derivation (600 000 iterations) instead of a single SHA3-256 hash, making offline password guessing far more expensive.
- Vault files and sync responses carrying absurd or malformed encryption parameters are rejected cleanly instead of freezing the app.
- Sync URLs must use `https` (plain `http` is allowed only for `localhost` and `127.0.0.1`); an insecure URL is refused when saved and never contacted. **Breaking:** existing `http://` sync settings must move to `https`.
- The vault file, its backup and the settings file are owner-only (`0600`) from the moment they are created and are written atomically, so a failed save never truncates the existing file; the vault folder is owner-only too.
- Locking the vault now clears the master password and everything derived from it, and clears a copied password from the clipboard; a copied password is also cleared after 30 seconds and when the app exits.
- The sync API token no longer appears in diagnostic text (it is shown masked), and stored record passwords are never printed.
- Sync change detection now uses a keyed HMAC-SHA256 value instead of an unkeyed MD5 of the ciphertext.

### Added

- New vaults must use a master password of at least 8 characters. Unlocking an existing vault still accepts its original password, whatever its length.

### Changed

- Vault files written by this version use a new versioned format that older app versions cannot read. No data is lost and existing vaults upgrade automatically on first save; rolling back loses only writes made in the new format.
- Under the hood, vault storage now goes through a small storage port and the vault lifecycle is coordinated separately from screen switching; behaviour is unchanged.

### Fixed

- A save that fails while encrypting no longer leaves an empty vault file — the previous file is kept.
- A malformed sync response no longer aborts the unlock flow: it is reported as a sync failure and the local vault still opens.

## [2.2.0] - 2026-10-04

### Added

- Portable downloads: every release now ships unpack-and-run archives for Linux, Windows and macOS with Java built in — no Java to install, just extract and start the vault.
- Release safety: publishing now stops immediately if the version tag and the project version disagree, so a release can never carry files named for the wrong version.

## [2.1.0] - 2026-10-03

### Added

- Creating a record now puts its name into the Search box, refreshes the filtered list and selects the record, so the record you just saved is the one you see — with its login and password shown and ready to copy. A search filter left over from earlier no longer hides the new record.
- A creation rejected for an empty name or empty password leaves the screen exactly as it was: the search text, the list, the selection and the shown record are untouched.

### Changed

- The record list is now sorted alphabetically, ignoring case, instead of coming out in arbitrary order, so it no longer reshuffles while you type.

### Fixed

- Creating a record with a name that already exists no longer silently overwrites the stored record; a warning explains and the old login and password are kept.

## [2.0.0] - 2026-09-30

### Added

- Resizable window: the app now opens at a fixed 1000x600 and stays that size until you resize it yourself. Switching between the unlock, vault, settings and setup screens no longer resizes the window out from under you.
- The vault screen's list of passwords fills the extra space when you enlarge the window, while the form fields and buttons keep their designed size and position.
- The unlock screen's password field and Unlock button stay centred at any window size, and the Options button sits an equal distance from the bottom and right edges.

### Fixed

- Fixed a crash on first launch and on every unlock: the app checked for an API address and token that had never been set, instead of treating "not configured" as normal. A fresh install now starts cleanly.
- Fixed the window stretching its contents across the whole screen instead of showing the app at its intended size.
- Fixed the area around the app showing a default colour when the window is larger than the screen content, so it now blends with the app's own background.

### Changed

- Migrated from Java 11 and Spring Boot 2.4.5 to Java 25 LTS with JavaFX 27, dropping the unsupported dependency and shrinking the dependency tree.
- The build now produces a single runnable jar for Linux containing the app and everything it needs, launched with `java -jar`.
- Continuous integration builds on Java 25.

### Removed

- Spring Boot and the FXML controller framework it pulled in. The app now wires its own components at startup and needs no application server or framework at runtime.
- The window resizing itself back to a fixed size whenever you moved between screens.
