package com.samyisok.jpassvaultclient.data;

import org.springframework.stereotype.Component;

@Component
public class Options {
  private String pathVault = "tmp.xdb";
  private String tokenApi;
  private String apiUrl;

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
}
