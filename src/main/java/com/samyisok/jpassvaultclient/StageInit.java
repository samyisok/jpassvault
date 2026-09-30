package com.samyisok.jpassvaultclient;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.options.OptionsLoader;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Prepares settings on first launch and selects the initial scene.
 * Replaces the former Spring {@code ApplicationListener<StageReadyEvent>};
 * invoked directly from the JavaFX startup path.
 */
public class StageInit {
    private final String applicationTitle;
    private final StageHolder stageHolder;
    private final VaultLoader vaultLoader;
    private final OptionsLoader optionsLoader;
    private final Options options;
    private final ViewLoader viewLoader;

    public StageInit(StageHolder stageHolder, VaultLoader vaultLoader,
            OptionsLoader optionsLoader, Options options, ViewLoader viewLoader,
            String applicationTitle) {
        this.stageHolder = stageHolder;
        this.vaultLoader = vaultLoader;
        this.optionsLoader = optionsLoader;
        this.options = options;
        this.viewLoader = viewLoader;
        this.applicationTitle = applicationTitle;
    }

    public void initialize(Stage stage) {
        loadSettings();
        if (vaultLoader.ifDbExists()) {
            stage.setScene(viewLoader.loadScene(MainController.FXML_PATH));
        } else {
            stage.setScene(viewLoader.loadScene(SetupController.FXML_PATH));
        }
        stage.setTitle(applicationTitle);
        stage.show();
        stageHolder.setStage(stage);
    }

    void loadSettings() {
        prepareFirstLaunch();
        optionsLoader.load();
    }

    void prepareFirstLaunch() {
        createDefaultFolderIfNotExists();
        createSettingsIfNotExist();
    }

    void createDefaultFolderIfNotExists() {
        Path defaultFolder = Path.of(Options.DEFAULT_FOLDER);
        if (Files.notExists(defaultFolder, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectory(defaultFolder);
            } catch (Exception e) {
                critical("Critical Error", e.toString());
            }
        }
    }

    void createSettingsIfNotExist() {
        Path defaultSettings =
                Path.of(Options.DEFAULT_FOLDER, Options.DEFAULT_SETTINGS_FILE_NAME);

        if (Files.notExists(defaultSettings, LinkOption.NOFOLLOW_LINKS)) {
            options.setDefaultData();
            optionsLoader.save(options);
        }
    }

    void critical(String headerMessage, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Critical Error!");
        alert.setHeaderText(headerMessage);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }
}
