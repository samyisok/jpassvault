package com.samyisok.jpassvaultclient.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.data.Options;
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

  public void save(Vault vault) {
    try (
        FileWriter file = new FileWriter(options.getFullPathVaultOrDefault().toFile());
        BufferedWriter br = new BufferedWriter(file);
        PrintWriter pr = new PrintWriter(br)) {
      String cryptedJson = aesCipher.encrypt(toJson(vault));
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
}
