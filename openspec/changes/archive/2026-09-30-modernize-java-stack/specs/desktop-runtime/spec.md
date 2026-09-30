# Spec Delta

## Purpose

Defines the runtime and platform contract for the jpassvault desktop application: which JDK it runs on, where the UI toolkit comes from, how components are wired without a framework, how the window behaves when resized, and how the app is built and packaged for Linux.

## ADDED Requirements

### Requirement: Resizable window behaviour

The application SHALL open at a default size of 1000x600 pixels and SHALL hold that size until the user resizes the window. The unlock and vault views SHALL grow to fill the window; the setup and options views SHALL remain at their designed size. Individual controls SHALL keep their designed size regardless of window size, and controls in growing views SHALL stay anchored to the window rather than to the view's original position. Changing scenes SHALL NOT change the window size. Any area exposed by a window larger than its view SHALL be filled with the application background colour.

#### Scenario: Default size on launch

- **WHEN** the application starts and the window manager does not resize the window
- **THEN** the window opens at 1000x600 pixels

#### Scenario: Unlock controls stay centred at any size

- **WHEN** the unlock view is shown in a window larger or smaller than 1000x600
- **THEN** the password field and Unlock button remain centred, and the Options button remains an equal distance from the bottom and right edges

#### Scenario: Vault list stretches while form controls hold position

- **WHEN** the vault view is shown in a window larger than 1000x600
- **THEN** the password list fills the additional space while the form fields and buttons keep their designed size and position

#### Scenario: Scene change preserves a user resize

- **WHEN** the user resizes the window and then triggers a scene transition
- **THEN** the window keeps the size the user chose

#### Scenario: Exposed area uses the application background

- **WHEN** the window is larger than the view it displays
- **THEN** the area around the view is filled with the application background colour rather than a default colour

## MODIFIED Requirements

### Requirement: Backward-compatible data and protocol formats

The migration SHALL NOT change user data or external interfaces: the encrypted vault file format (`jpassdb.xdb`), the AES/GCM encryption with SHA3-256-derived keys, the `config.json` settings file, and the remote sync HTTP protocol SHALL remain byte- and behavior-compatible with the previous version.

This requirement rests on the crypto, serialization and protocol code being left untouched by the migration, not on an executed test: no vault file created by the pre-migration version was available to verify against, so cross-version loading is unproven by test.

#### Scenario: Existing vault opens after migration

- **WHEN** a user inspects a vault file and settings file created by the previous version
- **THEN** the file format, AES/GCM parameters, and SHA3-256 key derivation are identical, so the existing data loads without migration

#### Scenario: Remote sync interoperates with existing server

- **WHEN** online sync is enabled with a previously configured API URL and token
- **THEN** checksum checks, downloads, uploads, and merges behave identically to the previous version
