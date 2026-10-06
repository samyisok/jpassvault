package com.samyisok.jpassvaultclient.controllers;

import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;

public class SetupController {

  public static final String FXML_PATH = "/com/samyisok/jpassvaultclient/setup.fxml";

  /** Minimum length for a newly created master password (creation only). */
  static final int MIN_PASSWORD_LENGTH = 8;

  private final EventPublisher eventPublisher;
  private final VaultLoader vaultLoader;
  private final Session session;

  @FXML
  PasswordField createPassword1;

  @FXML
  PasswordField createPassword2;

  public SetupController(EventPublisher eventPublisher, VaultLoader vaultLoader,
      Session session) {
    this.eventPublisher = eventPublisher;
    this.vaultLoader = vaultLoader;
    this.session = session;
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }

  @FXML
  void createDb() {
    if (!createPassword1.getText().equals(createPassword2.getText())) {
      warning("Password does not match!", "Specify correct password");
      return;
    }
    if (createPassword1.getText().length() < MIN_PASSWORD_LENGTH) {
      warning("Password invalid",
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
      return;
    }

    session.setPasswordVault(createPassword1.getText());
    vaultLoader.createEmptyDbIfNotExist();
    session.setPasswordVault(null);
    eventPublisher.publish(EventAction.LOCK);
  }

  @FXML
  void options() {
    eventPublisher.publish(EventAction.OPTIONS);
  }
}
