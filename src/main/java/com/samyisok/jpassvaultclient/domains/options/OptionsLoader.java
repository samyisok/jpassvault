package com.samyisok.jpassvaultclient.domains.options;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.FilePermissions;

public class OptionsLoader {

  private final Options options;

  public OptionsLoader(Options options) {
    this.options = options;
  }

  public String toJson(Options options) {
    Gson g = new Gson();
    return g.toJson(options);
  }

  public Options toObject(String json) {
    Gson g = new Gson();
    return g.fromJson(json, Options.class);
  }

  public void save(Options options) {
    save(options, Options.getFullDefaultSettingsPath());
  }

  public void save(Options options, Path path) {
    String json = toJson(options);
    try {
      FilePermissions.writeOwnerOnly(path, json);
    } catch (Exception exception) {
      System.err.println("cant write settings file: " + exception.getMessage());
    }
  }

  public void load() {
    try (
        FileReader file = new FileReader(Options.getFullDefaultSettingsPath().toFile());
        BufferedReader br = new BufferedReader(file)) {
      String json = br.lines().collect(Collectors.joining());
      Options newOptions = toObject(json);
      try {
        options.setApiUrl(newOptions.getApiUrl());
      } catch (IllegalArgumentException insecure) {
        // hand-edited config: keep sync disabled instead of aborting the load
        System.out.println("ignoring insecure api url from config: " + insecure.getMessage());
      }
      options.setTokenApi(newOptions.getTokenApi());
      options.setPathVault(newOptions.getPathVault());
    } catch (Exception exception) {
      System.out.println("cant load file:  " + exception.getMessage());
    }
  }
}
