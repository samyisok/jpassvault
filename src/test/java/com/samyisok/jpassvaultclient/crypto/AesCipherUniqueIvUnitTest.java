package com.samyisok.jpassvaultclient.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.samyisok.jpassvaultclient.domains.session.Session;

class AesCipherUniqueIvUnitTest {

  private AesCipher aesCipher;

  @BeforeEach
  void setUp() {
    Session session = new Session();
    session.setPasswordVault("master-password");
    aesCipher = new AesCipher(session);
  }

  @Test
  @DisplayName("encrypting the same plaintext twice yields different ciphertexts")
  void samePlaintextYieldsDifferentCiphertexts() throws Exception {
    String first = aesCipher.encrypt("identical content");
    String second = aesCipher.encrypt("identical content");
    assertNotEquals(first, second);
    assertEquals("identical content", aesCipher.decrypt(first));
  }
}
