package com.samyisok.jpassvaultclient.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.StageHolder;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.options.OptionsLoader;
import com.samyisok.jpassvaultclient.EventAction;
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
  Options options;

  @Autowired
  OptionsLoader optionsLoader;

  @Autowired
  StageHolder stageHolder;

  @FXML
  TextField databasePathField;

  @FXML
  TextField apiUrlField;

  @FXML
  TextField tokenField;


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    System.out.println(options.toString());
    update();
  }


  @FXML
  void chooseDb() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

    File selectedDirectory = directoryChooser.showDialog(stageHolder.getStage());

    options.setPathVaultWithDefaultName(selectedDirectory.getAbsolutePath());
    databasePathField.setText(options.getPathVault());
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
    appContext.publishEvent(
        new StageActionEvent(new Payload(EventAction.CANCEL_FROM_OPTIONS)));
  }

  @FXML
  void cancel() {
    appContext.publishEvent(
        new StageActionEvent(new Payload(EventAction.CANCEL_FROM_OPTIONS)));
  }

}
