package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import net.rgielen.fxweaver.core.FxmlView;

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
