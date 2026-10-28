package dev.shoko.minecraft.core.random;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fingerprint of a resource identifier used to seed named sequences (R26, R31). Pure function.
 *
 * <p>Level S hypotheses (spec Q5): the hashed text is the full form {@code namespace:path} encoded
 * in UTF-8, so {@code test}, {@code :test} and {@code minecraft:test} give the same fingerprint;
 * bytes 0 to 7 of the MD5 digest read big-endian form the low word, bytes 8 to 15 the high word
 * (vectors V-X4).
 */
public final class IdentifierFingerprint {

  /** Namespace of an identifier written without one, or with an empty one (R26, provisional). */
  private static final String DEFAULT_NAMESPACE = "minecraft";

  private IdentifierFingerprint() {}

  /**
   * Returns the MD5 fingerprint of an identifier (R26). Before hashing, the identifier is completed
   * with the {@code minecraft} namespace (provisional normalization, R26): text without a {@code :}
   * becomes {@code minecraft:text}, and an empty namespace ({@code :test}) becomes {@code
   * minecraft:test}. The empty string is therefore hashed as {@code minecraft:}.
   *
   * <p>Known limit: the identifier is not validated. Upper case, several {@code :}, or an isolated
   * surrogate (encoded as {@code ?} by Java's UTF-8 encoder) are hashed as given; validation is the
   * caller's job.
   *
   * @param identifier the identifier, {@code namespace:path}, {@code :path} or {@code path}
   * @return the digest as two words, low word from the first eight bytes
   * @throws NullPointerException when {@code identifier} is {@code null}
   */
  public static Seed128 of(String identifier) {
    byte[] text = completeForm(identifier).getBytes(StandardCharsets.UTF_8);
    // ByteBuffer reads big-endian by default: the provisional byte order of R26.
    ByteBuffer digest = ByteBuffer.wrap(md5().digest(text));
    return new Seed128(digest.getLong(), digest.getLong());
  }

  private static String completeForm(String identifier) {
    if (identifier.indexOf(':') < 0) {
      return DEFAULT_NAMESPACE + ":" + identifier;
    }
    return identifier.startsWith(":") ? DEFAULT_NAMESPACE + identifier : identifier;
  }

  private static MessageDigest md5() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("every Java platform must support MD5", e);
    }
  }
}
