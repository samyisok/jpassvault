package com.samyisok.jpassvaultclient.domains.vault;

import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;

/**
 * Domain-side vault persistence: serializes the aggregate, encrypts it, and
 * delegates storage to a {@link VaultStore} port (design D17). Contains no
 * file, path, or permission knowledge.
 */
public class VaultLoader {

  private final Vault vault;
  private final AesCipher aesCipher;
  private final VaultStore vaultStore;
  private final VaultStore backupStore;

  public VaultLoader(Vault vault, AesCipher aesCipher, VaultStore vaultStore,
      VaultStore backupStore) {
    this.vault = vault;
    this.aesCipher = aesCipher;
    this.vaultStore = vaultStore;
    this.backupStore = backupStore;
  }

  public String toJson(Vault vault) {
    Gson g = new Gson();
    return g.toJson(vault);
  }

  public Vault toObject(String json) {
    Gson g = new Gson();
    return g.fromJson(json, Vault.class);
  }

  String getEncryptedJsonDb(Vault vault) throws EncryptionException {
    return aesCipher.encrypt(toJson(vault));
  }

  public String getCurrentEncryptedJsonDb() throws EncryptionException {
    return getEncryptedJsonDb(vault);
  }

  void saveBackup() {
    write(backupStore);
  }

  public void save(Vault vault) {
    write(vaultStore);
  }

  private void write(VaultStore store) {
    String encryptedJson;
    try {
      // Encrypt before touching the destination: a failure must not truncate
      // the existing vault (design D11).
      encryptedJson = getEncryptedJsonDb(vault);
    } catch (Exception exception) {
      System.err.println(
          "cant encrypt vault, existing file preserved: " + exception.getMessage());
      return;
    }
    try {
      store.write(encryptedJson);
    } catch (Exception exception) {
      System.err.println("cant write vault file: " + exception.getMessage());
    }
  }

  public String getVaultEncryptCheckSum() throws EncryptionException {
    return aesCipher.checksumOf(toJson(vault));
  }

  public void load() {
    try {
      Vault newVault = toObject(loadDecrypt());
      vault.putAll(newVault);
    } catch (Exception exception) {
      System.out.println("cant load file:  " + exception.getMessage());
    }
  }

  public void createEmptyDbIfNotExist() {
    if (!vaultStore.exists()) {
      save(new Vault());
    }
  }

  public boolean ifDbExists() {
    return vaultStore.exists();
  }

  public boolean vaultPasswordIsValid() {
    if (!vaultStore.exists()) {
      return false;
    }

    try {
      loadDecrypt();
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  String loadDecrypt() throws Exception {
    return aesCipher.decrypt(vaultStore.read());
  }

  public void unload() {
    vault.clear();
    aesCipher.clearKeys();
  }

  public void merge(String encriptedDb) throws MergeVaultException {
    if (vault.isEmpty()) {
      load();
    }

    saveBackup();

    try {
      String remoteDbJson = aesCipher.decrypt(encriptedDb);
      Vault remoteDb = toObject(remoteDbJson);
      remoteDb.forEach((key, value) -> {
        if (vault.containsKey(key)) {
          if (vault.get(key).equals(value)) {
            // Already exits equals, do nothing;
          } else {
            // Save with another name.
            vault.put(key + "(" + value.hashCode() + ")", value);
          }
        } else {
          vault.put(key, value);
        }
      });

    } catch (Exception e) {
      System.out.println(e.toString() + e.getMessage());
      throw new MergeVaultException("Critical Error when merging");
    }
  }
}
