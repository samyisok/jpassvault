# Proposal

## Why

A security review of the released 2.2.0 vault found that the stored passwords are not actually protected: AES/GCM encrypts every file version with the same all-zero IV (so two captures of the vault XOR-reveal their plaintext), and the key is a single unsalted SHA3-256 of a master password the app accepts down to 3 characters (so offline guessing runs at GPU rates). Sync additionally sends the API token with no HTTPS requirement, vault/config files are created with default (group/world-readable) permissions, and the master password stays in memory after Lock. These must be fixed before the app can be trusted with real secrets. A follow-up security review of the implemented change confirmed the at-rest fixes and surfaced residual denial-of-service, TOCTOU, key-retention, and robustness gaps, which are folded into this proposal.

## What Changes

- Encrypt each save with a fresh random 12-byte GCM IV instead of the fixed zero IV; the IV keeps being stored with the ciphertext as today.
- Replace the single-round SHA3-256 key derivation with PBKDF2-HMAC-SHA256 (≥600 000 iterations) and a 16-byte random salt stored beside the vault data, behind a format-version tag; existing vault files migrate transparently after the first successful unlock that saves the vault.
- Replace the MD5-over-deterministic-ciphertext sync checksum with a keyed HMAC-SHA256 over the vault content, so change detection stays deterministic without reusing nonces (the checksum redesign is a direct consequence of the IV fix).
- Require master passwords of at least 8 characters when creating a vault; unlocking an existing vault accepts whatever password decrypts it.
- **BREAKING**: reject sync API URLs that are not `https` (plain `http` allowed only for `localhost`/`127.0.0.1`), so existing `http://` sync configurations stop working until moved to `https`.
- Create the vault file, its backup, and `config.json` with owner-only permissions (`0600`) on POSIX systems.
- Clear the master password from the session when the vault is locked.
- Auto-clear a copied password from the system clipboard after 30 seconds, and also when the vault locks or the application exits.
- Mask the API token in diagnostic string output (`Options.toString`) and keep vault record secrets out of value-object `toString` output.
- Validate and bound the KDF/format parameters read from a vault file (iteration ceiling, exact salt/IV lengths, known format version) so a tampered file or a hostile sync response cannot stall unlocking.
- Write the vault, its backup, and `config.json` atomically and owner-only from creation (temp file in the same directory plus atomic move; POSIX `0600` at creation), so a failed save preserves the previous file and secrets are never briefly world-readable; create the vault directory owner-only (`0700`).
- Clear the derived key material (AES/HMAC keys and salt), not only the master password, when the vault is locked.
- Reject malformed remote sync responses cleanly so a bad checksum payload cannot abort the unlock flow.
- Deferred (explicitly out of scope): OS keychain storage for the API token, idle auto-lock of the vault, replacing `System.out` diagnostics with a redacting logger. Accepted residuals: symlink-following vault paths and the absence of remote anti-rollback (see design Non-Goals).

## Capabilities

### New Capabilities

- `vault-security`: how the vault is protected at rest (unique IV, strong KDF with salt and versioned format, migration of old files, owner-only file permissions), how sync traffic is protected (HTTPS-only endpoints, keyed integrity checksum), and how secrets are handled while the app runs (session cleared on lock, token never echoed in diagnostics, clipboard auto-clear).

### Modified Capabilities

- `desktop-runtime`: the "Backward-compatible data and protocol formats" requirement currently freezes the AES/GCM parameters, SHA3-256 key derivation, `config.json`, and the sync protocol as byte-identical forever — exactly the code this change must replace. It becomes a versioned-format-with-migration guarantee: existing files keep opening, formats may evolve behind a version tag, and the sync wire protocol stays unchanged except for requiring HTTPS.

## Impact

- Code: `crypto/AesCipher` (IV + KDF + bounding/format validation + clearing derived keys on lock), `domains/vault/VaultLoader` (checksum, migration, atomic owner-only writes, permissions), `domains/vault/VaultContainer` (`toString` redaction), `StageInit` (owner-only vault directory), `domains/options/Options`/`OptionsLoader` (HTTPS validation, token masking, atomic permissions), `domains/session/Session` + `MainListener` (clear on lock), `controllers/SetupController` (minimum length), `remote/RemoteVault` (HTTPS guard, response robustness), `controllers/ClipboardAutoClear`/`VaultController` (clipboard timer, clear on lock/exit).
- User data: existing `jpassdb.xdb` files stay openable (the IV is stored inside each file; the salt/KDF upgrade happens on unlock and re-saves in the new format). Writes become atomic, so a failed save preserves the previous file instead of truncating it. Files written by the new version are not readable by pre-2.3 versions — a downgrade loses new writes, not the data.
- Dependencies: none added; PBKDF2 ships in the JDK (`javax.crypto.SecretKeyFactory`).
- Remote sync server: no wire-protocol change (same paths, headers, JSON); only the URL scheme policy changes client-side.
- Tests: written first (TDD), covering IV uniqueness, KDF verification + migration, bounded-parameter rejection, atomic writes, HTTPS rejection, permission bits, derived-key clearing, session clearing, remote-response robustness, and clipboard expiry/clearing; the toolkit-backed integration test continues to run under `xvfb-run`.
- Release: `desktop-runtime`'s compatibility scenarios and the `portable-executables` pipeline are unaffected beyond the app version bump.
