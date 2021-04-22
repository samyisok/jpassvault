package com.samyisok.jpassvaultclient;

import java.io.IOException;

import com.samyisok.jpassvaultclient.MainApplication.StageReadyEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.samyisok.jpassvaultclient.StageActionEvent.Payload;

import javafx.fxml.FXML;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/com/samyisok/jpassvaultclient/primary.fxml")
public class MainController {

  @Autowired
  private ApplicationContext appContext;


  @FXML
  private void switchToSecondary() throws IOException {
    appContext.publishEvent(new StageActionEvent(new Payload("vault")));
  }
}
