package com.samyisok.jpassvaultclient;

import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.OptionsController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.controllers.VaultController;
import com.samyisok.jpassvaultclient.models.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

@Component
public class MainListener implements ApplicationListener<StageActionEvent> {

  @Autowired
  StageHolder stageHolder;

  @Autowired
  VaultLoader vaultLoader;

  @Autowired
  FxWeaver fxWeaver;

  @Override
  public void onApplicationEvent(StageActionEvent event) {
    System.out.println("Received spring custom event: " + event.getEvent().action);
    Stage stage = stageHolder.getStage();

    if (event.getEvent().getAction().equals("unlock")) {
      vaultLoader.load();
      stage.setScene(new Scene(fxWeaver.loadView(VaultController.class)));
    } else if (event.getEvent().getAction().equals("exit")) {
      stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
    } else if (event.getEvent().getAction().equals("options")) {
      stage.setScene(new Scene(fxWeaver.loadView(OptionsController.class)));
    } else if (event.getEvent().getAction().equals("cancelFromOptions")) {
      if (vaultLoader.ifDbExists()) {
        stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
      } else {
        stage.setScene(new Scene(fxWeaver.loadView(SetupController.class)));
      }
    }
  }

}
