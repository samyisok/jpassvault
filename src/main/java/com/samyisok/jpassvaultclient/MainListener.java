package com.samyisok.jpassvaultclient;

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
  FxWeaver fxWeaver;

  @Override
  public void onApplicationEvent(StageActionEvent event) {
    System.out.println("Received spring custom event: " + event.getEvent().action);
    Stage stage = stageHolder.getStage();

    double width = stage.getWidth();
    double height = stage.getHeight();

    System.out.println("w:" + width + " h:" + height);

    if (event.getEvent().getAction().equals("vault")) {
      stage.setScene(new Scene(fxWeaver.loadView(VaultController.class),
          width, height));
    } else if (event.getEvent().getAction().equals("main")) {
      stage.setScene(new Scene(fxWeaver.loadView(MainController.class),
          width, height));
    }
  }

}
