# Spec Delta

## Purpose

Defines the visual contract for the jpassvault desktop client: the application bundles and applies its own typeface so every install renders alike, and each stage keeps a distinct, intentional background.

## ADDED Requirements

### Requirement: Bundled application typeface

The application SHALL bundle a single open-source typeface as an application resource and SHALL render all user-interface text in that typeface. The UI SHALL NOT depend on a typeface being installed on the host operating system, and the bundle SHALL be included in the shaded jar and in every packaged application image so jar runs and packaged executables render identically.

#### Scenario: Unlock and vault views render in the bundled typeface

- **WHEN** the application starts and displays the unlock, setup, options, or vault view
- **THEN** every text control and label is rendered using the bundled typeface

#### Scenario: Fresh host without the former font

- **WHEN** the application starts on a host where `Segoe UI` (and any other platform font previously referenced) is not installed
- **THEN** the views render in the bundled typeface instead of a toolkit fallback, with the same text metrics on Linux, Windows, and macOS

#### Scenario: Packaged executable carries the typeface

- **WHEN** a user runs a packaged application image produced by the release pipeline
- **THEN** it renders the same typeface as the shaded jar, because the font resource is packaged with the application

### Requirement: Single source of truth for typography

The UI font family SHALL be defined in one place in the application stylesheets and applied to every view. Views SHALL NOT pin a platform-specific font name per control, and name differences (for example a misspelled font family) SHALL NOT exist.

#### Scenario: Views carry no platform font names

- **WHEN** the FXML view files are inspected
- **THEN** they contain no per-control reference to a platform-specific typeface such as `Segoe UI`, and the vault list stylesheet names the bundled family correctly

#### Scenario: Changing the family is a one-place change

- **WHEN** the application font family is changed
- **THEN** the change is made in the shared stylesheet and takes effect in every view without editing individual FXML controls

### Requirement: Distinct Options stage background

The Options stage SHALL render its form on an explicit white background that is distinct from the application background colour, so the options zone is visually obvious and not blended into the surrounding frame.

#### Scenario: Options stage shows a white form background

- **WHEN** the user opens the Options stage
- **THEN** the options form area is painted white (`#ffffff`) and is distinguishable from the application background fill

#### Scenario: Options background is independent of the global scene fill

- **WHEN** the application background colour used for the area around pinned views changes
- **THEN** the Options stage still renders its white form background
