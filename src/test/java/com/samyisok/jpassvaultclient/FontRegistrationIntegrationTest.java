package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javafx.application.Platform;
import javafx.scene.text.Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link FontLoader} against a real toolkit so the bundled Open Sans
 * resources are actually resolved. Needs a display: locally {@code DISPLAY}, in
 * CI {@code xvfb-run}.
 */
class FontRegistrationIntegrationTest {

  @BeforeAll
  static void bootToolkitOnce() {
    Platform.setImplicitExit(false);
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException alreadyRunning) {
      // another toolkit test started it; one JVM, one toolkit
    }
  }

  @Test
  @DisplayName("register loads the bundled Open Sans family and returns its name")
  void registerLoadsBundledFamily() {
    String family = FontLoader.register();

    assertEquals(FontLoader.FAMILY, family);
  }

  @Test
  @DisplayName("a Font resolves from the registered family instead of a toolkit fallback")
  void familyResolvesToBundledFont() {
    FontLoader.register();

    Font font = Font.font(FontLoader.family(), 18);

    assertEquals(FontLoader.family(), font.getFamily());
    assertFalse(font.getFamily().isBlank());
  }
}
