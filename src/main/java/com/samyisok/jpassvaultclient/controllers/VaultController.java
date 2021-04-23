package com.samyisok.jpassvaultclient.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import com.samyisok.jpassvaultclient.StageActionEvent;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;
import com.samyisok.jpassvaultclient.models.Vault;
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

  @Autowired
  Vault vault;

  @FXML
  ListView<String> listVault = new ListView<String>();


  @FXML
  private void close() throws IOException {
    System.out.println("listVault: " + listVault.getItems());
    appContext.publishEvent(new StageActionEvent(new Payload("exit")));
  }


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    ObservableList<String> vaultElems =
        FXCollections.observableArrayList(vault.keySet());
    listVault.getItems().addAll(vaultElems);
  }
}
