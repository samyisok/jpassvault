
## GRASP Verification — 2026-10-04

**Scope:** git diff HEAD + untracked change files
**Files changed:** 12 modified + 13 new
**Findings:** 2

### Problems

- [ ] `src/main/java/com/samyisok/jpassvaultclient/MainListener.java:23` — High coupling: 6 direct dependencies (stageHolder, vaultLoader, viewLoader, remoteVault, options, session) exceeds the 5-dependency threshold; Session injection pushed it over. Extract UNLOCK/LOCK remote-sync coordination into a dedicated coordinator.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/controllers/VaultController.java:33` — Creator/birth-control violation: VaultController instantiates `ClipboardAutoClear.forSystemClipboard()` inline instead of receiving it from the composition root; blocks test seam for `copy()` and pulls JavaFX clipboard into construction. Inject via AppFactory constructor.

### Recommendations

- Introduce a sync coordinator (protected variation behind an interface) so MainListener only routes events.
- Pass ClipboardAutoClear through VaultController's constructor from AppFactory.

## DDD Verification — 2026-10-04

**Scope:** git diff HEAD + untracked change files
**Files changed:** 12 modified + 13 new
**Findings:** 2

### Problems

- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/vault/VaultLoader.java:16` — Infrastructure leaking into domain layer: `domains.vault.VaultLoader` imports root-level `FilePermissions` and performs file IO plus chmod (persistence/OS concern inside domain package). Pre-existing pattern; this diff extends it. Move VaultLoader behind a repository port in an infrastructure package.
- [ ] `src/main/java/com/samyisok/jpassvaultclient/domains/vault/VaultLoader.java:73` — Domain logic in persistence class: HMAC checksum over aggregate JSON is a crypto/domain rule assembled inside the loader. Location was spec'd by change task 4.2; consider a follow-up moving checksum computation into the crypto domain service.

### Recommendations

- Hexagonal split: domain exposes vault repository interface; file adapter owns IO, permissions and encryption IO.
- Relocate checksum assembly next to key material (AesCipher/VaultKeyDerivation) behind a stable method.
