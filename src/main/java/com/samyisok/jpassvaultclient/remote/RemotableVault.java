package com.samyisok.jpassvaultclient.remote;

import java.io.IOException;
import java.net.URISyntaxException;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.vault.MergeVaultException;

public interface RemotableVault {
  public void load() throws URISyntaxException, RemoteException, MergeVaultException;

  public void save() throws URISyntaxException, RemoteException, EncryptionException;

  public boolean isAvailible();

  public boolean checkHostAndToken(String host, String token)
      throws RemoteException, IOException, InterruptedException, URISyntaxException;
}
