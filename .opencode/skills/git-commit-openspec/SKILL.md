---
name: git-commit-openspec
description: >
  Generate a human-readable git commit from an archived OpenSpec change and commit it.
  Use when the user says "commit this change", "git commit openspec", "commit adr",
  "archive and commit", or after running opsx:archive. Also triggers when the user
  asks to create a commit from an OpenSpec change, ADR, or spec delta.
---

# Git Commit OpenSpec

Generate a clear, human-readable git commit message from an archived OpenSpec change, stage all files, and commit.

## When to use

- User explicitly asks to commit an OpenSpec change or ADR
- User says "commit this change", "git commit openspec", "commit adr", "archive and commit"
- After `opsx:archive` has been run and the user wants to commit the result

## Workflow

1. **Find the archived change**: Look in `./openspec/changes/archive/` for the most recently modified subdirectory. If multiple exist, use the one that was most recently archived (check directory mtime or the archive timestamp in the path).

2. **Read the change files**: Read `proposal.md`, `design.md`, and `specs/*/spec.md` from the archived change directory.

3. **Generate the commit message** using the format below.

4. **Stage and commit**: Run `git add -A` then `git commit` with the generated message.

## Commit message format

```
ADR-NNN: <short human-readable title>

tldr: <1-2 sentence summary a human can understand>

proposal: <2-3 sentence simplified version of proposal.md>

design: <1-5 lines simplified version of design.md — main decisions only>

spec: <1-5 sentence simplified version of spec.md>

ref: ./openspec/changes/archive/<change-name>

Co-authored-by: <model-name> <model@localhost>
Co-authored-by: opencode <opencode@localhost>
```

### Format rules

- **Title**: Derive from the change name. Convert kebab-case to readable form. Example: `adr-001-migration-to-java-25` → `ADR-001: migration to Java 25, JavaFX 25`
- **tldr**: One or two sentences. Plain language. What does this change do and why? No jargon.
- **proposal**: Two to three sentences. Summarize the "Why" and "What Changes" sections. Drop technical detail.
- **design**: One to five lines. Summarize the key decisions (D1, D2, etc.) in plain language. Focus on what was chosen and why.
- **spec**: One to five sentences. Summarize what the spec requires in plain language.
- **ref**: The relative path to the archived change directory from repo root.
- **Co-authored-by**: One line per model/client that contributed. Use the model name from the session. Always include `opencode <opencode@localhost>`.

### Simplification guidelines

- Remove file paths, class names, and implementation details unless critical to understanding
- Replace technical jargon with plain language where possible
- Keep sentences short and direct
- If a section is too long, prioritize the "why" over the "how"
- Do not invent information — only summarize what is in the files

## Example

Given an archived change `adr-001-migration-to-java-25` with proposal about removing Spring Boot and migrating to Java 25:

```
ADR-001: migration to Java 25, JavaFX 25

tldr: Remove Spring Boot and migrate the desktop app to Java 25 LTS with the latest JavaFX, keeping all user-visible behavior identical.

proposal: The app stack is end-of-life (Java 11, Spring Boot 2.4.5, JavaFX 11). Spring Boot is only used as a DI container and event bus for a desktop app. This change removes it entirely and modernizes to Java 25 LTS.

design: Use a hand-rolled composition root instead of a DI framework since the object graph is small and static. Replace Spring events with JavaFX-native events. Package as a fat jar via maven-shade-plugin.

spec: The desktop runtime requires JDK 25, JavaFX from Maven Central, manual DI, and a runnable fat jar. Vault format, crypto, and settings format remain unchanged.

ref: ./openspec/changes/archive/adr-001-migration-to-java-25

Co-authored-by: LongCat 2.5 Preview <longcat-2.5-preview-free@localhost>
Co-authored-by: opencode <opencode@localhost>
```

## Edge cases

- **No archive found**: Tell the user no archived OpenSpec change was found. Suggest running `opsx:archive` first.
- **Multiple archives**: Use the most recently modified one. Mention to the user which one was selected.
- **Missing files**: If `proposal.md`, `design.md`, or `spec.md` is missing, skip that section and note it in the commit body.
- **Unrelated changes**: If `git status` shows unrelated changes, warn the user before staging everything.
