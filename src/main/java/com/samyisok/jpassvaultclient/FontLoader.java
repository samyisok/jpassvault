package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.io.InputStream;
import javafx.scene.text.Font;

/**
 * Registers the single bundled UI typeface with the JavaFX toolkit so every view
 * can refer to it by family, independent of the fonts installed on the host.
 */
public final class FontLoader {

  static final String REGULAR_RESOURCE =
      "/com/samyisok/jpassvaultclient/fonts/OpenSans-Regular.ttf";
  static final String BOLD_RESOURCE =
      "/com/samyisok/jpassvaultclient/fonts/OpenSans-Bold.ttf";

  /** Family name the application stylesheets refer to. */
  public static final String FAMILY = "Open Sans";

  private static String family;

  private FontLoader() {
  }

  /**
   * Loads the bundled faces once and returns the registered family name.
   *
   * @throws IllegalStateException if a bundled face is missing or unusable
   */
  public static synchronized String register() {
    if (family == null) {
      Font regular = load(REGULAR_RESOURCE);
      load(BOLD_RESOURCE);
      family = regular.getFamily();
      if (!FAMILY.equals(family)) {
        throw new IllegalStateException("Unexpected bundled font family: " + family);
      }
    }
    return family;
  }

  /** Family name of the bundled typeface, registering it on first use. */
  public static String family() {
    return register();
  }

  private static Font load(String resource) {
    try (InputStream in = FontLoader.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Bundled font missing: " + resource);
      }
      Font font = Font.loadFont(in, 12.0);
      if (font == null) {
        throw new IllegalStateException("Bundled font unreadable: " + resource);
      }
      return font;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read bundled font: " + resource, e);
    }
  }
}
