package com.samyisok.jpassvaultclient.domains.vault;

import java.io.IOException;

/**
 * Persistence port for the encrypted vault body (design D17). The domain
 * layer reads and writes opaque encrypted text and knows nothing about files,
 * paths, or permissions; an infrastructure adapter supplies those.
 */
public interface VaultStore {

  boolean exists();

  String read() throws IOException;

  void write(String content) throws IOException;
}
