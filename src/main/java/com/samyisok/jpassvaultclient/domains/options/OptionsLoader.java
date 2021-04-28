package com.samyisok.jpassvaultclient.domains.options;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OptionsLoader {

  @Autowired
  Options options;

  public String toJson(Options options) {
    Gson g = new Gson();
    return g.toJson(options);
  }

  public Options toObject(String json) {
    Gson g = new Gson();
    return g.fromJson(json, Options.class);
  }

  public void save(Options options) {
    try (
        FileWriter file = new FileWriter(Options.getFullDefaultSettingsPath().toFile());
        BufferedWriter br = new BufferedWriter(file);
        PrintWriter pr = new PrintWriter(br)) {
      String json = toJson(options);
      pr.write(json);
    } catch (Exception exception) {
      System.out
          .println("cant write file: " + exception.toString() + exception.getMessage());
    }
  }

  public void load() {
    try (
        FileReader file = new FileReader(Options.getFullDefaultSettingsPath().toFile());
        BufferedReader br = new BufferedReader(file)) {
      String json = br.lines().collect(Collectors.joining());
      Options newOptions = toObject(json);
      options.setApiUrl(newOptions.getApiUrl());
      options.setTokenApi(newOptions.getTokenApi());
      options.setPathVault(newOptions.getPathVault());
    } catch (Exception exception) {
      System.out.println("cant load file:  " + exception.getMessage());
    }
  }
}
