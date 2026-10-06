package com.samyisok.jpassvaultclient.crypto;

/**
 * Versioned container for an encrypted vault body. Serialized as the file
 * body itself (design D1): {"format":2,"kdf":{...},"iv":...,"ct":...}.
 * Fields are not secret; tampering with them only makes decryption fail.
 */
public class VaultEnvelope {

  public static final int CURRENT_FORMAT = 2;

  private int format;
  private Kdf kdf;
  private String iv;
  private String ct;

  public VaultEnvelope() {
    // Gson
  }

  public VaultEnvelope(int format, Kdf kdf, String iv, String ct) {
    this.format = format;
    this.kdf = kdf;
    this.iv = iv;
    this.ct = ct;
  }

  public int getFormat() {
    return format;
  }

  public Kdf getKdf() {
    return kdf;
  }

  public String getIv() {
    return iv;
  }

  public String getCt() {
    return ct;
  }

  public static class Kdf {
    private String alg;
    private int iterations;
    private String salt;

    public Kdf() {
      // Gson
    }

    public Kdf(String alg, int iterations, String salt) {
      this.alg = alg;
      this.iterations = iterations;
      this.salt = salt;
    }

    public String getAlg() {
      return alg;
    }

    public int getIterations() {
      return iterations;
    }

    public String getSalt() {
      return salt;
    }
  }
}
