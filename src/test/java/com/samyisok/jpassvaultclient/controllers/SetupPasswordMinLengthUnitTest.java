package com.samyisok.jpassvaultclient.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.samyisok.jpassvaultclient.EventAction;
import com.samyisok.jpassvaultclient.EventPublisher;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import javafx.application.Platform;
import javafx.scene.control.PasswordField;

class SetupPasswordMinLengthUnitTest {

  private EventPublisher eventPublisher;
  private RecordingSetupController controller;

  @BeforeAll
  static void bootToolkitOnce() {
    Platform.setImplicitExit(false);
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException alreadyRunning) {
      // one JVM, one toolkit
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    eventPublisher = mock(EventPublisher.class);
    controller = fx(() -> {
      RecordingSetupController c = new RecordingSetupController(eventPublisher);
      c.createPassword1 = new PasswordField();
      c.createPassword2 = new PasswordField();
      return c;
    });
  }

  @Test
  @DisplayName("new password shorter than 8 characters is rejected")
  void rejectsShortPassword() throws Exception {
    fx(() -> {
      controller.createPassword1.setText("abcdefg");
      controller.createPassword2.setText("abcdefg");
      controller.createDb();
      assertEquals(1, controller.warnings.size(), "short password must warn");
      verify(eventPublisher, never()).publish(EventAction.LOCK);
      return null;
    });
  }

  @Test
  @DisplayName("new password of 8 characters is accepted and locks into the vault")
  void acceptsEightCharacters() throws Exception {
    fx(() -> {
      controller.createPassword1.setText("abcdefgh");
      controller.createPassword2.setText("abcdefgh");
      controller.createDb();
      assertTrue(controller.warnings.isEmpty(), "8 characters must pass");
      verify(eventPublisher).publish(EventAction.LOCK);
      return null;
    });
  }

  @Test
  @DisplayName("the unlock path is not length-checked (policy applies to creation only)")
  void unlockIsNotLengthChecked() throws Exception {
    fx(() -> {
      RecordingMainController unlock = new RecordingMainController(eventPublisher);
      unlock.unlockPassword = new PasswordField();
      unlock.unlockPassword.setText("abc");
      unlock.unlock();
      assertTrue(unlock.warnings.isEmpty(), "unlock must not enforce the length policy");
      verify(eventPublisher).publish(EventAction.UNLOCK);
      return null;
    });
  }

  private <T> T fx(Action<T> body) throws Exception {
    FutureTask<T> task = new FutureTask<>(body::run);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private interface Action<T> {
    T run() throws Exception;
  }

  /** Records modal dialogs instead of blocking on a human. */
  static class RecordingSetupController extends SetupController {

    final List<String> warnings = new ArrayList<>();

    RecordingSetupController(EventPublisher eventPublisher) {
      super(eventPublisher, new NoopVaultLoader(), new Session());
    }

    @Override
    protected void warning(String headerMessage, String message) {
      warnings.add(headerMessage + ": " + message);
    }
  }

  /** Never touches a real vault file. */
  static class NoopVaultLoader extends VaultLoader {

    NoopVaultLoader() {
      super(new Vault(), new AesCipher(new Session()),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-vault.xdb")),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-backup.xdb")));
    }

    @Override
    public void createEmptyDbIfNotExist() {
      // no file IO in unit tests
    }
  }

  /** Unlock proceeds for any password the loader accepts. */
  static class ValidVaultLoader extends VaultLoader {

    ValidVaultLoader() {
      super(new Vault(), new AesCipher(new Session()),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-vault.xdb")),
          new FileVaultStore(() -> java.nio.file.Path.of("unused-backup.xdb")));
    }

    @Override
    public boolean vaultPasswordIsValid() {
      return true;
    }
  }

  /** Records modal dialogs instead of blocking on a human. */
  static class RecordingMainController extends MainController {

    final List<String> warnings = new ArrayList<>();

    RecordingMainController(EventPublisher eventPublisher) {
      super(eventPublisher, new ValidVaultLoader(), new Session(),
          mock(RemoteVault.class), mock(Options.class));
    }

    @Override
    protected void warning(String headerMessage, String message) {
      warnings.add(headerMessage + ": " + message);
    }
  }
}
