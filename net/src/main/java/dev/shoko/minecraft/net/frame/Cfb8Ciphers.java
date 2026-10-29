package dev.shoko.minecraft.net.frame;

/**
 * Factory of the AES-128 CFB8 ciphers of a connection (R31, R32). The 16-byte shared secret is both
 * the key and the initial vector (vectors CH01 to CH03). One encryptor and one decryptor are
 * created per connection, each used for one direction only.
 */
public final class Cfb8Ciphers {

  private Cfb8Ciphers() {}

  /**
   * Creates the cipher for bytes sent.
   *
   * @param sharedSecret the 16-byte shared secret; the cipher copies it
   * @return a new encryptor
   * @throws IllegalArgumentException when {@code sharedSecret} is not 16 bytes long
   */
  public static StreamCipher encryptor(byte[] sharedSecret) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates the cipher for bytes received.
   *
   * @param sharedSecret the 16-byte shared secret; the cipher copies it
   * @return a new decryptor
   * @throws IllegalArgumentException when {@code sharedSecret} is not 16 bytes long
   */
  public static StreamCipher decryptor(byte[] sharedSecret) {
    return Skeleton.throwNotImplemented();
  }
}
