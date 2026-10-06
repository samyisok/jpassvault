package com.samyisok.jpassvaultclient.domains.vault;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VaultContainerToStringUnitTest {

  @Test
  @DisplayName("toString never contains the stored password")
  void hidesStoredPassword() {
    VaultContainer record = new VaultContainer("user@example.com", "super-secret");
    assertFalse(record.toString().contains("super-secret"),
        "diagnostic output must not leak the stored password");
  }
}
