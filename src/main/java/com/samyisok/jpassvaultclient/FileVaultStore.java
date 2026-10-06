package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import com.samyisok.jpassvaultclient.domains.vault.VaultStore;

/**
 * File-backed {@link VaultStore} adapter (design D17): resolves the target
 * path lazily so runtime settings changes take effect, reads as text, and
 * writes atomically with owner-only permissions via {@link FilePermissions}.
 */
public class FileVaultStore implements VaultStore {

  private final Supplier<Path> pathSupplier;

  public FileVaultStore(Supplier<Path> pathSupplier) {
    this.pathSupplier = pathSupplier;
  }

  @Override
  public boolean exists() {
    return Files.isRegularFile(pathSupplier.get());
  }

  @Override
  public String read() throws IOException {
    return Files.readString(pathSupplier.get());
  }

  @Override
  public void write(String content) throws IOException {
    FilePermissions.writeOwnerOnly(pathSupplier.get(), content);
  }
}
