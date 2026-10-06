package com.samyisok.jpassvaultclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.options.OptionsLoader;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultContainer;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;

class FilePermissionsUnitTest {

  private static final Set<PosixFilePermission> OWNER_ONLY =
      EnumSet.copyOf(PosixFilePermissions.fromString("rw-------"));

  @TempDir
  Path tempDir;

  @BeforeEach
  void posixOnly() {
    assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
        "POSIX permissions not supported here");
  }

  @Test
  @DisplayName("saved vault file carries owner-only permissions")
  void vaultFileIsOwnerOnly() throws Exception {
    Path vaultFile = tempDir.resolve("jpassdb.xdb");
    Session session = new Session();
    session.setPasswordVault("master-password");
    Vault vault = new Vault();
    vault.put("GitHub", new VaultContainer("user", "secret"));
    new VaultLoader(vault, new AesCipher(session),
        new FileVaultStore(() -> vaultFile),
        new FileVaultStore(() -> tempDir.resolve("jpassdb.bak.xdb"))).save(vault);
    assertEquals(OWNER_ONLY, Files.getPosixFilePermissions(vaultFile));
  }

  @Test
  @DisplayName("saved settings file carries owner-only permissions")
  void settingsFileIsOwnerOnly() throws Exception {
    Path settingsFile = tempDir.resolve("config.json");
    Options options = new Options();
    options.setDefaultData();
    options.setTokenApi("secret-token");
    new OptionsLoader(options).save(options, settingsFile);
    assertEquals(OWNER_ONLY, Files.getPosixFilePermissions(settingsFile));
  }
}
