package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import net.rgielen.fxweaver.core.FxmlView;
import com.samyisok.jpassvaultclient.StageActionEvent;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/main.fxml")
public class MainController {

  @Autowired
  private ApplicationContext appContext;

  @FXML
  PasswordField unlockPassword;

  @FXML
  private void unlock() throws IOException {
    System.out.println("unlockPass: " + unlockPassword.getText());
    appContext.publishEvent(new StageActionEvent(new Payload("unlock")));
  }
}
