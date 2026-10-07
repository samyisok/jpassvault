# Tasks

## 1. Bundle the typeface

- [x] 1.1 Add `OpenSans-Regular.ttf`, `OpenSans-Bold.ttf`, and the Open Font License text under `src/main/resources/com/samyisok/jpassvaultclient/fonts/`; verify with `ls` that all three files exist
- [x] 1.2 Confirm the font ships in the artifact: run `./mvnw -B package` and verify `unzip -l target/jpassvaultclient-3.0.0.jar | grep -i opensans` lists both `.ttf` files

## 2. Register and apply the font

- [x] 2.1 Write a failing `FontRegistrationIntegrationTest` that starts the JavaFX toolkit, calls the font registration helper, and asserts the bundled family is loaded and non-null (run under a display, as the CI does)
- [x] 2.2 Implement a small `FontLoader`/registration helper that loads the bundled resources once and exposes the family name; verify 2.1 passes
- [x] 2.3 Add a shared `app.css` that sets `-fx-font-family` on `.root` and load it for every scene in `ViewLoader`; add a `ViewLoaderUnitTest` asserting every loaded scene includes `app.css`, and verify it passes

## 3. Remove per-control platform fonts

- [x] 3.1 Write a failing `ViewTypographyUnitTest` that scans the four FXML views and the stylesheets and asserts no view names a platform-specific font (`Segoe UI`, `Segoe UIl`); verify it fails against the current files
- [x] 3.2 Remove all per-control `<font>` blocks from `main.fxml`, `setup.fxml`, `options.fxml`, and `vault.fxml`, move the designed sizes into the stylesheets (`.button` 20, `.smallButton` 15, `.iconButton` 18, text fields 18, `.list-cell` 20pt), and correct `vault.css` to use the bundled family; verify 3.1 passes
- [x] 3.3 Run `./mvnw -B package` and verify the build succeeds with the toolkit-backed tests green

## 4. Restore the Options background

- [x] 4.1 Write a failing `OptionsBackgroundUnitTest` asserting the `options.fxml` root declares an explicit white (`#ffffff`) background; verify it fails against the current file
- [x] 4.2 Set `-fx-background-color: #ffffff;` on the `options.fxml` root and verify 4.1 passes

## 5. Integration verification

- [x] 5.1 Run the app with `./mvnw javafx:run` (or under `xvfb-run -a`) and confirm the Options stage shows a white form background and all views render in one consistent typeface
- [x] 5.2 Confirm the bundled font is also present in a `jpackage` application image produced by the release packaging step
- [x] 5.3 Run `openspec validate stabilize-ui-frontend --strict` and confirm it reports valid
- [x] 5.4 Run the DDD, GRASP, and test verification skills over the diff and resolve any findings
