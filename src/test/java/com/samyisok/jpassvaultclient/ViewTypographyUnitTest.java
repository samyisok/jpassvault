package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the single-source-of-truth rule for the UI font: no view or stylesheet
 * may name a platform-specific typeface, and the bundled family must be the one
 * the shared stylesheet declares.
 */
class ViewTypographyUnitTest {

  private static final String APP_CSS = "/com/samyisok/jpassvaultclient/app.css";
  private static final String VAULT_CSS = "/com/samyisok/jpassvaultclient/vault.css";

  private static final List<String> VIEW_RESOURCES = List.of(
      "/com/samyisok/jpassvaultclient/main.fxml",
      "/com/samyisok/jpassvaultclient/setup.fxml",
      "/com/samyisok/jpassvaultclient/options.fxml",
      "/com/samyisok/jpassvaultclient/vault.fxml");

  private static final List<String> STYLESHEET_RESOURCES = List.of(
      "/com/samyisok/jpassvaultclient/buttons.css", VAULT_CSS, APP_CSS);

  private static final List<String> RESOURCES =
      Stream.concat(VIEW_RESOURCES.stream(), STYLESHEET_RESOURCES.stream()).toList();

  private final Map<String, String> sources = new HashMap<>();

  @BeforeEach
  void loadSources() throws Exception {
    for (String resource : RESOURCES) {
      sources.put(resource, read(resource));
    }
  }

  @Test
  @DisplayName("no view or stylesheet names a platform-specific font")
  void noPlatformFontNames() {
    RESOURCES.forEach(resource -> assertFalse(sources.get(resource).contains("Segoe"), resource));
  }

  @Test
  @DisplayName("views pin neither a font family nor a size per control")
  void viewsDoNotPinFonts() {
    VIEW_RESOURCES.forEach(resource -> assertFalse(sources.get(resource).contains("<Font"), resource));
  }

  @Test
  @DisplayName("the shared stylesheet declares the bundled Open Sans family")
  void appStylesheetDeclaresBundledFamily() {
    assertTrue(sources.get(APP_CSS).contains(FontLoader.FAMILY));
  }

  @Test
  @DisplayName("the vault list stylesheet uses the bundled Open Sans family")
  void vaultListUsesBundledFamily() {
    assertTrue(sources.get(VAULT_CSS).contains(FontLoader.FAMILY));
  }

  private static String read(String resource) throws Exception {
    try (InputStream in = ViewTypographyUnitTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
