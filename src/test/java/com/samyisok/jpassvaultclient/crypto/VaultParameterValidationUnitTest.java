package com.samyisok.jpassvaultclient.crypto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.domains.session.Session;

class VaultParameterValidationUnitTest {

  private AesCipher aesCipher;

  @BeforeEach
  void setUp() {
    Session session = new Session();
    session.setPasswordVault("master-password");
    aesCipher = new AesCipher(session);
  }

  @Test
  @DisplayName("unknown format version is rejected instead of decrypted")
  void unknownFormatRejected() throws Exception {
    String body = validEnvelopeJson(3, 1000);
    assertThrows(EncryptionException.class, () -> aesCipher.decrypt(body));
  }

  @Test
  @DisplayName("absurd iteration count is rejected before derivation runs")
  void hugeIterationsRejected() {
    VaultEnvelope envelope = envelope(Integer.MAX_VALUE, 16, 12);
    assertThrows(EncryptionException.class, () -> VaultEnvelopeValidator.validate(envelope));
  }

  @Test
  @DisplayName("zero iterations is rejected")
  void zeroIterationsRejected() {
    assertThrows(EncryptionException.class,
        () -> VaultEnvelopeValidator.validate(envelope(0, 16, 12)));
  }

  @Test
  @DisplayName("wrong salt or IV length is rejected")
  void wrongLengthsRejected() {
    assertThrows(EncryptionException.class,
        () -> VaultEnvelopeValidator.validate(envelope(600000, 8, 12)));
    assertThrows(EncryptionException.class,
        () -> VaultEnvelopeValidator.validate(envelope(600000, 16, 8)));
  }

  private static VaultEnvelope envelope(int iterations, int saltBytes, int ivBytes) {
    return new VaultEnvelope(2,
        new VaultEnvelope.Kdf(VaultKeyDerivation.PBKDF2_ALG, iterations, encode(new byte[saltBytes])),
        encode(new byte[ivBytes]), encode(new byte[8]));
  }

  /** A genuinely decryptable envelope, so only the format check can reject it. */
  private static String validEnvelopeJson(int format, int iterations) throws Exception {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE,
        VaultKeyDerivation.deriveAesKey("master-password", salt, iterations),
        new GCMParameterSpec(128, iv));
    byte[] ct = cipher.doFinal("data".getBytes(StandardCharsets.UTF_8));
    return new Gson().toJson(new VaultEnvelope(format,
        new VaultEnvelope.Kdf(VaultKeyDerivation.PBKDF2_ALG, iterations, encode(salt)),
        encode(iv), encode(ct)));
  }

  private static String encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }
}
