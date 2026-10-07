package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Loads the real views through {@link ViewLoader} and checks the rendered
 * result: every scene inherits the bundled typeface and the Options scene
 * paints its own white background. Needs a display, as the app itself does.
 */
class ViewThemeIntegrationTest {

  private static final List<String> VIEWS = List.of(
      "/com/samyisok/jpassvaultclient/main.fxml",
      "/com/samyisok/jpassvaultclient/setup.fxml",
      "/com/samyisok/jpassvaultclient/options.fxml",
      "/com/samyisok/jpassvaultclient/vault.fxml");

  private ViewLoader viewLoader;

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

  @BeforeEach
  void registerFontAndLoader() {
    FontLoader.register();
    viewLoader = new ViewLoader(type -> mock(type));
  }

  @Test
  @DisplayName("every view inherits the bundled Open Sans family from the shared stylesheet")
  void everyViewInheritsBundledTypeface() {
    for (String view : VIEWS) {
      Scene scene = load(view);
      Region frame = (Region) scene.getRoot();
      onFx(() -> {
        frame.applyCss();
        frame.layout();
        return null;
      });
      Labeled control = (Labeled) frame.lookup(".button");
      assertNotNull(control, view);
      assertEquals("Open Sans", control.getFont().getFamily(), view);
    }
  }

  @Test
  @DisplayName("the rendered options scene paints its own white background")
  void optionsSceneRendersWhiteBackground() {
    Scene scene = load("/com/samyisok/jpassvaultclient/options.fxml");
    Parent root = scene.getRoot();

    onFx(() -> {
      root.applyCss();
      root.layout();
      return null;
    });

    Region content = (Region) ((StackPane) root).getChildren().get(0);
    assertNotNull(content.getBackground());
    assertEquals(Color.WHITE, content.getBackground().getFills().get(0).getFill());
  }

  private Scene load(String fxml) {
    return onFx(() -> viewLoader.loadScene(fxml));
  }

  private static <T> T onFx(Body<T> body) {
    try {
      FutureTask<T> task = new FutureTask<>(body::run);
      Platform.runLater(task);
      return task.get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException("JavaFX action failed", e);
    }
  }

  private interface Body<T> {
    T run() throws Exception;
  }
}
