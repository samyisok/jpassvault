# Tasks

## 1. Unique IV per encryption

- [x] 1.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/crypto/AesCipherUniqueIvUnitTest.java`: encrypting the same plaintext twice yields different ciphertexts, and a roundtrip still decrypts — verify: `./mvnw test -Dtest=AesCipherUniqueIvUnitTest` fails (red)
- [x] 1.2 Implement a fresh random 12-byte IV per `encrypt` call via `SecureRandom` (IV still prepended to ciphertext) — verify: 1.1 turns green and the whole suite still passes

## 2. Salted iterated KDF and versioned envelope

- [x] 2.1 Write failing tests `src/test/java/com/samyisok/jpassvaultclient/crypto/VaultEnvelopeUnitTest.java`: save/load roundtrip through the JSON envelope (`format`, `kdf`, `iv`, `ct`), two vaults created with the same password store different salts, derivation parameters are read from the stored file, and a wrong password fails to decrypt — verify: `./mvnw test -Dtest=VaultEnvelopeUnitTest` fails (red)
- [x] 2.2 Implement the envelope writer/reader with PBKDF2-HMAC-SHA256, 600 000 iterations, 16-byte random salt, 64-byte output split into AES key + HMAC key (split KDF into its own class if `AesCipher` would exceed 350 lines) — verify: 2.1 green and `./mvnw test` fully green

## 3. Migration of old-format vault files

- [x] 3.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/domains/vault/VaultMigrationIntegrationTest.java` (IntegrationTest suffix; static legacy fixture under `src/test/resources` crafted independently — base64 `IV||ciphertext`, SHA3-256 key, zero IV — since task 2.2 removes the production code that could mint one): correct password opens it, a wrong password rejects it **and leaves the file bytes unchanged**, and a subsequent save rewrites it as envelope v2 — verify: `./mvnw test -Dtest=VaultMigrationIntegrationTest` fails on the save assertion (red)
- [x] 3.2 Write failing test `src/test/java/com/samyisok/jpassvaultclient/crypto/LegacyDecodeGuardUnitTest.java`: the legacy base64/IV decode path given a v2 envelope body raises an error and returns no plaintext (this is what a downgraded application version experiences) — verify: `./mvnw test -Dtest=LegacyDecodeGuardUnitTest` fails to compile/red (the legacy path is not yet isolated)
- [x] 3.3 Make `VaultLoader.load` branch on the leading `{` (envelope) vs base64 body (legacy), keep the legacy decode isolated in its own method so the downgrade scenario stays pinned, and confirm saves always write v2 — verify: 3.1 and 3.2 green, and the legacy fixture remains openable after the change

## 4. Keyed deterministic sync checksum

- [x] 4.1 Write failing tests `src/test/java/com/samyisok/jpassvaultclient/domains/vault/VaultChecksumUnitTest.java`: checksum is identical when content is unchanged, changes when a record is added/edited/deleted, and differs for identical content under a different password (keyed) — verify: `./mvnw test -Dtest=VaultChecksumUnitTest` fails (red)
- [x] 4.2 Replace the MD5-over-ciphertext in `VaultLoader.getVaultEncryptCheckSum` with HMAC-SHA256 over the vault JSON using the HMAC key from D2 — verify: 4.1 green; grep confirms no `MD5` usage remains under `src/main` and `./mvnw test` is green (sync merge/upload tests still pass)

## 5. HTTPS-only sync endpoints

- [x] 5.1 Write failing tests `src/test/java/com/samyisok/jpassvaultclient/domains/options/SyncUrlSecurityUnitTest.java`: `setApiUrl` rejects `http://sync.example.com`, accepts `https://sync.example.com`, accepts `http://localhost:8080` and `http://127.0.0.1:8080`, and a scheme guard used before sending refuses a non-https non-local URL — verify: `./mvnw test -Dtest=SyncUrlSecurityUnitTest` fails (red)
- [x] 5.2 Implement validation in `Options.setApiUrl`, the pre-send guard in `RemoteVault.getRequest`/`postRequest`, and the user-facing warning in `OptionsController` — verify: 5.1 green and `./mvnw test` green

## 6. Owner-only file permissions

- [x] 6.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/FilePermissionsUnitTest.java`: after `VaultLoader.save` (including backup) and `OptionsLoader.save` on a POSIX system, vault, backup, and `config.json` carry `rw-------` (0600); skip assertion when POSIX view is unavailable — verify: `./mvnw test -Dtest=FilePermissionsUnitTest` fails (red)
- [x] 6.2 Implement the `FilePermissions.ownerOnly(path)` helper (no-op without POSIX view) and call it after every write in `VaultLoader.save`/`saveBackup` and `OptionsLoader.save` — verify: 6.1 green and `./mvnw test` green

## 7. Session cleared on lock

- [x] 7.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/MainListenerLockUnitTest.java`: with a password set in the session and remote sync stubbed, handling `LOCK` leaves `session.getPasswordVault()` null — verify: `./mvnw test -Dtest=MainListenerLockUnitTest` fails (red)
- [x] 7.2 In `MainListener`'s LOCK branch, call `session.setPasswordVault(null)` after `vaultLoader.unload()` and before the scene switch — verify: 7.1 green and `./mvnw test` green

## 8. Minimum master password length at creation

- [x] 8.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/controllers/SetupPasswordMinLengthUnitTest.java`: a 7-character password shows a warning and creates no vault, an 8-character password proceeds, and the unlock path is not length-checked — verify: `./mvnw test -Dtest=SetupPasswordMinLengthUnitTest` fails (red)
- [x] 8.2 Add the length-8 check to `SetupController.createDb` using the existing warning pattern — verify: 8.1 green and `./mvnw test` green

## 9. Clipboard auto-clear

- [x] 9.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/controllers/ClipboardAutoClearUnitTest.java`: scheduling a clear removes the copied password after the 30-second delay, and does NOT clear when the clipboard already holds different content (fake scheduler, no real waiting) — verify: `./mvnw test -Dtest=ClipboardAutoClearUnitTest` fails (red)
- [x] 9.2 Implement `ClipboardAutoClear` with an injectable one-shot scheduler (JavaFX `PauseTransition` in production) and wire it into `VaultController.copy()` — verify: 9.1 green and `./mvnw test` green

## 10. API token masked in diagnostics

- [x] 10.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/domains/options/OptionsToStringUnitTest.java`: `Options.toString()` never contains the raw token value and renders it masked — verify: `./mvnw test -Dtest=OptionsToStringUnitTest` fails (red)
- [x] 10.2 Fix `Options.toString` to mask `tokenApi` — verify: 10.1 green and `./mvnw test` green

## 11. Integration verification

- [x] 11.1 Run the full CI-equivalent build `xvfb-run -a ./mvnw -B package` on JDK 25 and confirm every unit and toolkit-backed integration test passes — verify: `BUILD SUCCESS`
- [x] 11.2 Confirm no regressions in scope guards: `grep -rn "MD5" src/main` returns nothing, `openspec validate --strict` passes for this change, and no touched file exceeds 350 lines — verify: commands exit clean
## 12. Security-review hardening

- [x] 12.1 Write failing test `src/test/java/com/samyisok/jpassvaultclient/crypto/VaultParameterValidationUnitTest.java`: a body with `iterations` outside the accepted range, a wrong-length salt or IV, or an unknown `format` is rejected cleanly and does not hang — verify: `./mvnw test -Dtest=VaultParameterValidationUnitTest` fails (red)
- [x] 12.2 Implement bounded parameter + format validation in the envelope reader (iteration ceiling, exact salt/IV lengths, `format == current`) raising `EncryptionException` — verify: 12.1 green and `./mvnw test` green
- [x] 12.3 Write failing test `src/test/java/com/samyisok/jpassvaultclient/domains/vault/AtomicVaultWriteUnitTest.java`: a save whose encryption fails leaves the previous file byte-identical, a successful save is owner-only from creation, and the vault directory is `0700` — verify: `./mvnw test -Dtest=AtomicVaultWriteUnitTest` fails (red)
- [x] 12.4 Implement atomic owner-only writes (encrypt to string first, same-directory `0600` temp file, atomic move) with failures surfaced, and create the vault directory `0700` — verify: 12.3 green and `./mvnw test` green
- [x] 12.5 Write failing test (extend `src/test/java/com/samyisok/jpassvaultclient/MainListenerLockUnitTest.java`): after LOCK, no checksum or decryption succeeds without re-unlocking — verify: `./mvnw test -Dtest=MainListenerLockUnitTest` fails (red)
- [x] 12.6 Implement `AesCipher.clearKeys()` (best-effort zeroization of salt/HMAC key, drop AES key) wired into the LOCK path after `unload()` — verify: 12.5 green and `./mvnw test` green
- [x] 12.7 Write failing test `src/test/java/com/samyisok/jpassvaultclient/remote/RemoteResponseRobustnessUnitTest.java`: a checksum payload with no hash value is reported as a sync failure and the unlock flow still completes — verify: `./mvnw test -Dtest=RemoteResponseRobustnessUnitTest` fails (red)
- [x] 12.8 Implement null-safe remote checksum parsing (`RemoteException`) and catch runtime errors in the UNLOCK remote block — verify: 12.7 green and `./mvnw test` green
- [x] 12.9 Write failing test (extend `src/test/java/com/samyisok/jpassvaultclient/controllers/ClipboardAutoClearUnitTest.java`): a pending copied password is cleared on LOCK and on application exit, and the copied value is not retained — verify: `./mvnw test -Dtest=ClipboardAutoClearUnitTest` fails (red)
- [x] 12.10 Implement clipboard clear on lock/exit (shared "only if still ours" guard) and drop the retained copied value — verify: 12.9 green and `./mvnw test` green
- [x] 12.11 Write failing test `src/test/java/com/samyisok/jpassvaultclient/domains/vault/VaultContainerToStringUnitTest.java`: record `toString` never contains the stored password — verify: `./mvnw test -Dtest=VaultContainerToStringUnitTest` fails (red)
- [x] 12.12 Redact record secrets in `VaultContainer.toString` — verify: 12.11 green and `./mvnw test` green
- [x] 12.13 Run the full CI-equivalent build and scope guards (`xvfb-run -a ./mvnw -B package`, no `MD5` under `src/main`, `openspec validate --strict`, no touched file over 350 lines) — verify: commands exit clean

## 13. Review follow-ups (GRASP / DDD)

- [x] Fix high coupling in `src/main/java/com/samyisok/jpassvaultclient/MainListener.java:23` — 6 direct dependencies; extract UNLOCK/LOCK sync coordination (design D16)
- [x] Fix Creator violation in `src/main/java/com/samyisok/jpassvaultclient/controllers/VaultController.java:33` — inject `ClipboardAutoClear` via AppFactory instead of inline creation
- [x] Fix infrastructure leakage in `src/main/java/com/samyisok/jpassvaultclient/domains/vault/VaultLoader.java:16` — domain package performs file IO and chmod; move behind repository port (design D17)
- [x] Fix domain-logic placement in `src/main/java/com/samyisok/jpassvaultclient/domains/vault/VaultLoader.java:73` — HMAC checksum computed in persistence class (design D18)
