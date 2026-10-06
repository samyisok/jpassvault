package com.samyisok.jpassvaultclient.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.StageHolder;
import com.samyisok.jpassvaultclient.controllers.ClipboardAutoClear;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.password.PasswordGenerator;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real {@code vault.fxml} against a real toolkit, so the FXML
 * bindings, the controller wiring and the post-create behaviour are checked
 * together. Needs a display: locally {@code DISPLAY}, in CI {@code xvfb-run}.
 */
class VaultControllerIntegrationTest {

  private Vault vault;
  private StubVaultLoader vaultLoader;
  private RecordingController controller;

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
  void loadVaultView() throws Exception {
    vault = new Vault();
    vaultLoader = new StubVaultLoader(vault);
    controller = fx(this::loadController);
  }

  private RecordingController loadController() throws Exception {
    FXMLLoader loader =
        new FXMLLoader(VaultController.class.getResource(VaultController.FXML_PATH));
    loader.setControllerFactory(
        type -> new RecordingController(new EventPublisher(new StageHolder()), vault,
            vaultLoader, new PasswordGenerator(), ClipboardAutoClear.forSystemClipboard()));
    loader.load();
    return (RecordingController) loader.getController();
  }

  /** Runs the body on the JavaFX Application Thread and returns its result. */
  private <T> T fx(Action<T> body) throws Exception {
    FutureTask<T> task = new FutureTask<>(body::run);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private interface Action<T> {
    T run() throws Exception;
  }

  private List<String> items() {
    return List.copyOf(controller.listVault.getItems());
  }

  private void createRecord(String name, String login, String password) {
    controller.nameCreate.setText(name);
    controller.loginCreate.setText(login);
    controller.passwordCreate.setText(password);
    controller.create();
  }

  @Test
  @DisplayName("creating a record prefills search, lists it, selects it and shows it")
  void createFillsSearchSelectsAndShowsRecord() throws Exception {
    fx(() -> {
      createRecord("GitHub", "user@example.com", "pw123");

      assertEquals("GitHub", controller.searchViewByName.getText());
      assertEquals(List.of("GitHub"), items());
      assertEquals("GitHub", controller.listVault.getSelectionModel().getSelectedItem());
      assertEquals("user@example.com", controller.loginView.getText());
      assertEquals("pw123", controller.passwordView.getText());
      return null;
    });
  }

  @Test
  @DisplayName("clicking a row loads its record into the view")
  void clickLoadsRecordIntoView() throws Exception {
    vault.put("GitHub", new VaultContainer("user@example.com", "pw123"));

    fx(() -> {
      controller.listVault.getItems().add("GitHub");
      controller.listVault.getSelectionModel().select("GitHub");
      controller.onClick();

      assertEquals("GitHub", controller.nameView.getText());
      assertEquals("user@example.com", controller.loginView.getText());
      assertEquals("pw123", controller.passwordView.getText());
      return null;
    });
  }

  @Test
  @DisplayName("an empty name warns and leaves the screen untouched")
  void emptyNameWarnsAndTouchesNothing() throws Exception {
    fx(() -> {
      controller.searchViewByName.setText("Bank");
      controller.nameCreate.setText("");
      controller.passwordCreate.setText("pw123");
      controller.create();

      assertEquals(List.of("Can't create record: Fields is empty"), controller.warnings);
      assertEquals("Bank", controller.searchViewByName.getText());
      assertEquals(List.of(), items());
      assertEquals("", controller.nameView.getText());
      assertEquals(0, vaultLoader.saves);
      return null;
    });
  }

  @Test
  @DisplayName("an empty password warns and stores nothing")
  void emptyPasswordWarnsAndStoresNothing() throws Exception {
    fx(() -> {
      controller.nameCreate.setText("GitHub");
      controller.passwordCreate.setText("");
      controller.create();

      assertEquals(1, controller.warnings.size());
      assertFalse(vault.containsKey("GitHub"));
      assertEquals(List.of(), items());
      return null;
    });
  }

  @Test
  @DisplayName("a stale filter is replaced so the new record stays visible")
  void staleFilterIsReplacedByTheNewName() throws Exception {
    fx(() -> {
      controller.searchViewByName.setText("Bank");
      createRecord("GitHub", "user@example.com", "pw123");

      assertEquals("GitHub", controller.searchViewByName.getText());
      assertEquals(List.of("GitHub"), items());
      return null;
    });
  }

  @Test
  @DisplayName("saving a record keeps the current search")
  void saveKeepsCurrentSearch() throws Exception {
    vault.put("GitHub", new VaultContainer("old-login", "old-password"));

    fx(() -> {
      controller.searchViewByName.setText("Git");
      controller.updateSelector();
      controller.listVault.getSelectionModel().select("GitHub");
      controller.nameView.setText("GitHub");
      controller.loginView.setText("new-login");
      controller.passwordView.setText("new-password");
      controller.save();

      assertEquals("Git", controller.searchViewByName.getText());
      assertEquals(List.of("GitHub"), items());
      assertEquals("new-password", vault.get("GitHub").getPassword());
      return null;
    });
  }

  @Test
  @DisplayName("deleting a record keeps the current search")
  void deleteKeepsCurrentSearch() throws Exception {
    vault.put("GitHub", new VaultContainer("login", "password"));

    fx(() -> {
      controller.searchViewByName.setText("Git");
      controller.updateSelector();
      controller.listVault.getSelectionModel().select("GitHub");
      controller.delete();

      assertEquals("Git", controller.searchViewByName.getText());
      assertEquals(List.of(), items());
      assertTrue(vault.isEmpty());
      return null;
    });
  }

  @Test
  @DisplayName("a duplicate name warns, keeps the old record and touches nothing")
  void duplicateNameWarnsAndKeepsOldRecord() throws Exception {
    vault.put("GitHub", new VaultContainer("old-login", "old-password"));

    fx(() -> {
      controller.searchViewByName.setText("Git");
      controller.updateSelector();
      controller.listVault.getSelectionModel().select("GitHub");
      controller.onClick();
      createRecord("GitHub", "new-login", "new-password");

      assertEquals(1, controller.warnings.size());
      assertEquals("old-password", vault.get("GitHub").getPassword());
      assertEquals("Git", controller.searchViewByName.getText());
      assertEquals(List.of("GitHub"), items());
      assertEquals("old-password", controller.passwordView.getText());
      assertEquals(0, vaultLoader.saves);
      return null;
    });
  }

  /** Controllers that record modal dialogs instead of blocking on a human. */
  static class RecordingController extends VaultController {

    final List<String> warnings = new ArrayList<>();
    boolean confirmAnswer = true;

    RecordingController(EventPublisher eventPublisher, Vault vault, VaultLoader vaultLoader,
        PasswordGenerator passwordGenerator, ClipboardAutoClear clipboardAutoClear) {
      super(eventPublisher, vault, vaultLoader, passwordGenerator, clipboardAutoClear);
    }

    @Override
    protected void warning(String headerMessage, String message) {
      warnings.add(headerMessage + ": " + message);
    }

    @Override
    protected boolean confirm(String title, String content) {
      return confirmAnswer;
    }
  }

  /** Records saves so no test can reach a real vault file. */
  static class StubVaultLoader extends VaultLoader {

    int saves;

    StubVaultLoader(Vault vault) {
      super(vault, new AesCipher(new Session()),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-vault.xdb")),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-backup.xdb")));
    }

    @Override
    public void save(Vault vault) {
      saves++;
    }
  }
}
