package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.domains.vault.RecordSearch;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.password.PasswordGenerator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class VaultController implements Initializable {

  public static final String FXML_PATH = "/com/samyisok/jpassvaultclient/vault.fxml";

  private final EventPublisher eventPublisher;
  private final Vault vault;
  private final VaultLoader vaultLoader;
  private final PasswordGenerator passwordGenerator;
  private final ClipboardAutoClear clipboardAutoClear;

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
      VaultLoader vaultLoader, PasswordGenerator passwordGenerator,
      ClipboardAutoClear clipboardAutoClear) {
    this.eventPublisher = eventPublisher;
    this.vault = vault;
    this.vaultLoader = vaultLoader;
    this.passwordGenerator = passwordGenerator;
    this.clipboardAutoClear = clipboardAutoClear;
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

  /**
   * Refills the list from the Search box text: case-insensitive substring
   * match, alphabetical, so the list stops reshuffling between keystrokes.
   */
  void updateSelector() {
    fillList(RecordSearch.matches(vault.keySet(), searchViewByName.getText()));
  }

  /** Replaces the visible records; the selection is re-applied afterwards. */
  private void fillList(Collection<String> visibleNames) {
    listVault.getItems().setAll(visibleNames);
  }

  @FXML
  void onClick() {
    String item = listVault.getSelectionModel().getSelectedItem();
    if (item == null) {
      return;
    }

    showRecord(item);
  }

  /** Loads a stored record into the view pane; shared by click and create. */
  private void showRecord(String name) {
    VaultContainer vaultContainer = vault.get(name);
    nameView.setText(name);
    loginView.setText(vaultContainer.getLogin());
    passwordView.setText(vaultContainer.getPassword());
  }

  @FXML
  void create() {
    if (nameCreate.getText().isEmpty() || passwordCreate.getText().isEmpty()) {
      warning("Can't create record", "Fields is empty");
      return;
    }

    String createdName = nameCreate.getText();
    if (vault.containsKey(createdName)) {
      warning("Can't create record", "Record already exists");
      return;
    }

    vault.put(createdName,
        new VaultContainer(loginCreate.getText(), passwordCreate.getText()));
    applyState(RecordSearch.afterCreate(vault.keySet(), createdName));
    showRecord(createdName);
    vaultLoader.save(vault);
    nameCreate.setText(null);
    loginCreate.setText(null);
    passwordCreate.setText(null);
  }

  /**
   * Shows the post-create screen state: the created name replaces whatever
   * filter text was there, the list is refreshed from it, then the created
   * record is selected — order matters, because refreshing clears the selection.
   */
  private void applyState(RecordSearch.PostCreateState state) {
    searchViewByName.setText(state.filterText());
    fillList(state.visibleNames());
    listVault.getSelectionModel().select(state.selectedName());
  }

  @FXML
  void delete() {
    String item = listVault.getSelectionModel().getSelectedItem();
    if (item == null || item.isEmpty()) {
      warning("Can't delete record", "Dothing to delete");
      return;
    }

    if (confirm("Delete record?", "Data would be permanently deleted!")) {
      vault.remove(item);
      updateSelector();
      vaultLoader.save(vault);
    }
  }

  /**
   * Modal warning. Overridable so a test can record the message instead of
   * blocking forever in {@code showAndWait()}; production behaviour unchanged.
   */
  protected void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);

    alert.showAndWait();
  }

  /**
   * Modal confirmation, the dialog {@code save()} and {@code delete()} used to
   * open inline. Overridable for the same reason as {@link #warning}.
   *
   * @return true when the user confirmed
   */
  protected boolean confirm(String title, String content) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setContentText(content);
    Optional<ButtonType> result = alert.showAndWait();

    return result.isPresent() && result.get() == ButtonType.OK;
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

    if (confirm("Save record?", "Data would be permanently changed!")) {
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
    clipboardAutoClear.copy(passwordView.getText());
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
