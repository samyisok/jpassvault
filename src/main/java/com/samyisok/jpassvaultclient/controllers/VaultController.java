package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.models.Vault;
import com.samyisok.jpassvaultclient.models.VaultContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;


@Component
@FxmlView("/com/samyisok/jpassvaultclient/vault.fxml")
public class VaultController implements Initializable {

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  Vault vault;

  @FXML
  ListView<String> listVault = new ListView<String>();


  @FXML
  TextField nameView;

  @FXML
  TextField loginView;

  @FXML
  PasswordField passwordView;

  @FXML
  TextField nameCreate;

  @FXML
  TextField loginCreate;

  @FXML
  PasswordField passwordCreate;


  @FXML
  void close() throws IOException {
    System.out.println("listVault: " + listVault.getItems());
    appContext.publishEvent(new StageActionEvent(new Payload("exit")));
  }


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    updateSelector();
  }

  void updateSelector() {
    ObservableList<String> vaultElems =
        FXCollections.observableArrayList(vault.keySet());
    listVault.getItems().clear();
    listVault.getItems().addAll(vaultElems);
  }

  @FXML
  void onClick() {
    String item = listVault.getSelectionModel().getSelectedItem();
    System.out.println(item);

    VaultContainer vaultContainer = vault.get(item);
    nameView.setText(item);
    loginView.setText(vaultContainer.getLogin());
    passwordView.setText(vaultContainer.getLogin());
  }

  @FXML
  void create() {
    if (nameCreate.getText().isEmpty() || passwordCreate.getText().isEmpty()) {
      System.out.println(nameCreate.getText());
      warning("Can't create record", "Fields is empty");
      return;
    }

    VaultContainer item =
        new VaultContainer(loginCreate.getText(), passwordCreate.getText());
    vault.put(nameCreate.getText(), item);
    System.out.println(item);
    updateSelector();
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);

    alert.showAndWait();
  }
}
