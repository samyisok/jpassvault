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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

@Component
public class MainListener implements ApplicationListener<StageActionEvent> {

  @Autowired
  StageHolder stageHolder;

  @Autowired
  VaultLoader vaultLoader;

  @Autowired
  FxWeaver fxWeaver;

  @Autowired
  RemoteVault remoteVault;

  @Autowired
  Options options;

  @Override
  public void onApplicationEvent(StageActionEvent event) {
    Stage stage = stageHolder.getStage();

    switch (event.getEvent().getAction()) {
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
        stage.setScene(new Scene(fxWeaver.loadView(VaultController.class)));
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
        stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
        break;

      case OPTIONS:
        stage.setScene(new Scene(fxWeaver.loadView(OptionsController.class)));
        break;

      case CANCEL_FROM_OPTIONS:
        if (vaultLoader.ifDbExists()) {
          vaultLoader.unload();
          stage.setScene(new Scene(fxWeaver.loadView(MainController.class)));
        } else {
          stage.setScene(new Scene(fxWeaver.loadView(SetupController.class)));
        }
        break;

      default:
        break;
    }
  }

}
