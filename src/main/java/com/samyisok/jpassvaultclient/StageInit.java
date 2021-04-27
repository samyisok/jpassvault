package com.samyisok.jpassvaultclient;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import com.samyisok.jpassvaultclient.MainApplication.StageReadyEvent;
import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.data.Options;
import com.samyisok.jpassvaultclient.data.OptionsLoader;
import com.samyisok.jpassvaultclient.models.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

@Component
public class StageInit implements ApplicationListener<StageReadyEvent> {
    private final String applicationTitle;
    private final FxWeaver fxWeaver;

    @Autowired
    private StageHolder stageHolder;

    @Autowired
    private VaultLoader vaultLoader;

    @Autowired
    private OptionsLoader optionsLoader;

    @Autowired
    private Options options;

    public StageInit(@Value("${spring.application.ui.title}") String applicationTitle,
            FxWeaver fxWeaver) {
        this.applicationTitle = applicationTitle;
        this.fxWeaver = fxWeaver;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        loadSettings();
        if (vaultLoader.ifDbExists()) {
            stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
        } else {
            stage.setScene(new Scene(fxWeaver.loadView(SetupController.class)));
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
