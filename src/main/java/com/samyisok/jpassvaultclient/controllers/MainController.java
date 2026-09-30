package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;

public class MainController {

  public static final String FXML_PATH = "/com/samyisok/jpassvaultclient/main.fxml";

  private final EventPublisher eventPublisher;
  private final VaultLoader vaultLoader;
  private final Session session;
  private final RemoteVault remoteVault;
  private final Options options;

  @FXML
  PasswordField unlockPassword;

  public MainController(EventPublisher eventPublisher, VaultLoader vaultLoader,
      Session session, RemoteVault remoteVault, Options options) {
    this.eventPublisher = eventPublisher;
    this.vaultLoader = vaultLoader;
    this.session = session;
    this.remoteVault = remoteVault;
    this.options = options;
  }

  @FXML
  void unlock() throws IOException {
    // TODO auto lock
    validatePassword();
  }

  void validatePassword() {
    String password = unlockPassword.getText();
    session.setPasswordVault(password);

    if (vaultLoader.vaultPasswordIsValid()) {
      if (options.ifOnlineSyncOn()) {
        if (!remoteVault.isAvailible()) {
          warning("Remote vault is not availible",
              "Please check internet connection or remote vault settings.");
        }
      }
      eventPublisher.publish(EventAction.UNLOCK);
    } else {
      session.setPasswordVault(null);
      warning("Wrong password!", "Try again!");
    }
  }

  @FXML
  void options() {
    eventPublisher.publish(EventAction.OPTIONS);
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
