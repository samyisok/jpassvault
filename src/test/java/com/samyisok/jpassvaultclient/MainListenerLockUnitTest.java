package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.samyisok.jpassvaultclient.controllers.ClipboardAutoClear;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteException;
import com.samyisok.jpassvaultclient.remote.RemoteVault;
import javafx.stage.Stage;

class MainListenerLockUnitTest {

  @TempDir
  Path tempDir;

  private StageHolder stageHolder;
  private VaultLoader vaultLoader;
  private ViewLoader viewLoader;
  private RemoteVault remoteVault;
  private Options options;
  private Session session;
  private ClipboardAutoClear clipboardAutoClear;

  @BeforeEach
  void setUp() {
    stageHolder = mock(StageHolder.class);
    vaultLoader = mock(VaultLoader.class);
    viewLoader = mock(ViewLoader.class);
    remoteVault = mock(RemoteVault.class);
    options = mock(Options.class);
    session = new Session();
    clipboardAutoClear = mock(ClipboardAutoClear.class);
    when(stageHolder.getStage()).thenReturn(mock(Stage.class));
    when(options.ifOnlineSyncOn()).thenReturn(true);
  }

  @Test
  @DisplayName("LOCK clears the session password after the remote save")
  void lockClearsSessionAfterRemoteSave() {
    session.setPasswordVault("master-password");
    listener().handle(new StageActionEvent(EventAction.LOCK));
    assertNull(session.getPasswordVault(), "session must not survive LOCK");
    verify(clipboardAutoClear).clearNow();
  }

  @Test
  @DisplayName("a failing remote save still clears the session on LOCK")
  void failedRemoteSaveStillClearsSession() throws Exception {
    session.setPasswordVault("master-password");
    doThrow(new RemoteException("sync down")).when(remoteVault).save();
    listener().handle(new StageActionEvent(EventAction.LOCK));
    assertNull(session.getPasswordVault(), "clearing must not depend on sync success");
  }

  @Test
  @DisplayName("LOCK clears derived key material, not only the password")
  void lockClearsDerivedKeys() throws Exception {
    Session vaultSession = new Session();
    vaultSession.setPasswordVault("master-password");
    AesCipher aesCipher = new AesCipher(vaultSession);
    Vault vault = new Vault();
    vault.put("GitHub", new VaultContainer("user", "secret"));
    VaultLoader realLoader = new VaultLoader(vault, aesCipher,
        new FileVaultStore(() -> tempDir.resolve("jpassdb.xdb")),
        new FileVaultStore(() -> tempDir.resolve("jpassdb.bak.xdb")));
    realLoader.save(vault);

    when(options.ifOnlineSyncOn()).thenReturn(false);
    VaultLifecycleCoordinator lifecycle = new VaultLifecycleCoordinator(realLoader,
        remoteVault, options, vaultSession, clipboardAutoClear);
    new MainListener(stageHolder, viewLoader, lifecycle)
        .handle(new StageActionEvent(EventAction.LOCK));

    assertThrows(EncryptionException.class, aesCipher::getHmacKey,
        "no key material may remain usable after LOCK");
  }

  private MainListener listener() {
    VaultLifecycleCoordinator lifecycle = new VaultLifecycleCoordinator(vaultLoader,
        remoteVault, options, session, clipboardAutoClear);
    return new MainListener(stageHolder, viewLoader, lifecycle);
  }
}
