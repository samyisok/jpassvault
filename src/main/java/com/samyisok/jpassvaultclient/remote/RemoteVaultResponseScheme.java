package com.samyisok.jpassvaultclient.remote;

public class RemoteVaultResponseScheme {
  Integer id;
  String file;
  String createdDate;
  String modifiedDate;

  /**
   * @return the id
   */
  public Integer getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * @return the file
   */
  public String getFile() {
    return file;
  }

  /**
   * @param file the file to set
   */
  public void setFile(String file) {
    this.file = file;
  }

  /**
   * @return the createdDate
   */
  public String getCreatedDate() {
    return createdDate;
  }

  /**
   * @param createdDate the createdDate to set
   */
  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * @return the modifiedDate
   */
  public String getModifiedDate() {
    return modifiedDate;
  }

  /**
   * @param modifiedDate the modifiedDate to set
   */
  public void setModifiedDate(String modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */

  @Override
  public String toString() {
    return "RemoteVaultResponseScheme [createdDate=" + createdDate + ", file=" + file
        + ", id=" + id + ", modifiedDate=" + modifiedDate + "]";
  }
}
