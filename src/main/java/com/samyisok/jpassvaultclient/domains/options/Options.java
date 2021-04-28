package com.samyisok.jpassvaultclient.domains.options;

import java.io.Serializable;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class Options implements Serializable {
  public static final String DEFAULT_APP_FOLDER_NAME = "jpassvault";
  public static final String DEFAULT_FOLDER =
      Path.of(System.getProperty("user.home"), Options.DEFAULT_APP_FOLDER_NAME)
          .toString();
  public static final String DEFAULT_DB_NAME = "jpassdb.xdb";
  public static final String DEFAULT_SETTINGS_FILE_NAME = "config.json";
  private String pathVault;
  private String tokenApi;
  private String apiUrl;


  public static Path getFullDefaultVaultPath() {
    return Path.of(DEFAULT_FOLDER, DEFAULT_DB_NAME);
  }

  public static Path getFullDefaultSettingsPath() {
    return Path.of(DEFAULT_FOLDER, DEFAULT_SETTINGS_FILE_NAME);
  }

  public static Path getFullDefaultFolderPath() {
    return Path.of(DEFAULT_FOLDER);
  }

  public void setDefaultData() {
    this.pathVault = getFullDefaultVaultPath().toString();
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
   * @param apiUrl the apiUrl to set
   */
  public void setApiUrl(String apiUrl) {
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
        + tokenApi + "]";
  }
}
