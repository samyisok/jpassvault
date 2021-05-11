package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.MergeVaultException;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteException;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/main.fxml")
public class MainController {

  @Autowired
  private ApplicationContext appContext;

  @FXML
  PasswordField unlockPassword;

  @Autowired
  VaultLoader vaultLoader;

  @Autowired
  Session session;

  @Autowired
  RemoteVault remoteVault;

  @FXML
  void unlock() throws IOException {
    // TODO auto lock
    validatePassword();
  }

  void validatePassword() {
    String password = unlockPassword.getText();
    session.setPasswordVault(password);

    if (vaultLoader.vaultPasswordIsValid()) {
      try {
        if ( remoteVault.isAvailible() ) {
          remoteVault.load();
        } else {
          warning("Remote vault is not availible", "Please check internet connection or remote vault settings.");
        }
      } catch (URISyntaxException | RemoteException | MergeVaultException e) {
        warning("Error when loading from remote location:" + e.getMessage(),
            e.toString());
      }
      appContext.publishEvent(new StageActionEvent(new Payload(EventAction.UNLOCK)));
    } else {
      session.setPasswordVault(null);
      warning("Wrong password!", "Try again!");
    }
  }

  @FXML
  void options() {
    appContext.publishEvent(new StageActionEvent(new Payload(EventAction.OPTIONS)));
  }

  void warning(String headerMessage, String message) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(headerMessage);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
