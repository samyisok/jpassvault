package com.samyisok.jpassvaultclient.crypto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import javax.crypto.SecretKeyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.samyisok.jpassvaultclient.domains.session.Session;

class VaultKeyDerivationUnitTest {

  private AesCipher aesCipher;

  @BeforeEach
  void setUp() {
    Session session = new Session();
    session.setPasswordVault("master-password");
    aesCipher = new AesCipher(session);
  }

  @Test
  @DisplayName("one encrypt and one decrypt each run exactly one PBKDF2 derivation")
  void singleDerivationPerOperation() throws Exception {
    try (MockedStatic<SecretKeyFactory> kdf = mockStatic(SecretKeyFactory.class,
        CALLS_REAL_METHODS)) {
      String body = aesCipher.encrypt("data");
      kdf.verify(() -> SecretKeyFactory.getInstance(any()), times(1));
      aesCipher.decrypt(body);
      kdf.verify(() -> SecretKeyFactory.getInstance(any()), times(2));
    }
  }
}
