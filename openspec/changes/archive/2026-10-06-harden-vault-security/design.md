# Design

## Context

- See proposal.md for the findings that motivate this change. Current code: `AesCipher` (zero IV at encrypt, `SHA3-256(password)` once for the key, IV prepended to base64 ciphertext, raw base64 file body), `VaultLoader` (MD5 over a freshly re-encrypted vault for sync change detection — deterministic only because the IV never changes), `RemoteVault` (token header, URL straight from settings, no scheme check), `MainListener` (LOCK clears the vault map but not `Session`), plain `FileWriter` outputs (default umask), `SetupController` (minimum password length 3).
- Constraint: existing users hold vault files in the current format; the file itself carries no version marker today (body is base64 of `IV || ciphertext`).
- Constraint: JDK 25 only, no new dependencies — PBKDF2 (`SecretKeyFactory`), `Mac` (HMAC-SHA256), `SecureRandom` are all in the JDK.
- Constraint: remote server is a third-party sync store we do not control — wire shapes must not change.

## Goals / Non-Goals

**Goals:**

- Kill both critical findings: unique IV per encryption, iterated salted KDF.
- Keep every existing vault file openable, with a tested migration path.
- Keep sync change detection deterministic and make it keyed (the IV fix removes accidental determinism).
- Enforce HTTPS, owner-only files, session/clipboard/token hygiene.
- TDD: failing tests land before each production change (repo rule).

**Non-Goals:**

- OS keychain storage of the API token; idle auto-lock; a redacting logger (deferred in proposal).
- Re-encrypting data the user already exported/backed up outside the app.
- Changing the sync server's API or the JSON record model.
- Windows ACL hardening beyond what POSIX permission setting gives us (documented residual).
- Symlink-following vault paths: a hand-edited `config.json` can point the vault path at a symlink; the settings file is owner-only, so this is a local-only accepted residual.
- Remote anti-rollback: the sync server can replay an older, validly-encrypted vault or claim a matching checksum; `merge` never deletes local records, so impact is stale state only (accepted residual).

## Decisions

### D1: File envelope — JSON wrapper, version `2`, detected by leading `{`

New vault body becomes a small JSON envelope: `{"format":2,"kdf":{"alg":"PBKDF2WithHmacSHA256","iterations":600000,"salt":"<b64>"},"iv":"<b64>","ct":"<b64>"}`. Old files are detected because their body is pure base64 (never `{`).

- Why JSON: the project already ships Gson; no new parser; fields extensible later (e.g. argon2 params) without a new discriminator scheme.
- Why not a magic byte prefix: the file is currently consumed as text end-to-end (`BufferedReader.lines()`); a JSON prefix keeps every reader path unchanged in shape.
- The GCM tag keeps covering only the ciphertext; `format`/`kdf`/`iv` are not secret and need no authenticity — tampering with them only makes unlocking fail (wrong salt/iterations ⇒ tag mismatch).

### D2: One PBKDF2 call, 64-byte output split into AES key and HMAC key

`PBKDF2WithHmacSHA256(password, salt, iterations=600000, 512 bits)` → first 32 bytes AES-256 key, last 32 bytes HMAC-SHA256 key. Salt = 16 random bytes, generated once per vault (at creation and at migration), stored in the envelope.

- Alternative: separate PBKDF2 calls with different labels — doubles the dominant cost (600k iterations ×2) for no security gain; a single wide derivation is standard practice (SP 800-108 counter mode style split).
- Alternative: Argon2id — better memory-hardness, but needs a third-party library; rejected under the no-new-dependency constraint. The envelope's `kdf` block keeps the door open.

### D3: Sync checksum = HMAC-SHA256(vaultKeyHmac, plaintext JSON), uppercase hex, replaces MD5

- Deterministic without touching IV reuse: keyed over the plaintext content, stable across saves while content is unchanged.
- Keyed ⇒ the sync server cannot forge or even compute candidate checksums, so it cannot do confirmation-of-content attacks.
- Known behavior kept from today: two machines with identical records inserted in different map orders serialize differently, produce different checksums, and trigger a merge — the merge path already tolerates this; no regression.
- The IV fix and this checksum ship in the same task (checksum's determinism currently depends on the zero IV).
- **Legacy vaults before their first save have no salt**: until the envelope exists, the HMAC key is the legacy SHA3-256-derived key (still keyed and secret, so the server-side guarantees hold). The first save migrates the file and switches the HMAC key to the PBKDF2 split — the checksum changes exactly once, forcing one harmless re-upload that the merge path already tolerates.

### D4: HTTPS validation at two points — `Options.setApiUrl` and request build

- `setApiUrl` rejects non-https (except localhost/127.0.0.1) with an exception the Options screen turns into a warning — covers the UI path.
- `RemoteVault.getRequest/postRequest` re-checks `URI scheme` before sending — covers a hand-edited `config.json` (settings are plain text).
- Alternative: validate only at request time — poor UX (silent failure of an edit); only-at-config — bypassable. Both points are three lines each.

### D5: Owner-only permissions via a wrapped POSIX attribute (helper kept)

A tiny `FilePermissions` helper exposes the owner-only attribute: if `FileSystems.getDefault().supportedFileAttributeViews().contains("posix")` use `rw-------`, else no-op. The attribute is applied **at file creation** by the atomic write path (D11); the previous post-write `chmod` (write-then-harden) is retired because it left a group/world-readable window (finding M3). Existing files are recreated owner-only on the next successful save rather than at startup (avoids write access on every launch).

### D6: LOCK clears session after remote save

`MainListener` LOCK branch order stays: `remoteVault.save()` (still needs the key) → `vaultLoader.unload()` → **new** `session.setPasswordVault(null)` → switch scene. Clearing earlier would break the final sync upload; clearing later than the scene switch leaves a window where a test can observe it — right after unload is the seam.

### D7: Clipboard clear with an injectable one-shot scheduler

`ClipboardAutoClear` component: `clearAfter(Duration.ofSeconds(30), Clipboard, expectedContent)` using JavaFX `PauseTransition` (runs on the FX thread, no extra thread). Guard: only clear if the clipboard still holds the exact password we placed (don't clobber something the user copied afterwards). Controller takes it via the composition root; tests drive a fake scheduler — no 30-second sleeps.

### D8: Minimum length enforced in the setup/change path only

`SetupController.createDb` gets the 8-char check (with the existing warning pattern). Unlock (`MainController.validatePassword`) is untouched — length is not a secret to validate, the GCM tag already is the oracle. Migration does not re-check length either, so pre-existing short-password vaults keep opening (spec scenario).

### D9: Migration is lazy — on first successful unlock + save

No migration pass at startup: wrong-password attempts must not rewrite files (spec), and a passive upgrade that never opens the vault must not touch it. `VaultLoader.save` always writes the current envelope; `load` branches on leading `{`.

### D10: Validate and bound the stored KDF/format parameters (findings M1, L6)

Before deriving in `decryptEnvelope`: require `format == CURRENT_FORMAT`, `1 <= iterations <= 5_000_000`, `salt.length == 16`, `iv.length == 12`, and a bounded body length; otherwise fail closed with `EncryptionException`.

- Rationale: the spec requires stored parameters to drive derivation, but stored data must not be trusted unboundedly — a tampered file or a hostile/compromised sync response could otherwise pin the CPU (`iterations = Integer.MAX_VALUE`) or exhaust memory (oversized Base64) during unlock. The GCM tag remains the integrity oracle for `ct`; these checks fail before it is consulted.
- Alternative: cap only the iteration count — leaves the OOM path open; rejected.

### D11: Atomic writes, owner-only from creation (findings M3, M4; supersedes D5's ordering)

Encrypt to a `String` first; write a same-directory temp file created with the POSIX `0600` attribute; flush/fsync; then `Files.move(tmp, dest, ATOMIC_MOVE, REPLACE_EXISTING)`. Encryption or write failure leaves the previous file untouched and is surfaced instead of the current silent `System.out`-only truncation.

- Rationale: closes the TOCTOU window where `config.json`'s plaintext token was briefly umask-readable, and removes silent truncate-then-encrypt data loss. Reverses the atomic-move deferral previously recorded in Risks.
- Trade-off: `ATOMIC_MOVE` within one filesystem works on Linux/macOS; where the filesystem rejects it, fall back to a non-atomic replace of the temp file — still owner-only and still not truncating the live file until the new content is complete.

### D12: Clear derived key material on lock (finding M2)

`AesCipher.clearKeys()` zero-fills `salt` and `hmacKey` (`Arrays.fill`) and drops `aesKey`, invoked from the LOCK path after `unload()`. `getHmacKey()` no longer falls back to the legacy key after a vault has been unlocked and locked: without a session password it fails closed (also addresses the L5 fallback).

- Best-effort only: `SecretKeySpec` is not destroyable and the JIT may keep copies; documented residual. Removes the easy post-lock heap-dump key recovery that defeated the password clear.

### D13: Remote response robustness (finding L2)

A checksum payload that cannot be interpreted raises `RemoteException("invalid response")` instead of returning `null`, and the UNLOCK remote block also catches `RuntimeException`, so a malformed response cannot abort the scene switch after the vault has loaded.

### D14: Clipboard cleared on lock and exit; no retained copy (finding L3)

`ClipboardAutoClear` gains an explicit clear reused on LOCK and on application exit (shutdown hook), sharing the "only if still ours" guard. When foreign content is detected, the retained `copiedText` reference is dropped.

### D15: Vault directory is owner-only (finding L4)

`StageInit` creates the default vault directory with the POSIX `0700` attribute where supported.

### D16: Extract UNLOCK/LOCK lifecycle coordination out of `MainListener`

`VaultLifecycleCoordinator(vaultLoader, remoteVault, options, session, clipboardAutoClear)` owns the unlock action (local load, optional remote availability check and remote load) and the lock action (optional remote save, unload, clear session, clear clipboard), plus `hasVault()`/`discard()` for the options-cancel path. `MainListener` shrinks to `stageHolder`, `viewLoader`, coordinator and only routes events to scenes.

- Why: scene routing (GRASP Controller) and vault lifecycle side effects are different responsibilities; `MainListener` had crossed 6 direct dependencies (7 after the clipboard clear) and could not change for one reason.
- Trade-off: one more indirection; observable behavior is unchanged, and the existing listener tests re-target to the coordinator.

### D17: Vault persistence behind a `VaultStore` port

`domains.vault.VaultStore` (port): `exists()`, `read()`, `write(String)`. `FileVaultStore` (root package, infrastructure) resolves its `Path` lazily from a `Supplier<Path>` and performs writes through `FilePermissions.writeOwnerOnly` (atomic, owner-only). `VaultLoader` depends only on the port (a main store and a backup store) and no longer imports `java.nio.file`, `Options`, or permission code.

- Why: the domain layer must not know about files, paths, or POSIX modes (DDD: persistence ignorance); the atomic/owner-only policy stays in the adapter.
- Trade-off: constructor signature changes and `AppFactory` wires `new FileVaultStore(options::getFullPathVaultOrDefault)` plus the backup store; `save(Vault, Path)` disappears.

### D18: Checksum assembly moves next to the key material

New `AesCipher.checksumOf(String content)` computes HMAC-SHA256 over the content with the derived HMAC key and returns uppercase hex. `VaultLoader.getVaultEncryptCheckSum()` remains a thin delegation so callers (e.g. `RemoteVault`) are unchanged.

- Why: the crypto rule belongs with the key it uses, not with the persistence class that happens to serialize the aggregate (DDD/GRASP Information Expert).
- Trade-off: none beyond the extra method; behavior (stable, keyed, record-sensitive) is unchanged and already pinned by `VaultChecksumUnitTest`.

## Risks / Trade-offs

- [Old app reads a new-format file and silently fails to load content] → old code base64-decodes `{...}` and throws ⇒ `load()` catches and reports; spec scenario "Newer-format file is recognized, not garbled" pins the clean-failure behavior; downgrade loses new writes only (accepted in proposal).
- [600k PBKDF2 iterations make unlock slow on weak machines] → target well under 1 s on modern CPUs (~0.3–0.5 s); if CI proves slow, the stored-params design allows lowering the count without breaking files.
- [Migration rewrites the vault file — a crash mid-write can truncate it] → mitigated by D11: encrypt first, write a temp file, atomic move; a failed write preserves the previous file.
- [HMAC over serialized plaintext depends on Gson field/iteration order] → same as today's MD5-over-ciphertext; merge path tolerates ordering differences (D3).
- [HTTPS rejection locks out users on plain-http sync servers] → deliberate BREAKING change, called out in proposal; localhost carve-out keeps dev setups alive.
- [Timer-based clipboard clear races with system clipboard managers] → clear only when content still matches ours (D7); worst case the password stays — no worse than today.
- [POSIX perms are a no-op on Windows] → residual documented; Windows user-profile ACLs already scope files to the user by default.
- [Lazy migration leaves a read-only legacy vault on the single-round SHA3 + zero IV key indefinitely] → accepted (D9; spec scenario "First save upgrades the file format"); the Open Question below tracks an explicit upgrade action. Residual until the user next saves.
- [Derived-key zeroization is best-effort] → JIT copies of key material may survive `Arrays.fill`; D12 reduces, not eliminates, the post-lock window.
- [Symlinked vault path and remote anti-rollback] → accepted residuals, recorded in Non-Goals.

## Migration Plan

1. Ship: tests first, then envelope + KDF + IV + checksum, then transport/hygiene items (task order in tasks.md).
2. On first unlock of an old vault: decrypt with old path (single SHA3-256, IV from file) → in-memory vault → next save writes envelope v2 with fresh salt (PBKDF2) and fresh IV.
3. Rollback: the previous release cannot read v2 files (clean failure, no corruption); users restore from `jpassdb.bak.xdb` **if** it was written by the old version — backup written after upgrade is also v2. Communicated in release notes when this change ships.
4. Sync servers: no action; clients keep uploading the same envelope string as the `file` value.

## Open Questions

- Whether to add a "re-encrypt now" menu action for impatient migrations (spec does not require eager migration; can be added later without spec changes).
- Whether the backup file should keep one old-format generation alongside the v2 file — deferred; current single-backup behavior is spec-neutral.
- Whether to promote the lazy-migration residual (finding M5) to eager re-encryption on the first successful unlock, or to the "re-encrypt now" action above; the spec currently requires migration on save only.
