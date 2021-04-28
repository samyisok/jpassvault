package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import net.rgielen.fxweaver.core.FxmlView;


@Component
@FxmlView("/com/samyisok/jpassvaultclient/vault.fxml")
public class VaultController implements Initializable {

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  Vault vault;

  @Autowired
  VaultLoader vaultLoader;

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
  TextField searchViewByName;


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
    Set<String> filteredSet = vault.keySet().stream().filter(
        el -> el.toLowerCase().contains(searchViewByName.getText().toLowerCase()))
        .collect(Collectors.toSet());
    ObservableList<String> vaultElems =
        FXCollections.observableArrayList(filteredSet);
    listVault.getItems().clear();
    listVault.getItems().addAll(vaultElems);
  }

  @FXML
  void onClick() {
    String item = listVault.getSelectionModel().getSelectedItem();
    if (item == null) {
      return;
    }

    VaultContainer vaultContainer = vault.get(item);
    nameView.setText(item);
    loginView.setText(vaultContainer.getLogin());
    passwordView.setText(vaultContainer.getLogin());
  }

  @FXML
  void create() {
    if (nameCreate.getText().isEmpty() || passwordCreate.getText().isEmpty()) {
      warning("Can't create record", "Fields is empty");
      return;
    }

    VaultContainer item =
        new VaultContainer(loginCreate.getText(), passwordCreate.getText());
    vault.put(nameCreate.getText(), item);
    updateSelector();
    vaultLoader.save(vault);
    nameCreate.setText(null);
    loginCreate.setText(null);
    passwordCreate.setText(null);
  }

  @FXML
  void delete() {
    String item = listVault.getSelectionModel().getSelectedItem();
    if (item == null || item.isEmpty()) {
      warning("Can't delete record", "Dothing to delete");
      return;
    }

    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Delete record?");
    alert.setContentText("Data would be permanently deleted!");
    Optional<ButtonType> result = alert.showAndWait();

    if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
      vault.remove(item);
      updateSelector();
      vaultLoader.save(vault);
    }
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);

    alert.showAndWait();
  }

  @FXML
  void save() {
    String item = listVault.getSelectionModel().getSelectedItem();
    if (item == null || item.isEmpty()) {
      if (vault.containsKey(nameView.getText())) {
        item = nameView.getText();
      } else {
        warning("Can't save record", "Dothing to save");
        return;
      }
    }

    if (nameView.getText().isEmpty() || passwordView.getText().isEmpty()) {
      warning("Can't save record", "Fields is empty");
      return;
    }

    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Save record?");
    alert.setContentText("Data would be permanently changed!");
    Optional<ButtonType> result = alert.showAndWait();

    if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
      VaultContainer newVaultContainer =
          new VaultContainer(loginView.getText(), passwordView.getText());
      String name = nameView.getText();
      vault.remove(item);
      vault.put(name, newVaultContainer);
      updateSelector();
      vaultLoader.save(vault);
    }
  }

  @FXML
  void copy() {
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(passwordView.getText());
    clipboard.setContent(content);
  }

  @FXML
  void search() {
    System.out.println(searchViewByName.getText());
    updateSelector();
  }
}
