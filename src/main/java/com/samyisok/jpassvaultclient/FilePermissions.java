package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

/**
 * Owner-only safe file handling (design D5/D11): files that hold secrets are
 * owner-only from creation, and writes are atomic so a failed write leaves
 * the previous file untouched. No-ops on filesystems without POSIX support.
 */
public final class FilePermissions {

  public static final Set<PosixFilePermission> OWNER_ONLY =
      EnumSet.copyOf(PosixFilePermissions.fromString("rw-------"));

  private static final String TEMP_SUFFIX = ".tmp";

  private FilePermissions() {
  }

  /** Creates a directory owner-only (0700) where POSIX is supported. */
  public static void createOwnerOnlyDirectory(Path directory) throws IOException {
    if (posix()) {
      Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
    } else {
      Files.createDirectory(directory);
    }
  }

  /**
   * Writes content atomically to {@code destination}, owner-only from
   * creation: a temp file in the same directory (0600 where supported), then
   * an atomic move. Any existing destination is left untouched on failure.
   */
  public static void writeOwnerOnly(Path destination, String content) throws IOException {
    Path directory = destination.toAbsolutePath().getParent();
    Path temp = createOwnerOnlyTempFile(directory, destination.getFileName().toString());
    try {
      Files.writeString(temp, content);
      moveIntoPlace(temp, destination);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static Path createOwnerOnlyTempFile(Path directory, String prefix) throws IOException {
    if (posix()) {
      return Files.createTempFile(directory, prefix + ".", TEMP_SUFFIX,
          PosixFilePermissions.asFileAttribute(OWNER_ONLY));
    }
    return Files.createTempFile(directory, prefix + ".", TEMP_SUFFIX);
  }

  private static void moveIntoPlace(Path temp, Path destination) throws IOException {
    try {
      Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static boolean posix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }
}
