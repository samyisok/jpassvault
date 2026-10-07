package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ViewLoaderUnitTest {

  private final Region root = mock(Region.class);

  @Test
  @DisplayName("pinToPreferred fixes min and max on both axes so content never stretches")
  void pinToPreferredPinsBothAxes() {
    ViewLoader.pinToPreferred(root, new ViewLoader.SceneSize(1000.0, 600.0));

    InOrder order = inOrder(root);
    order.verify(root).setMinWidth(1000.0);
    order.verify(root).setMinHeight(600.0);
    order.verify(root).setMaxWidth(1000.0);
    order.verify(root).setMaxHeight(600.0);
  }

  @Test
  @DisplayName("frameStyle paints the resize letterbox in the app background colour")
  void frameStyleUsesAppBackground() {
    String style = ViewLoader.frameStyle();

    assertEquals("-fx-background-color: " + ViewLoader.APP_BACKGROUND + ";", style);
  }

  @Test
  @DisplayName("grows is true for views that should fill the window and false for fixed ones")
  void growsSelectsTheResizableViews() {
    assertTrue(ViewLoader.grows("/com/samyisok/jpassvaultclient/main.fxml"));
    assertTrue(ViewLoader.grows("/com/samyisok/jpassvaultclient/vault.fxml"));
    assertFalse(ViewLoader.grows("/com/samyisok/jpassvaultclient/setup.fxml"));
    assertFalse(ViewLoader.grows("/com/samyisok/jpassvaultclient/options.fxml"));
  }

  @Test
  @DisplayName("retainOrDesigned keeps a user-chosen window size across a scene swap")
  void retainOrDesignedKeepsUserSize() {
    ViewLoader.SceneSize userSize = new ViewLoader.SceneSize(1400.0, 900.0);

    ViewLoader.SceneSize result = ViewLoader.retainOrDesigned(userSize, userSize);

    assertEquals(1400.0, result.width());
    assertEquals(900.0, result.height());
  }

  @Test
  @DisplayName("retainOrDesigned falls back to the designed size before the window exists")
  void retainOrDesignedUsesDesignedSizeWhenUnknown() {
    ViewLoader.SceneSize designed = new ViewLoader.SceneSize(1000.0, 600.0);

    ViewLoader.SceneSize result = ViewLoader.retainOrDesigned(null, designed);

    assertEquals(designed, result);
  }

  @Test
  @DisplayName("sizeFor uses the root preferred size so the Scene is not left uninitialized")
  void sizeForUsesPreferredSize() {
    when(root.prefWidth(-1)).thenReturn(1000.0);
    when(root.prefHeight(-1)).thenReturn(600.0);

    ViewLoader.SceneSize size = ViewLoader.sizeFor(root);

    assertEquals(1000.0, size.width());
    assertEquals(600.0, size.height());
  }

  @Test
  @DisplayName("sizeFor falls back to 1000x600 when the root reports no usable preferred size")
  void sizeForFallsBackOnUnusablePreferredSize() {
    when(root.prefWidth(-1)).thenReturn(0.0);
    when(root.prefHeight(-1)).thenReturn(Double.NaN);

    ViewLoader.SceneSize size = ViewLoader.sizeFor(root);

    assertEquals(1000.0, size.width());
    assertEquals(600.0, size.height());
  }

  @Test
  @DisplayName("addApplicationStylesheet gives every scene the shared app stylesheet")
  void addApplicationStylesheetAddsSharedStylesheet() {
    Scene scene = mock(Scene.class);
    when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());

    ViewLoader.addApplicationStylesheet(scene);

    assertEquals(1, scene.getStylesheets().size());
    assertTrue(scene.getStylesheets().get(0).endsWith("app.css"));
  }

  @Test
  @DisplayName("the shared app stylesheet resource exists on the classpath")
  void appStylesheetResourceExists() {
    assertNotNull(ViewLoader.class.getResource(ViewLoader.APP_STYLESHEET));
  }
}
