package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Lower-case hexadecimal digests of files and streams. */
final class Hashes {

  private Hashes() {}

  static String hex(Path file, String algorithm) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return hex(in, algorithm);
    }
  }

  static String hex(InputStream in, String algorithm) throws IOException {
    MessageDigest digest = newDigest(algorithm);
    try (OutputStream sink = new DigestOutputStream(OutputStream.nullOutputStream(), digest)) {
      in.transferTo(sink);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  static String hex(byte[] data, String algorithm) {
    return HexFormat.of().formatHex(newDigest(algorithm).digest(data));
  }

  private static MessageDigest newDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(algorithm + " is required to be provided by the JDK", e);
    }
  }
}
