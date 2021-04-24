package com.samyisok.jpassvaultclient;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainApplication extends Application {
  private ConfigurableApplicationContext applicationContext;


  @Override
  public void init() {
    applicationContext =
        new SpringApplicationBuilder(JpassvaultclientApplication.class).run();
  }

  @Override
  public void start(Stage stage) {
    // stageHolder.setStage(stage);
    applicationContext.publishEvent(new StageReadyEvent(stage));
  }


  @Override
  public void stop() {
    applicationContext.close();
    Platform.exit();
  }

  static class StageReadyEvent extends ApplicationEvent {
    public StageReadyEvent(Stage stage) {
      super(stage);
    }

    public Stage getStage() {
      return ((Stage) getSource());
    }
  }

}
