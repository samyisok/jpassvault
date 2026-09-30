package com.samyisok.jpassvaultclient;

import java.net.URISyntaxException;
import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.OptionsController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.controllers.VaultController;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.vault.MergeVaultException;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteException;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import javafx.stage.Stage;

/**
 * Handles {@link StageActionEvent}s fired on the stage and swaps scenes.
 * Replaces the former Spring {@code ApplicationListener}.
 */
public class MainListener {

  private final StageHolder stageHolder;
  private final VaultLoader vaultLoader;
  private final ViewLoader viewLoader;
  private final RemoteVault remoteVault;
  private final Options options;

  public MainListener(StageHolder stageHolder, VaultLoader vaultLoader,
      ViewLoader viewLoader, RemoteVault remoteVault, Options options) {
    this.stageHolder = stageHolder;
    this.vaultLoader = vaultLoader;
    this.viewLoader = viewLoader;
    this.remoteVault = remoteVault;
    this.options = options;
  }

  public void handle(StageActionEvent event) {
    Stage stage = stageHolder.getStage();

    switch (event.getAction()) {
      case UNLOCK:
        vaultLoader.load();
        if (options.ifOnlineSyncOn()) {
          try {
            if (remoteVault.isAvailible()) {
              remoteVault.load();
            }
          } catch (URISyntaxException | RemoteException | MergeVaultException e1) {
            e1.printStackTrace();
          }
        }
        stage.setScene(viewLoader.loadScene(VaultController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case LOCK:
        if (options.ifOnlineSyncOn()) {
          try {
            remoteVault.save();
          } catch (URISyntaxException | RemoteException | EncryptionException e) {
            System.out.println("SAVE EXCEPTION: " + e.getMessage());
          }
        }
        vaultLoader.unload();
        stage.setScene(viewLoader.loadScene(MainController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case OPTIONS:
        stage.setScene(viewLoader.loadScene(OptionsController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case CANCEL_FROM_OPTIONS:
        if (vaultLoader.ifDbExists()) {
          vaultLoader.unload();
          stage.setScene(viewLoader.loadScene(MainController.FXML_PATH, ViewLoader.of(stage)));
        } else {
          stage.setScene(viewLoader.loadScene(SetupController.FXML_PATH, ViewLoader.of(stage)));
        }
        break;

      default:
        break;
    }
  }

}
