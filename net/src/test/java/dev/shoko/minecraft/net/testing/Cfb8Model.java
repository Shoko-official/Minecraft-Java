package dev.shoko.minecraft.net.testing;

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-128 CFB8 with the key as initial vector (rule R31), through the standard Java cipher: an
 * independent model for the stream ciphers of the module under test.
 */
public final class Cfb8Model {

  private Cfb8Model() {}

  /** Returns a continuing cipher in {@link Cipher#ENCRYPT_MODE} or {@link Cipher#DECRYPT_MODE}. */
  public static Cipher cipher(byte[] secret, int mode) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
      cipher.init(mode, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
      return cipher;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Encrypts a whole stream with a fresh cipher. */
  public static byte[] encrypt(byte[] secret, byte[] plain) {
    return plain.length == 0 ? plain : cipher(secret, Cipher.ENCRYPT_MODE).update(plain);
  }

  /** Decrypts a whole stream with a fresh cipher. */
  public static byte[] decrypt(byte[] secret, byte[] encrypted) {
    return encrypted.length == 0
        ? encrypted
        : cipher(secret, Cipher.DECRYPT_MODE).update(encrypted);
  }
}
