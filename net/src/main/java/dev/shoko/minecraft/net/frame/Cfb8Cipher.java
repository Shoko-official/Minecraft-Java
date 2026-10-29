package dev.shoko.minecraft.net.frame;

import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** One direction of AES-128 CFB8 over the JDK provider, used in place on a continuous stream. */
final class Cfb8Cipher implements StreamCipher {

  private static final int SECRET_LENGTH = 16;

  private final Cipher cipher;

  Cfb8Cipher(int mode, byte[] sharedSecret) {
    if (sharedSecret.length != SECRET_LENGTH) {
      throw new IllegalArgumentException("the shared secret must be 16 bytes");
    }
    byte[] secret = sharedSecret.clone();
    try {
      cipher = Cipher.getInstance("AES/CFB8/NoPadding");
      cipher.init(mode, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("AES/CFB8/NoPadding is unavailable", e);
    }
  }

  @Override
  public void apply(byte[] data, int offset, int length) {
    Objects.checkFromIndexSize(offset, length, data.length);
    try {
      int produced = cipher.update(data, offset, length, data, offset);
      if (produced != length) {
        throw new IllegalStateException("CFB8 produced " + produced + " bytes for " + length);
      }
    } catch (ShortBufferException e) {
      throw new IllegalStateException("CFB8 output buffer too short", e);
    }
  }
}
