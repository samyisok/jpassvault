package com.samyisok.jpassvaultclient.domains.options;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptionsToStringUnitTest {

  private Options options;

  @BeforeEach
  void setUp() {
    options = new Options();
    options.setDefaultData();
    options.setTokenApi("super-secret-token");
  }

  @Test
  @DisplayName("toString never contains the raw API token and renders it masked")
  void masksToken() {
    assertFalse(options.toString().contains("super-secret-token"),
        "token must be masked in logs");
    assertTrue(options.toString().contains("********"),
        "token must be visible as a mask, not silently dropped");
  }
}
