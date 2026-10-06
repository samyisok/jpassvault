package com.samyisok.jpassvaultclient;

import java.net.URISyntaxException;
import com.samyisok.jpassvaultclient.controllers.ClipboardAutoClear;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.MergeVaultException;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.remote.RemoteException;
import com.samyisok.jpassvaultclient.remote.RemoteVault;

/**
 * Coordinates the side effects of unlocking and locking the vault (design
 * D16): local load, optional remote merge, optional remote save, unload,
 * session clearing and clipboard clearing. Keeps {@link MainListener} a thin
 * scene router with a low, single-purpose dependency set.
 */
public class VaultLifecycleCoordinator {

  private final VaultLoader vaultLoader;
  private final RemoteVault remoteVault;
  private final Options options;
  private final Session session;
  private final ClipboardAutoClear clipboardAutoClear;

  public VaultLifecycleCoordinator(VaultLoader vaultLoader, RemoteVault remoteVault,
      Options options, Session session, ClipboardAutoClear clipboardAutoClear) {
    this.vaultLoader = vaultLoader;
    this.remoteVault = remoteVault;
    this.options = options;
    this.session = session;
    this.clipboardAutoClear = clipboardAutoClear;
  }

  /** Unlocks the local vault and, when sync is on, merges the remote copy. */
  public void unlock() {
    vaultLoader.load();
    if (options.ifOnlineSyncOn()) {
      try {
        if (remoteVault.isAvailible()) {
          remoteVault.load();
        }
      } catch (URISyntaxException | RemoteException | MergeVaultException
          | RuntimeException e) {
        e.printStackTrace();
      }
    }
  }

  /** Saves to the remote when sync is on, then unloads and clears all secrets. */
  public void lock() {
    if (options.ifOnlineSyncOn()) {
      try {
        remoteVault.save();
      } catch (URISyntaxException | RemoteException | EncryptionException e) {
        System.out.println("SAVE EXCEPTION: " + e.getMessage());
      }
    }
    vaultLoader.unload();
    session.setPasswordVault(null);
    clipboardAutoClear.clearNow();
  }

  public boolean hasVault() {
    return vaultLoader.ifDbExists();
  }

  /** Discards the in-memory vault without a remote save (options-cancel path). */
  public void discard() {
    vaultLoader.unload();
  }
}
