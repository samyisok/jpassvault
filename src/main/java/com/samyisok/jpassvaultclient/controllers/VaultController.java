package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.password.PasswordGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class VaultController implements Initializable {

  public static final String FXML_PATH = "/com/samyisok/jpassvaultclient/vault.fxml";

  private final EventPublisher eventPublisher;
  private final Vault vault;
  private final VaultLoader vaultLoader;
  private final PasswordGenerator passwordGenerator;

  @FXML
  ListView<String> listVault = new ListView<String>();

  @FXML
  TextField nameView;

  @FXML
  TextField loginView;

  @FXML
  PasswordField passwordView;

  @FXML
  TextField passwordViewTextField;

  @FXML
  TextField nameCreate;

  @FXML
  TextField loginCreate;

  @FXML
  PasswordField passwordCreate;

  @FXML
  TextField passwordCreateTextField;

  @FXML
  TextField searchViewByName;

  @FXML
  CheckBox showPasswordViewCheckBox;

  @FXML
  CheckBox showPasswordCreateCheckBox;

  public VaultController(EventPublisher eventPublisher, Vault vault,
      VaultLoader vaultLoader, PasswordGenerator passwordGenerator) {
    this.eventPublisher = eventPublisher;
    this.vault = vault;
    this.vaultLoader = vaultLoader;
    this.passwordGenerator = passwordGenerator;
  }

  @FXML
  void close() throws IOException {
    eventPublisher.publish(EventAction.LOCK);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

    passwordCreate.textProperty().addListener((observableValue, oldValue, newValue) -> {
      passwordCreateTextField.setText(newValue);
    });

    passwordCreateTextField.textProperty()
        .addListener((observableValue, oldValue, newValue) -> {
          passwordCreate.setText(newValue);
        });


    passwordView.textProperty().addListener((observableValue, oldValue, newValue) -> {
      passwordViewTextField.setText(newValue);
    });

    passwordViewTextField.textProperty()
        .addListener((observableValue, oldValue, newValue) -> {
          passwordView.setText(newValue);
        });


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
    passwordView.setText(vaultContainer.getPassword());
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
      nameView.setText(null);
      loginView.setText(null);
      passwordView.setText(null);
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
    updateSelector();
  }

  @FXML
  void showPasswordView() {
    passwordView.setVisible(!showPasswordViewCheckBox.isSelected());
    passwordViewTextField.setVisible(showPasswordViewCheckBox.isSelected());;
  }

  @FXML
  void showPasswordCreate() {
    passwordCreate.setVisible(!showPasswordCreateCheckBox.isSelected());
    passwordCreateTextField.setVisible(showPasswordCreateCheckBox.isSelected());
  }

  @FXML
  void generatePassword() {
    passwordCreate.setText(passwordGenerator.getPassword());
  }
}
