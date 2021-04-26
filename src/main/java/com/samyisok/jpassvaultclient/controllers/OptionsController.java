package com.samyisok.jpassvaultclient.controllers;

import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/options.fxml")
public class OptionsController {

  @Autowired
  private ApplicationContext appContext;

  @FXML
  TextField databasePathField;

  @FXML
  TextField apiUrlField;

  @FXML
  TextField tokenField;


  // @Autowired
  // VaultLoader vaultLoader;

  // @Autowired
  // Session session;

  // void warning(String headerMessage, String message) {
  // Alert alert = new Alert(AlertType.WARNING);
  // alert.setTitle("Warning");
  // alert.setHeaderText(headerMessage);
  // alert.setContentText(message);
  // alert.showAndWait();
  // }

  // @FXML
  // void createDb() {
  // if (!createPassword1.getText().equals(createPassword2.getText())) {
  // warning("Password does not match!", "Specify correct password");
  // return;
  // }
  // if (createPassword1.getText().isEmpty() || createPassword1.getText().length() < 3)
  // {
  // warning("Password invalid", "Password is empty or not complain to security!");
  // return;
  // }

  // session.setPasswordVault(createPassword1.getText());
  // vaultLoader.createEmptyDbIfNotExist();
  // session.setPasswordVault(null);
  // appContext.publishEvent(new StageActionEvent(new Payload("exit")));
  // }

  @FXML
  void chooseDb() {

  }

  @FXML
  void saveOptions() {

  }

  @FXML
  void cancel() {
    appContext.publishEvent(new StageActionEvent(new Payload("cancelFromOptions")));
  }

}
