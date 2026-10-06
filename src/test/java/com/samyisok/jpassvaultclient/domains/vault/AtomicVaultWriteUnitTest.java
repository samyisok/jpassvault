package com.samyisok.jpassvaultclient.domains.vault;

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
import com.samyisok.jpassvaultclient.FilePermissions;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.domains.session.Session;

class AtomicVaultWriteUnitTest {

  private static final Set<PosixFilePermission> OWNER_ONLY =
      EnumSet.copyOf(PosixFilePermissions.fromString("rw-------"));

  @TempDir
  Path tempDir;

  private Vault vault;
  private Path vaultFile;

  @BeforeEach
  void setUp() {
    vaultFile = tempDir.resolve("jpassdb.xdb");
    vault = new Vault();
    vault.put("GitHub", new VaultContainer("user", "secret"));
  }

  @Test
  @DisplayName("a save whose encryption fails leaves the previous file byte-identical")
  void failedSavePreservesPreviousFile() throws Exception {
    Files.writeString(vaultFile, "ORIGINAL");
    Session noPassword = new Session();
    loaderWith(noPassword).save(vault);
    assertEquals("ORIGINAL", Files.readString(vaultFile));
  }

  @Test
  @DisplayName("a successful save produces an owner-only vault file")
  void successfulSaveIsOwnerOnly() throws Exception {
    assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    loaderWith(sessionWithPassword()).save(vault);
    assertEquals(OWNER_ONLY, Files.getPosixFilePermissions(vaultFile));
  }

  private VaultLoader loaderWith(Session session) {
    return new VaultLoader(vault, new AesCipher(session),
        new FileVaultStore(() -> vaultFile),
        new FileVaultStore(() -> tempDir.resolve("jpassdb.bak.xdb")));
  }

  @Test
  @DisplayName("the vault directory is created owner-only")
  void vaultDirectoryIsOwnerOnly() throws Exception {
    assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    Path directory = tempDir.resolve("newvault");
    FilePermissions.createOwnerOnlyDirectory(directory);
    assertEquals(OWNER_ONLY, Files.getPosixFilePermissions(directory));
  }

  private static Session sessionWithPassword() {
    Session session = new Session();
    session.setPasswordVault("master-password");
    return session;
  }
}
