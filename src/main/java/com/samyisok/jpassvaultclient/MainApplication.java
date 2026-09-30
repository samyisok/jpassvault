package com.samyisok.jpassvaultclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainApplication extends Application {

  private AppFactory appFactory;

  @Override
  public void init() {
    appFactory = new AppFactory();
  }

  @Override
  public void start(Stage stage) {
    stage.addEventHandler(StageActionEvent.STAGE_ACTION,
        appFactory.mainListener()::handle);
    appFactory.stageInit().initialize(stage);
  }

  @Override
  public void stop() {
    Platform.exit();
  }

}
