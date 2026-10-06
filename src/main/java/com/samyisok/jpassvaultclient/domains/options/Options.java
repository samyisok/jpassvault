package com.samyisok.jpassvaultclient.domains.options;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Options implements Serializable {
  public static final String DEFAULT_APP_FOLDER_NAME = "jpassvault";
  public static final String DEFAULT_FOLDER =
      Path.of(System.getProperty("user.home"), Options.DEFAULT_APP_FOLDER_NAME)
          .toString();
  public static final String DEFAULT_DB_NAME = "jpassdb.xdb";
  public static final String DEFAULT_SETTINGS_FILE_NAME = "config.json";
  public static final String DEFAULT_DB_BACKUP_NAME = "jpassdb.bak.xdb";
  public static final String DEFAULT_BACKUP_FOLDER = DEFAULT_FOLDER;
  private String pathVault;
  private String tokenApi;
  private String apiUrl;


  public static Path getFullDefaultVaultPath() {
    return Path.of(DEFAULT_FOLDER, DEFAULT_DB_NAME);
  }

  public static Path getFullDefaultBackupVaultPath() {
    return Path.of(DEFAULT_BACKUP_FOLDER, DEFAULT_DB_BACKUP_NAME);
  }

  public static Path getFullDefaultSettingsPath() {
    return Path.of(DEFAULT_FOLDER, DEFAULT_SETTINGS_FILE_NAME);
  }

  public static Path getFullDefaultFolderPath() {
    return Path.of(DEFAULT_FOLDER);
  }

  public void setDefaultData() {
    this.pathVault = getFullDefaultVaultPath().toString();
    this.apiUrl = "";
    this.tokenApi = "";
  }


  public Path getFullPathVaultOrDefault() {
    if (this.pathVault == null) {
      return getFullDefaultVaultPath();
    } else {
      return Path.of(this.pathVault);
    }
  }

  /**
   * @return the pathVault
   */
  public String getPathVault() {
    return pathVault;
  }

  /**
   * @param pathVault the pathVault to set
   */
  public void setPathVault(String pathVault) {
    this.pathVault = pathVault;
  }

  public void setPathVaultWithDefaultName(String pathFolder) {
    this.pathVault = Path.of(pathFolder, Options.DEFAULT_DB_NAME).toString();
  }

  /**
   * @return the tokenApi
   */
  public String getTokenApi() {
    return tokenApi;
  }

  /**
   * @param tokenApi the tokenApi to set
   */
  public void setTokenApi(String tokenApi) {
    this.tokenApi = tokenApi;
  }

  /**
   * @return the apiUrl
   */
  public String getApiUrl() {
    return apiUrl;
  }

  /**
   * Sync URLs must be https; plain http survives only for local development
   * hosts. Empty means "sync disabled" and is always acceptable.
   */
  public static boolean isSecureUrl(String url) {
    if (url == null || url.isEmpty()) {
      return true;
    }
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return false;
    }
    if ("https".equalsIgnoreCase(uri.getScheme())) {
      return true;
    }
    if ("http".equalsIgnoreCase(uri.getScheme())) {
      String host = uri.getHost();
      return "localhost".equals(host) || "127.0.0.1".equals(host);
    }
    return false;
  }

  /**
   * @param apiUrl the apiUrl to set
   * @throws IllegalArgumentException when the URL is neither https nor a
   *         localhost http URL
   */
  public void setApiUrl(String apiUrl) {
    if (!isSecureUrl(apiUrl)) {
      throw new IllegalArgumentException(
          "Only https:// sync URLs are allowed (plain http is limited to localhost)");
    }
    this.apiUrl = apiUrl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */

  @Override
  public String toString() {
    return "Options [apiUrl=" + apiUrl + ", pathVault=" + pathVault + ", tokenApi="
        + (tokenApi == null || tokenApi.isEmpty() ? "" : "********") + "]";
  }

  public boolean ifOnlineSyncOn(){
    if ( getApiUrl() == null || getApiUrl().isEmpty()
        || getTokenApi() == null || getTokenApi().isEmpty()) {
      return false;
    }
    return true;
  }
}
