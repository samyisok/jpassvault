package com.samyisok.jpassvaultclient.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.domains.session.Session;

class VaultEnvelopeUnitTest {

  private Session session;
  private AesCipher aesCipher;

  @BeforeEach
  void setUp() {
    session = new Session();
    session.setPasswordVault("master-password");
    aesCipher = new AesCipher(session);
  }

  @Test
  @DisplayName("ciphertext is a versioned JSON envelope with format, kdf, iv and ct")
  void encryptProducesVersionedEnvelope() throws Exception {
    String body = aesCipher.encrypt("secret data");
    assertTrue(body.startsWith("{"), "body must be the JSON envelope");
    VaultEnvelope parsed = new Gson().fromJson(body, VaultEnvelope.class);
    assertEquals(2, parsed.getFormat());
    assertEquals("PBKDF2WithHmacSHA256", parsed.getKdf().getAlg());
    assertEquals(600000, parsed.getKdf().getIterations());
    assertEquals("secret data", aesCipher.decrypt(body));
  }

  @Test
  @DisplayName("two vaults with the same password store different salts")
  void differentVaultsUseDifferentSalts() throws Exception {
    Session other = new Session();
    other.setPasswordVault("master-password");
    String first = new Gson().fromJson(aesCipher.encrypt("x"), VaultEnvelope.class).getKdf().getSalt();
    String second = new Gson().fromJson(new AesCipher(other).encrypt("x"), VaultEnvelope.class).getKdf().getSalt();
    assertNotEquals(first, second);
  }

  @Test
  @DisplayName("decryption uses the kdf parameters stored in the envelope")
  void storedDerivationParametersDriveDecryption() throws Exception {
    byte[] salt = new byte[16];
    new java.security.SecureRandom().nextBytes(salt);
    byte[] iv = new byte[12];
    new java.security.SecureRandom().nextBytes(iv);
    SecretKeySpec key = VaultKeyDerivation.deriveAesKey("master-password", salt, 1000);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] ct = cipher.doFinal("low-cost".getBytes(StandardCharsets.UTF_8));
    VaultEnvelope env = new VaultEnvelope(2,
        new VaultEnvelope.Kdf("PBKDF2WithHmacSHA256", 1000, Base64.getEncoder().encodeToString(salt)),
        Base64.getEncoder().encodeToString(iv), Base64.getEncoder().encodeToString(ct));
    assertEquals("low-cost", aesCipher.decrypt(new Gson().toJson(env)));
  }

  @Test
  @DisplayName("a wrong password fails to decrypt the envelope")
  void wrongPasswordFailsToDecrypt() throws Exception {
    String body = aesCipher.encrypt("secret data");
    session.setPasswordVault("wrong-password");
    assertThrows(EncryptionException.class, () -> aesCipher.decrypt(body));
  }
}
