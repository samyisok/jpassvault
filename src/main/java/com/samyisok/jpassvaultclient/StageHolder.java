package com.samyisok.jpassvaultclient;

import org.springframework.stereotype.Component;

import javafx.stage.Stage;

@Component
public class StageHolder {
  private Stage stage;

  /**
   * @return the stage
   */
  public Stage getStage() {
    return stage;
  }

  /**
   * @param stage the stage to set
   */
  public void setStage(Stage stage) {
    this.stage = stage;
  }  
}
