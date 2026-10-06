package com.samyisok.jpassvaultclient;

import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.OptionsController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.controllers.VaultController;
import javafx.stage.Stage;

/**
 * Handles {@link StageActionEvent}s fired on the stage and swaps scenes. The
 * vault lifecycle side effects live in {@link VaultLifecycleCoordinator}
 * (design D16); this class only routes events to scenes.
 */
public class MainListener {

  private final StageHolder stageHolder;
  private final ViewLoader viewLoader;
  private final VaultLifecycleCoordinator lifecycle;

  public MainListener(StageHolder stageHolder, ViewLoader viewLoader,
      VaultLifecycleCoordinator lifecycle) {
    this.stageHolder = stageHolder;
    this.viewLoader = viewLoader;
    this.lifecycle = lifecycle;
  }

  public void handle(StageActionEvent event) {
    Stage stage = stageHolder.getStage();

    switch (event.getAction()) {
      case UNLOCK:
        lifecycle.unlock();
        stage.setScene(viewLoader.loadScene(VaultController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case LOCK:
        lifecycle.lock();
        stage.setScene(viewLoader.loadScene(MainController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case OPTIONS:
        stage.setScene(viewLoader.loadScene(OptionsController.FXML_PATH, ViewLoader.of(stage)));
        break;

      case CANCEL_FROM_OPTIONS:
        if (lifecycle.hasVault()) {
          lifecycle.discard();
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
