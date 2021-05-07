package com.samyisok.jpassvaultclient.domains.vault;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.options.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VaultLoader {

  @Autowired
  Options options;

  @Autowired
  Vault vault;

  @Autowired
  AesCipher aesCipher;

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

  public void save(Vault vault) {
    try (
        FileWriter file = new FileWriter(options.getFullPathVaultOrDefault().toFile());
        BufferedWriter br = new BufferedWriter(file);
        PrintWriter pr = new PrintWriter(br)) {
      String cryptedJson = getEncryptedJsonDb(vault);
      pr.write(cryptedJson);
    } catch (Exception exception) {
      System.out
          .println("cant write file: " + exception.toString() + exception.getMessage());
    }
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
    File tempFile = new File(options.getFullPathVaultOrDefault().toString());
    if (!tempFile.exists()) {
      save(new Vault());
    }
  }

  public boolean ifDbExists() {
    File tempFile = new File(options.getFullPathVaultOrDefault().toString());
    return tempFile.exists();
  }

  public boolean vaultPasswordIsValid() {
    File tempFile = new File(options.getFullPathVaultOrDefault().toString());
    if (!tempFile.exists()) {
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
    try (
        FileReader file = new FileReader(options.getFullPathVaultOrDefault().toFile());
        BufferedReader br = new BufferedReader(file)) {
      String cryptedJson = br.lines().collect(Collectors.joining());
      return aesCipher.decrypt(cryptedJson);
    } catch (Exception e) {
      throw e;
    }
  }

  public void unload() {
    vault.clear();
  }

  public void merge(String encriptedDb) throws MergeVaultException {
    if (vault.isEmpty()) {
      load();
    }

    // TODO save backup

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
