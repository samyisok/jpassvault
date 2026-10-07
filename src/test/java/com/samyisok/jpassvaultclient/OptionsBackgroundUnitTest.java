package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the Options stage's distinct white form background: the root element
 * itself must declare it, so the view does not depend on the global scene fill.
 */
class OptionsBackgroundUnitTest {

  private static final String OPTIONS_FXML =
      "/com/samyisok/jpassvaultclient/options.fxml";
  private static final String WHITE = "-fx-background-color: #ffffff";

  private String rootStartTag;

  @BeforeEach
  void extractRootStartTag() throws Exception {
    String fxml = read(OPTIONS_FXML);
    int start = fxml.indexOf("<BorderPane");
    rootStartTag = fxml.substring(start, fxml.indexOf('>', start) + 1);
  }

  @Test
  @DisplayName("the options root declares an explicit white background")
  void optionsRootDeclaresWhiteBackground() {
    assertTrue(rootStartTag.contains(WHITE), rootStartTag);
  }

  private static String read(String resource) throws Exception {
    try (InputStream in = OptionsBackgroundUnitTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
