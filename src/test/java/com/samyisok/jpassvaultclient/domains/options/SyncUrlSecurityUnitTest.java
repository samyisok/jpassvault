package com.samyisok.jpassvaultclient.domains.options;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncUrlSecurityUnitTest {

  private Options options;

  @BeforeEach
  void setUp() {
    options = new Options();
    options.setDefaultData();
  }

  @Test
  @DisplayName("plain http remote URL is rejected when configured")
  void rejectsPlainHttp() {
    assertThrows(IllegalArgumentException.class,
        () -> options.setApiUrl("http://sync.example.com"));
  }

  @Test
  @DisplayName("https remote URL is accepted")
  void acceptsHttps() {
    assertDoesNotThrow(() -> options.setApiUrl("https://sync.example.com"));
    assertEquals("https://sync.example.com", options.getApiUrl());
  }

  @Test
  @DisplayName("http on localhost and 127.0.0.1 stays usable")
  void acceptsLocalhostHttp() {
    assertDoesNotThrow(() -> options.setApiUrl("http://localhost:8080"));
    assertDoesNotThrow(() -> options.setApiUrl("http://127.0.0.1:8080"));
  }

  @Test
  @DisplayName("pre-send guard predicate refuses a non-https non-local URL")
  void guardRefusesRemoteHttp() {
    assertFalse(Options.isSecureUrl("http://evil.example"));
    assertTrue(Options.isSecureUrl("https://evil.example"));
    assertTrue(Options.isSecureUrl("http://localhost:9000"));
  }
}
