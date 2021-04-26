package com.samyisok.jpassvaultclient;

import com.samyisok.jpassvaultclient.MainApplication.StageReadyEvent;
import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.models.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.scene.Scene;
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

    public StageInit(@Value("${spring.application.ui.title}") String applicationTitle,
            FxWeaver fxWeaver) {
        this.applicationTitle = applicationTitle;
        this.fxWeaver = fxWeaver;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        if (vaultLoader.ifDbExists()) {
            stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
        } else {
            stage.setScene(new Scene(fxWeaver.loadView(SetupController.class)));
        }
        stage.setTitle(applicationTitle);
        stage.show();
        stageHolder.setStage(stage);
    }
}
