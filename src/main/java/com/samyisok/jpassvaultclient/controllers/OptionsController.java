package com.samyisok.jpassvaultclient.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.StageHolder;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.options.OptionsLoader;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class OptionsController implements Initializable {

  public static final String FXML_PATH = "/com/samyisok/jpassvaultclient/options.fxml";

  private final EventPublisher eventPublisher;
  private final Options options;
  private final OptionsLoader optionsLoader;
  private final StageHolder stageHolder;
  private final RemoteVault remoteVault;

  @FXML
  TextField databasePathField;

  @FXML
  TextField apiUrlField;

  @FXML
  TextField tokenField;

  public OptionsController(EventPublisher eventPublisher, Options options,
      OptionsLoader optionsLoader, StageHolder stageHolder, RemoteVault remoteVault) {
    this.eventPublisher = eventPublisher;
    this.options = options;
    this.optionsLoader = optionsLoader;
    this.stageHolder = stageHolder;
    this.remoteVault = remoteVault;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    update();
  }

  @FXML
  void chooseDb() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

    File selectedDirectory = directoryChooser.showDialog(stageHolder.getStage());

    if(selectedDirectory != null){
      options.setPathVaultWithDefaultName(selectedDirectory.getAbsolutePath());
      databasePathField.setText(options.getPathVault());
    }
  }

  void update() {
    databasePathField.setText(options.getPathVault());
    apiUrlField.setText(options.getApiUrl());
    tokenField.setText(options.getTokenApi());
  }

  @FXML
  void saveOptions() {
    options.setApiUrl(apiUrlField.getText());
    options.setTokenApi(tokenField.getText());
    options.setPathVault(databasePathField.getText());
    optionsLoader.save(options);
    eventPublisher.publish(EventAction.CANCEL_FROM_OPTIONS);
  }

  @FXML
  void cancel() {
    eventPublisher.publish(EventAction.CANCEL_FROM_OPTIONS);
  }

  @FXML
  void checkRemoteDb() {
    try {
      // TODO make visual loader bar
      boolean status =
          remoteVault.checkHostAndToken(apiUrlField.getText(), tokenField.getText());
      if (status == true) {
        info("Check remote db", "Data is valid");
      }
    } catch (Exception e) {
      warning("Check Failed", "Reason: " + e.getMessage());
    }
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }

  void info(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle("Info");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }

}
