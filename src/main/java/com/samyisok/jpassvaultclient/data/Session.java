package com.samyisok.jpassvaultclient.data;

import org.springframework.stereotype.Component;

@Component
public class Session {
  private String passwordVault;

  /**
   * @return the passwordVault
   */
  public String getPasswordVault() {
    return passwordVault;
  }

  /**
   * @param passwordVault the passwordVault to set
   */
  public void setPasswordVault(String passwordVault) {
    this.passwordVault = passwordVault;
  }

}
