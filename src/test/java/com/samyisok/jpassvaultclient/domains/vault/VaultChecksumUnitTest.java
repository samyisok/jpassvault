package com.samyisok.jpassvaultclient.domains.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.samyisok.jpassvaultclient.FileVaultStore;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.session.Session;

class VaultChecksumUnitTest {

  @TempDir
  Path tempDir;

  private Vault vault;
  private VaultLoader loader;

  @BeforeEach
  void setUp() {
    vault = new Vault();
    vault.put("GitHub", new VaultContainer("user", "secret"));
    loader = loaderWith("master-password");
  }

  private VaultLoader loaderWith(String password) {
    Session session = new Session();
    session.setPasswordVault(password);
    return new VaultLoader(vault, new AesCipher(session),
        new FileVaultStore(() -> tempDir.resolve("vault.xdb")),
        new FileVaultStore(() -> tempDir.resolve("vault.bak.xdb")));
  }

  @Test
  @DisplayName("checksum is identical when the content is unchanged")
  void stableForUnchangedContent() throws Exception {
    assertEquals(loader.getVaultEncryptCheckSum(), loader.getVaultEncryptCheckSum());
  }

  @Test
  @DisplayName("adding a record changes the checksum")
  void changesWhenRecordAdded() throws Exception {
    String before = loader.getVaultEncryptCheckSum();
    vault.put("Bank", new VaultContainer("buser", "bsecret"));
    assertNotEquals(before, loader.getVaultEncryptCheckSum());
  }

  @Test
  @DisplayName("identical content under a different password yields a different checksum")
  void keyedByPassword() throws Exception {
    String mine = loader.getVaultEncryptCheckSum();
    assertNotEquals(mine, loaderWith("other-master-password").getVaultEncryptCheckSum());
  }
}
