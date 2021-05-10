package com.samyisok.jpassvaultclient.remote;

public class HashScheme {
  private String hash;

  /**
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  /**
   * @param hash the hash to set
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */

  @Override
  public String toString() {
    return "HashScheme [hash=" + hash + "]";
  }
  
}
