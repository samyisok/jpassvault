package com.samyisok.jpassvaultclient.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.StageHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/options.fxml")
public class OptionsController implements Initializable {

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  StageHolder stageHolder;

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

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }


  @FXML
  void chooseDb() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

    File selectedDirectory = directoryChooser.showDialog(stageHolder.getStage());

    System.out.println(selectedDirectory.getAbsolutePath());
  }

  @FXML
  void saveOptions() {

  }

  @FXML
  void cancel() {
    appContext.publishEvent(new StageActionEvent(new Payload("cancelFromOptions")));
  }

}
