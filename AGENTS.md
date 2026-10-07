# AGENTS.md

## Versioning & Commits

- Bump the project version in `pom.xml` **before every commit**, using Semantic Versioning (breaking change → major, new feature → minor, fix → patch).
- Keep `CHANGELOG.md` in sync: the top released version heading must match the `pom.xml` version before committing, and the change's entry goes under it.
- Never commit or tag a mismatch between the `pom.xml` version, the top `CHANGELOG.md` heading, and the release tag — the release workflow fails when the tag and `pom.xml` disagree.
- A version commit and its annotated tag are pushed together (`git push origin main` then `git push origin vX.Y.Z`); the tag triggers the release pipeline.

## Build & Run

- Always use the Maven wrapper: `./mvnw <goal>` (do not rely on a system `mvn`).
- Build/package: `./mvnw -B package --file pom.xml`
- Compile only (fastest check): `./mvnw compile`
- Run the GUI: `./mvnw javafx:run` (requires a display; use Xvfb on headless servers)
- Java 11 is pinned in `pom.xml` and CI — do not assume newer JDKs work.

## Testing

- There are **no active tests**. The single test class (`JpassvaultclientApplicationTests`) has its `@Test` commented out.
- `./mvnw test` passes vacuously. It is not a meaningful quality gate.
- No coverage tools, no test fixtures, no test profile.

## Verification

- No linter, typecheck, checkstyle, or static analysis is configured.
- The only automated verification is `./mvnw compile` (or `package`).
- CI (`.github/workflows/maven.yml`) runs only `mvn -B package` on push/PR to `main`.

## Architecture

- Desktop app: JavaFX 11 UI + Spring Boot 2.4.5 (non-web) + `javafx-weaver-spring-boot-starter` bridges controllers to Spring beans.
- Entry point: `JpassvaultclientApplication` → `MainApplication` (JavaFX `Application`).
- Scene switching is driven by `StageActionEvent` (LOCK / UNLOCK / OPTIONS / CANCEL_FROM_OPTIONS) through `MainListener` and `StageInit`.
- Vault storage is a single encrypted JSON file (default `~/jpassvault/jpassdb.xdb`) — AES/GCM with SHA3-256-derived key. Not a real database; no migrations.
- Settings live in `~/jpassvault/config.json` (plain JSON via Gson).
- First launch triggers `SetupController` to create the directory and default config.
- Remote sync (`RemoteVault`) is optional and only activates when both `apiUrl` and `tokenApi` are set in Options. Uses MD5 checksum for change detection.

## Project requirements to new code

- New changes must be complain with GRASP, SOLID, DDD, TDD, SDD, it will use openspec.
- Before production code fixes, faulty tests should be written first according to TDD.
- at the end coding step, agent should run verification skills for DDD, Openspec, GRASP, and Tests.
- each java file should not be more than 350 lines of the code, if it more than that it is a signal that it requires refactoring, or may be it goes agains GRASP or SOLID and needed to be checked.


## Writing Tests

- tests should have @DisplayName with one line description of what we are testing
- tests should have @BeforeEach with common mocks in the file and common parts, like multiply when() or doReturn()
- tests should be written according with TDD, first tests then business code.
- tests should use mockito.
- unit test should have UnitTest as suffix in their name, and integration tests(that tests which do not mock some modules or uses common context) should have IntegrationTest as suffix in the name
- tests should be readable for a human, and no more than 10 lines for unit tests. 



## Gotchas

- `target/` is committed to git (unusual — do not assume it is gitignored).
- Eclipse IDE files (`.classpath`, `.project`, `.settings/`) are also tracked.
- The vault "DB" is a flat encrypted file — there is no schema, no migration framework, and no way to recover a forgotten password (no key escrow).
