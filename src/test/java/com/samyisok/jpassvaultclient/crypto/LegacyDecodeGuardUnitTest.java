package com.samyisok.jpassvaultclient.crypto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.samyisok.jpassvaultclient.domains.session.Session;

class LegacyDecodeGuardUnitTest {

  private AesCipher aesCipher;

  @BeforeEach
  void setUp() {
    Session session = new Session();
    session.setPasswordVault("master-password");
    aesCipher = new AesCipher(session);
  }

  @Test
  @DisplayName("feeding an envelope to the legacy base64/IV path raises and returns no plaintext")
  void envelopeFailsCleanlyOnLegacyPath() throws Exception {
    String envelope = aesCipher.encrypt("secret data");
    assertThrows(IllegalArgumentException.class, () -> aesCipher.decryptLegacy(envelope));
  }
}
