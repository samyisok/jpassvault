package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/vault.fxml")
public class VaultController implements Initializable {

  @Autowired
  private ApplicationContext appContext;

  ObservableList<String> vaultElems = FXCollections.observableArrayList(
      "pornhub", "MAL", "animechart", "bash.org.ru", "yandex", "google", "pornhub",
      "MAL", "animechart", "bash.org.ru", "yandex", "google", "pornhub", "MAL",
      "animechart", "bash.org.ru", "yandex", "google");

  @FXML
  ListView<String> listVault = new ListView<String>();


  @FXML
  private void close() throws IOException {
    System.out.println("listVault: " + listVault.getItems());
    appContext.publishEvent(new StageActionEvent(new Payload("exit")));
  }


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    listVault.getItems().addAll(vaultElems);
  }
}
