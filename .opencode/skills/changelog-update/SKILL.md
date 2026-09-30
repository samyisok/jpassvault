---
name: changelog-update
description: >
  Update CHANGELOG.md from an archived OpenSpec change and current code changes.
  Use when the user says "update changelog", "changelog", "after archive", or after
  running opsx:archive. Also triggers when the user asks to document changes in
  a changelog or release notes.
---

# Changelog Update

Update `CHANGELOG.md` (create if it does not exist) using the [Keep a Changelog](https://keepachangelog.com/) format. Focus on features and functions over code — write for humans, not machines.

## When to use

- User explicitly asks to update the changelog
- User says "update changelog", "changelog", "after archive"
- After `opsx:archive` has been run and the user wants to document the change
- Before a release or version bump

## Workflow

1. **Find the archived change**: Look in `./openspec/changes/archive/` for the most recently modified subdirectory.

2. **Read the change files**: Read `proposal.md`, `design.md`, and `specs/*/spec.md` from the archived change directory.

3. **Check current code changes**: Run `git diff HEAD~1` or `git log --oneline -5` to understand what code changed. Use this for detail if the OpenSpec files are unclear.

4. **Determine version**: Check `pom.xml` or `package.json` for the current version. If the archived change introduces a breaking change, bump the major version. New features bump the minor. Fixes bump the patch.

5. **Update CHANGELOG.md**: Add a new version section with the appropriate subsections. If `CHANGELOG.md` does not exist, create it with the standard header.

6. **Do not commit**: Leave the file for the user to review and commit with the next commit.

## Changelog format

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2026-09-30

### Added

- Vault export to CSV: users can now export all records to a portable CSV file with a file chooser dialog and overwrite confirmation.

### Changed

- Migrated from Java 11 and Spring Boot 2.4.5 to Java 25 LTS with JavaFX 27, removing Spring Boot entirely.

### Fixed

- CSV injection prevention: fields starting with formula characters are now prefixed with a single quote.

### Removed

- Spring Boot dependency and javafx-weaver; replaced with a hand-rolled composition root.

## [1.1.0] - 2026-08-15

### Added

- Remote sync with MD5 checksum-based change detection.
```

### Section rules

- **Added**: New features or capabilities the user can now do
- **Changed**: Changes to existing functionality (not fixes)
- **Fixed**: Bug fixes or corrections
- **Removed**: Features or capabilities removed
- **Deprecated**: Features marked for removal in a future version
- **Security**: Security-related changes

### Writing guidelines

- Write in plain language a non-technical person can understand
- Focus on what the user can do or experience, not how the code works
- One bullet per change, concise but complete
- Use present tense ("add" not "added", "fix" not "fixed")
- Do not mention file paths, class names, or implementation details unless critical
- Do not invent information — only document what is in the OpenSpec files or git diff
- If a change is internal-only (refactoring, dependency bumps with no user impact), omit it or put it under `### Changed` with a brief note

## Version bump rules

- **Major (X.0.0)**: Breaking changes — removed features, changed data formats, changed user workflows
- **Minor (0.X.0)**: New features, new capabilities, non-breaking additions
- **Patch (0.0.X)**: Bug fixes, security patches, no new features

If unsure, default to minor for new features and patch for fixes.

## Edge cases

- **No archive found**: Tell the user no archived OpenSpec change was found. Suggest running `opsx:archive` first.
- **Multiple archives**: Use the most recently modified one. Mention to the user which one was selected.
- **Missing files**: If `proposal.md`, `design.md`, or `spec.md` is missing, skip that source and rely on git diff.
- **No CHANGELOG.md**: Create it with the standard header and the first version section.
- **Existing CHANGELOG.md**: Add the new version section above the previous one, below `## [Unreleased]` if present.
- **Unrelated changes**: If git diff shows changes not related to the archived OpenSpec change, ask the user whether to include them.
