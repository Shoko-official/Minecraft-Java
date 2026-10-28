package dev.shoko.minecraft.core.random;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fingerprint of a resource identifier used to seed named sequences (R26, R31). Pure function.
 *
 * <p>Level S hypotheses (spec Q5): the hashed text is the full form {@code namespace:path} encoded
 * in UTF-8, so {@code test} and {@code minecraft:test} give the same fingerprint; bytes 0 to 7 of
 * the MD5 digest read big-endian form the low word, bytes 8 to 15 the high word (vectors V-X4).
 */
public final class IdentifierFingerprint {

  /** Namespace of an identifier written without one (R26). */
  private static final String DEFAULT_NAMESPACE_PREFIX = "minecraft:";

  private IdentifierFingerprint() {}

  /**
   * Returns the MD5 fingerprint of an identifier (R26). Text without a {@code :} is completed with
   * the {@code minecraft} namespace before hashing. The identifier is not otherwise validated.
   *
   * @param identifier the identifier, {@code namespace:path} or {@code path}
   * @return the digest as two words, low word from the first eight bytes
   */
  public static Seed128 of(String identifier) {
    String fullText =
        identifier.indexOf(':') < 0 ? DEFAULT_NAMESPACE_PREFIX + identifier : identifier;
    // ByteBuffer reads big-endian by default: the provisional byte order of R26.
    ByteBuffer digest = ByteBuffer.wrap(md5().digest(fullText.getBytes(StandardCharsets.UTF_8)));
    return new Seed128(digest.getLong(), digest.getLong());
  }

  private static MessageDigest md5() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("every Java platform must support MD5", e);
    }
  }
}
