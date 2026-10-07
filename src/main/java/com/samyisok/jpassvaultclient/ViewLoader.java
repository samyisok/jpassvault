package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Loads FXML views with controllers supplied by the application composition
 * root. Replaces the former javafx-weaver {@code FxWeaver.loadView}.
 */
public class ViewLoader {

  private static final double DEFAULT_WIDTH = 1000.0;
  private static final double DEFAULT_HEIGHT = 600.0;

  /**
   * Application background colour, matching the {@code -fx-background-color}
   * the FXML views use for their panels, so the area exposed when the window is
   * larger than the view is not left in JavaFX's default white.
   */
  static final String APP_BACKGROUND = "#beb";

  /** Shared stylesheet that carries the bundled application typeface. */
  static final String APP_STYLESHEET = "/com/samyisok/jpassvaultclient/app.css";

  /** Inline style for the frame behind a pinned view. */
  static String frameStyle() {
    return "-fx-background-color: " + APP_BACKGROUND + ";";
  }

  private final Function<Class<?>, Object> controllerProvider;

  public ViewLoader(Function<Class<?>, Object> controllerProvider) {
    this.controllerProvider = controllerProvider;
  }

  public Parent load(String fxmlPath) {
    return loadRoot(fxmlPath);
  }

  /**
   * Loads an FXML view and wraps it in a {@link Scene} at the view's designed
   * size, centred inside a resizable window.
   *
   * <p>Two independent problems are handled here. {@code new Scene(root)}
   * delegates with width/height {@code -1}, leaving the scene size
   * uninitialized so the window manager picks the geometry; and a view whose
   * root is free to grow stretches its children across whatever size the
   * window ends up. The scene is therefore sized from the root's preferred
   * size, the root is pinned to that size so controls keep their designed
   * geometry, and a resizable frame becomes the scene root so the window can
   * still be resized without distorting the content.
   */
  public Scene loadScene(String fxmlPath) {
    return loadScene(fxmlPath, null);
  }

  /**
   * Loads a view, sized to {@code currentStageSize} when the window already
   * exists, otherwise to the view's designed size.
   *
   * <p>A {@link Scene} carries its own size, and setting a scene on a stage
   * makes the stage adopt it. Passing the designed size on every swap would
   * therefore undo any resize the user made. Keeping the stage's current size
   * means the app holds still until the user resizes the window themselves.
   */
  public Scene loadScene(String fxmlPath, SceneSize currentStageSize) {
    Region content = (Region) loadRoot(fxmlPath);
    SceneSize designed = sizeFor(content);
    SceneSize size = retainOrDesigned(currentStageSize, designed);
    boolean grows = grows(fxmlPath);
    if (grows) {
      growToWindow(content, designed);
    } else {
      pinToPreferred(content, designed);
    }
    StackPane frame = framed(content, grows);
    Scene scene = new Scene(frame, size.width(), size.height());
    addApplicationStylesheet(scene);
    scene.setFill(Color.web(APP_BACKGROUND));
    return scene;
  }

  /**
   * Applies the shared application stylesheet, which carries the bundled UI
   * typeface, so every view inherits one consistent font family.
   *
   * @throws IllegalStateException if the stylesheet is not on the classpath
   */
  static void addApplicationStylesheet(Scene scene) {
    URL url = ViewLoader.class.getResource(APP_STYLESHEET);
    if (url == null) {
      throw new IllegalStateException("Missing application stylesheet: " + APP_STYLESHEET);
    }
    scene.getStylesheets().add(url.toExternalForm());
  }

  /**
   * Whether a view should fill the window instead of staying at its designed
   * size.
   *
   * <p>The unlock and vault views grow: the unlock view anchors its Options
   * button to the bottom-right corner, and the vault view keeps its list in the
   * {@code BorderPane} centre, where it can usefully fill the extra space. The
   * setup and options views are fixed forms whose controls are absolutely
   * positioned, so growing them would only stretch empty background.
   */
  static boolean grows(String fxmlPath) {
    return fxmlPath.endsWith("main.fxml") || fxmlPath.endsWith("vault.fxml");
  }

  /**
   * Lets a view fill the frame, keeping its designed size as the starting
   * point. Clearing the {@code -Infinity} bounds lets the region track the
   * window, and the minimum keeps it from collapsing.
   */
  static void growToWindow(Region root, SceneSize designed) {
    root.setMinWidth(0);
    root.setMinHeight(0);
    root.setMaxWidth(Double.MAX_VALUE);
    root.setMaxHeight(Double.MAX_VALUE);
    root.setPrefWidth(designed.width());
    root.setPrefHeight(designed.height());
  }

  /** Keeps an existing window size, or falls back to the designed size. */
  static SceneSize retainOrDesigned(SceneSize currentStageSize, SceneSize designed) {
    return currentStageSize != null ? currentStageSize : designed;
  }

  /** Current geometry of a stage, so a scene swap can keep it. */
  public static SceneSize of(Stage stage) {
    return new SceneSize(stage.getWidth(), stage.getHeight());
  }

  /** A scene size for an explicit width and height. */
  public static SceneSize ofSize(double width, double height) {
    return new SceneSize(width, height);
  }

  private static StackPane framed(Region content, boolean grows) {
    StackPane frame = new StackPane(content);
    frame.setAlignment(content, grows ? Pos.TOP_LEFT : Pos.CENTER);
    frame.setStyle(frameStyle());
    return frame;
  }

  private Parent loadRoot(String fxmlPath) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    loader.setControllerFactory(controllerProvider::apply);
    try {
      return loader.load();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load view " + fxmlPath, e);
    }
  }

  /**
   * Pins a view to its designed size on both axes.
   *
   * <p>Scene Builder writes {@code min/maxWidth = -Infinity} on FXML roots,
   * which lets the region grow with the window. Overriding all four bounds
   * with the same value keeps the laid-out controls (text fields, buttons)
   * exactly as the FXML designed them.
   */
  static void pinToPreferred(Region root, SceneSize size) {
    root.setMinWidth(size.width());
    root.setMinHeight(size.height());
    root.setMaxWidth(size.width());
    root.setMaxHeight(size.height());
  }

  /** Preferred size of the view, falling back to 1000x600 when unusable. */
  static SceneSize sizeFor(Region root) {
    return new SceneSize(usableOr(root.prefWidth(-1), DEFAULT_WIDTH),
        usableOr(root.prefHeight(-1), DEFAULT_HEIGHT));
  }

  private static double usableOr(double preferred, double fallback) {
    return Double.isFinite(preferred) && preferred > 0 ? preferred : fallback;
  }

  /** Scene size in pixels. */
  public record SceneSize(double width, double height) {
  }
}
