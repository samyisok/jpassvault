package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/main.fxml")
public class MainController {

  @Autowired
  private ApplicationContext appContext;

  @FXML
  PasswordField unlockPassword;

  @Autowired
  VaultLoader vaultLoader;

  @Autowired
  Session session;

  @FXML
  void unlock() throws IOException {
    validatePassword();
    System.out.println("unlockPass: " + unlockPassword.getText());
  }

  void validatePassword() {
    String password = unlockPassword.getText();
    session.setPasswordVault(password);

    if (vaultLoader.vaultPasswordIsValid()) {
      appContext.publishEvent(new StageActionEvent(new Payload("unlock")));
    } else {
      session.setPasswordVault(null);
      warning("Wrong password!", "Try again!");
    }
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
