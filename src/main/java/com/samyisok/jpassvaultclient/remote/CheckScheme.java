package com.samyisok.jpassvaultclient.remote;

public class CheckScheme {
  private String check;

  public boolean isValid() {
    return check.equals("ok");
  }

  /**
   * @return the check
   */
  public String getCheck() {
    return check;
  }

  /**
   * @param check the check to set
   */
  public void setCheck(String check) {
    this.check = check;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */

  @Override
  public String toString() {
    return "CheckScheme [check=" + check + "]";
  }
}
