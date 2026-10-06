package com.samyisok.jpassvaultclient.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.domains.session.Session;

/**
 * Encrypts the vault as a versioned JSON envelope (design D1): PBKDF2 keys
 * with a persisted per-vault salt (D2), a fresh random IV per encryption,
 * AES-256/GCM. Pre-envelope files (base64 of IV||ciphertext, SHA3 key) are
 * read through the isolated legacy path and rewritten on the next save.
 */
public class AesCipher {
  private final static String CIPHERMODE = "AES/GCM/NoPadding";
  private final static int GCM_IV_LENGTH = 12;
  private final static int GCM_TAG_LENGTH = 16;
  private final static int SALT_BYTES = VaultKeyDerivation.SALT_BYTES;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final Session session;
  private final Gson gson = new Gson();

  /** Per-vault salt; null until an envelope is loaded or a new vault saves. */
  private byte[] salt;
  private SecretKeySpec aesKey;
  private byte[] hmacKey;

  public AesCipher(Session session) {
    this.session = session;
  }

  public String encrypt(String strToEncrypt) throws EncryptionException {
    try {
      String password = session.getPasswordVault();
      if (password == null) {
        throw new EncryptionException("no vault password in session");
      }
      if (salt == null) {
        salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
      }
      VaultKeyDerivation.DerivedKeys keys =
          VaultKeyDerivation.deriveKeys(password, salt, VaultKeyDerivation.ITERATIONS);
      aesKey = keys.aesKey();
      hmacKey = keys.hmacKey();

      byte[] iv = new byte[GCM_IV_LENGTH];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHERMODE);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec(iv));
      byte[] ciphertext = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));

      VaultEnvelope envelope = new VaultEnvelope(VaultEnvelope.CURRENT_FORMAT,
          new VaultEnvelope.Kdf(VaultKeyDerivation.PBKDF2_ALG, VaultKeyDerivation.ITERATIONS,
              encode(salt)),
          encode(iv), encode(ciphertext));
      return gson.toJson(envelope);
    } catch (EncryptionException e) {
      throw e;
    } catch (Exception e) {
      throw new EncryptionException(e.getMessage());
    }
  }

  public String decrypt(String strToDecrypt) throws EncryptionException {
    try {
      String body = strToDecrypt.trim();
      return body.startsWith("{") ? decryptEnvelope(body) : decryptLegacy(body);
    } catch (EncryptionException e) {
      throw e;
    } catch (Exception e) {
      throw new EncryptionException(e.getMessage());
    }
  }

  /** Envelope path: derivation parameters come from the stored file (spec: stored KDF parameters drive derivation). */
  private String decryptEnvelope(String body) throws Exception {
    String password = requirePassword();
    VaultEnvelope envelope = gson.fromJson(body, VaultEnvelope.class);
    VaultEnvelopeValidator.validate(envelope);
    byte[] envSalt = decode(envelope.getKdf().getSalt());
    int iterations = envelope.getKdf().getIterations();

    VaultKeyDerivation.DerivedKeys keys =
        VaultKeyDerivation.deriveKeys(password, envSalt, iterations);
    byte[] iv = decode(envelope.getIv());
    Cipher cipher = Cipher.getInstance(CIPHERMODE);
    cipher.init(Cipher.DECRYPT_MODE, keys.aesKey(), gcmSpec(iv));
    String plaintext = new String(cipher.doFinal(decode(envelope.getCt())), StandardCharsets.UTF_8);

    // Commit derived state only after the tag verified.
    salt = envSalt;
    aesKey = keys.aesKey();
    hmacKey = keys.hmacKey();
    return plaintext;
  }

  /**
   * Legacy path, isolated (task 3.2): exactly what a downgraded application
   * version does — base64 body of IV||ciphertext, single SHA3-256 key, IV
   * taken from the file. An envelope fed here raises and returns nothing.
   */
  String decryptLegacy(String body) throws Exception {
    String password = requirePassword();
    byte[] decoded = Base64.getDecoder().decode(body);
    byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
    SecretKeySpec key = VaultKeyDerivation.legacyAesKey(password);
    Cipher cipher = Cipher.getInstance(CIPHERMODE);
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec(iv));
    String plaintext =
        new String(cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH),
            StandardCharsets.UTF_8);

    salt = null;
    aesKey = key;
    hmacKey = VaultKeyDerivation.legacyHmacKey(password);
    return plaintext;
  }

  /**
   * HMAC key for the sync checksum: PBKDF2 split once an envelope exists,
   * legacy SHA3 bytes before the first save (design D3, migration case).
   */
  public byte[] getHmacKey() throws EncryptionException {
    if (hmacKey == null) {
      hmacKey = VaultKeyDerivation.legacyHmacKey(requirePassword());
    }
    return hmacKey;
  }

  /**
   * Keyed content checksum for sync change detection (design D18): HMAC-SHA256
   * over the given content with the derived HMAC key, uppercase hex.
   */
  public String checksumOf(String content) throws EncryptionException {
    byte[] key = getHmacKey();
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return hex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new EncryptionException(e.getMessage());
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder builder = new StringBuilder();
    for (byte b : bytes) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString().toUpperCase();
  }

  /**
   * Best-effort removal of derived key material when the vault locks (design
   * D12): zero-fills the salt and HMAC key and drops the AES key. The JVM may
   * still hold copies, but the easy post-lock heap-dump recovery is gone.
   */
  public void clearKeys() {
    if (salt != null) {
      Arrays.fill(salt, (byte) 0);
      salt = null;
    }
    if (hmacKey != null) {
      Arrays.fill(hmacKey, (byte) 0);
      hmacKey = null;
    }
    aesKey = null;
  }

  private String requirePassword() throws EncryptionException {
    String password = session.getPasswordVault();
    if (password == null) {
      throw new EncryptionException("no vault password in session");
    }
    return password;
  }

  private static GCMParameterSpec gcmSpec(byte[] iv) {
    return new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
  }

  private static String encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  private static byte[] decode(String text) {
    return Base64.getDecoder().decode(text);
  }
}
