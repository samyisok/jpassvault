package com.samyisok.jpassvaultclient.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Key derivation for the vault (design D2): PBKDF2-HMAC-SHA256 with a stored
 * salt and iteration count, one 64-byte output split into AES and HMAC keys.
 * The legacy single-round SHA3-256 path is kept for pre-envelope files.
 */
public final class VaultKeyDerivation {

  public static final String PBKDF2_ALG = "PBKDF2WithHmacSHA256";
  public static final int ITERATIONS = 600000;
  public static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 512;

  private VaultKeyDerivation() {
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance(PBKDF2_ALG).generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException("PBKDF2 derivation failed", e);
    } finally {
      spec.clearPassword();
    }
  }

  /**
   * One 64-byte PBKDF2 run split into both keys (design D2: derive once,
   * never twice — derivation is the expensive part of every save).
   */
  public static DerivedKeys deriveKeys(String password, byte[] salt, int iterations) {
    byte[] material = derive(password, salt, iterations);
    return new DerivedKeys(
        new SecretKeySpec(Arrays.copyOf(material, 32), "AES"),
        Arrays.copyOfRange(material, 32, 64));
  }

  /** First 32 bytes of the PBKDF2 output, as an AES-256 key. */
  public static SecretKeySpec deriveAesKey(String password, byte[] salt, int iterations) {
    return deriveKeys(password, salt, iterations).aesKey();
  }

  /** Pre-envelope files: single SHA3-256 round over the password bytes. */
  public static SecretKeySpec legacyAesKey(String password) {
    return new SecretKeySpec(legacyDigest(password), "AES");
  }

  /** Pre-envelope HMAC key: the same legacy digest bytes (design D3). */
  public static byte[] legacyHmacKey(String password) {
    return legacyDigest(password);
  }

  private static byte[] legacyDigest(String password) {
    try {
      return MessageDigest.getInstance("SHA3-256").digest(password.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA3-256 unavailable", e);
    }
  }

  /** Both keys produced by a single derivation. */
  public static final class DerivedKeys {

    private final SecretKeySpec aesKey;
    private final byte[] hmacKey;

    DerivedKeys(SecretKeySpec aesKey, byte[] hmacKey) {
      this.aesKey = aesKey;
      this.hmacKey = hmacKey;
    }

    public SecretKeySpec aesKey() {
      return aesKey;
    }

    public byte[] hmacKey() {
      return hmacKey;
    }
  }
}
