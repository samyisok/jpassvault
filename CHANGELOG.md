# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
