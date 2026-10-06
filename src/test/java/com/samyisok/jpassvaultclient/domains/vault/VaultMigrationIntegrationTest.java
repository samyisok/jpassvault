package com.samyisok.jpassvaultclient.domains.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.session.Session;

class VaultMigrationIntegrationTest {

  private static final String LEGACY_PASSWORD = "legacy-master-pass";

  @TempDir
  Path tempDir;

  private Session session;
  private Vault vault;
  private AesCipher aesCipher;
  private VaultLoader vaultLoader;
  private Path vaultFile;

  @BeforeEach
  void setUp() throws Exception {
    vaultFile = tempDir.resolve("jpassdb.xdb");
    try (InputStream in = getClass().getResourceAsStream("/legacy-vault-fixture.xdb")) {
      Files.copy(in, vaultFile, StandardCopyOption.REPLACE_EXISTING);
    }
    session = new Session();
    session.setPasswordVault(LEGACY_PASSWORD);
    aesCipher = new AesCipher(session);
    vault = new Vault();
    vaultLoader = new VaultLoader(vault, aesCipher,
        new FileVaultStore(() -> vaultFile),
        new FileVaultStore(() -> tempDir.resolve("jpassdb.bak.xdb")));
  }

  @Test
  @DisplayName("legacy vault opens with the correct password")
  void legacyVaultOpens() {
    vaultLoader.load();
    assertEquals("old-secret", vault.get("GitHub").getPassword());
  }

  @Test
  @DisplayName("wrong password rejects the legacy vault and leaves the file bytes unchanged")
  void wrongPasswordLeavesFileUntouched() throws Exception {
    byte[] before = Files.readAllBytes(vaultFile);
    session.setPasswordVault("wrong-password");
    assertFalse(vaultLoader.vaultPasswordIsValid());
    assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(vaultFile)));
  }

  @Test
  @DisplayName("first save after unlocking a legacy vault rewrites it as envelope v2")
  void saveUpgradesToEnvelope() throws Exception {
    vaultLoader.load();
    vaultLoader.save(vault);
    String body = Files.readString(vaultFile);
    assertTrue(body.startsWith("{"), "file must be the JSON envelope after save");
    assertTrue(body.contains("\"format\":2"), "envelope must carry format2");
  }
}
