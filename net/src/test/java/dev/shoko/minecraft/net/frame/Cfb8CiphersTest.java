package dev.shoko.minecraft.net.frame;

import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.testing.Cfb8Model;
import dev.shoko.minecraft.net.testing.Fuzz;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R31 (AES-128 CFB8, key and initial vector equal to the shared secret),
// vectors CH01 to CH03
class Cfb8CiphersTest {

  private static final byte[] SECRET = Hex.ramp(16);

  /** CH01: the plaintext frame of F05. */
  private static final String PLAIN =
      "17 00 05 4e 6f 74 63 68 b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f";

  private static final String ENCRYPTED =
      "1d 16 6a dd 3a ef 9a bb 54 7c 35 d3 d2 49 fa d3 5a a2 be 70 f7 52 0f da";

  private static byte[] apply(StreamCipher cipher, byte[] data) {
    byte[] copy = data.clone();
    cipher.apply(copy, 0, copy.length);
    return copy;
  }

  @Test
  void theModelAgreesWithTheReferenceVector() {
    // spec: R31, vector CH01 (checks the independent model used by the other tests)
    assertThat(Hex.format(Cfb8Model.encrypt(SECRET, Hex.bytes(PLAIN)))).isEqualTo(ENCRYPTED);
  }

  @Test
  void theEncryptorMatchesVectorCh01() {
    // spec: R31, vector CH01
    StreamCipher encryptor = Cfb8Ciphers.encryptor(SECRET);
    assertThat(Hex.format(apply(encryptor, Hex.bytes(PLAIN)))).isEqualTo(ENCRYPTED);
  }

  @Test
  void theDecryptorRoundTripsVectorCh01() {
    // spec: R31, vector CH03
    StreamCipher decryptor = Cfb8Ciphers.decryptor(SECRET);
    assertThat(Hex.format(apply(decryptor, Hex.bytes(ENCRYPTED)))).isEqualTo(PLAIN);
  }

  @Test
  void splittingTheStreamAtAnyPositionGivesTheSameBytes() {
    // spec: R31, vector CH01 (split at byte 5 identical) and the continuity of the stream
    byte[] plain = Hex.bytes(PLAIN);
    for (int cut = 0; cut <= plain.length; cut++) {
      StreamCipher encryptor = Cfb8Ciphers.encryptor(SECRET);
      byte[] data = plain.clone();
      encryptor.apply(data, 0, cut);
      encryptor.apply(data, cut, data.length - cut);
      assertThat(Hex.format(data)).as("cut at %d", cut).isEqualTo(ENCRYPTED);
    }
  }

  @Test
  void restartingTheCipherChangesTheBytes() {
    // spec: R31, vector CH02 (a cipher restarted after byte 5 gives other bytes)
    byte[] data = Hex.bytes(PLAIN);
    Cfb8Ciphers.encryptor(SECRET).apply(data, 0, 5);
    Cfb8Ciphers.encryptor(SECRET).apply(data, 5, data.length - 5);
    assertThat(Hex.format(data)).isNotEqualTo(ENCRYPTED);
    assertThat(Hex.format(Arrays.copyOf(data, 5))).isEqualTo(ENCRYPTED.substring(0, 14));
  }

  @Test
  @Timeout(60)
  void anyChunkingOfARandomStreamMatchesTheReferenceCipher() {
    // spec: R31 (one continuous stream per direction), fixed seed
    Random random = new Random(0xC1F8L);
    for (int round = 0; round < 200; round++) {
      byte[] secret = new byte[16];
      random.nextBytes(secret);
      byte[] plain = Fuzz.bytes(random, 300);
      byte[] expected = Cfb8Model.encrypt(secret, plain);
      StreamCipher encryptor = Cfb8Ciphers.encryptor(secret);
      byte[] data = plain.clone();
      int position = 0;
      while (position < data.length) {
        int step = Math.min(data.length - position, 1 + random.nextInt(40));
        encryptor.apply(data, position, step);
        position += step;
      }
      assertThat(data).isEqualTo(expected);
      StreamCipher decryptor = Cfb8Ciphers.decryptor(secret);
      position = 0;
      while (position < data.length) {
        int step = Math.min(data.length - position, 1 + random.nextInt(40));
        decryptor.apply(data, position, step);
        position += step;
      }
      assertThat(data).isEqualTo(plain);
    }
  }

  @Test
  void aRangeIsTransformedInPlaceAndTheRestIsUntouched() {
    // spec: API contract of apply
    byte[] data = Hex.bytes("aa bb cc dd ee ff");
    byte[] before = data.clone();
    Cfb8Ciphers.encryptor(SECRET).apply(data, 2, 3);
    assertThat(Arrays.copyOfRange(data, 0, 2)).isEqualTo(Arrays.copyOfRange(before, 0, 2));
    assertThat(Arrays.copyOfRange(data, 5, 6)).isEqualTo(Arrays.copyOfRange(before, 5, 6));
    byte[] expected = Cfb8Model.encrypt(SECRET, Arrays.copyOfRange(before, 2, 5));
    assertThat(Arrays.copyOfRange(data, 2, 5)).isEqualTo(expected);
  }

  @Test
  void anEmptyRangeChangesNothingAndDoesNotAdvanceTheStream() {
    // spec: R31
    StreamCipher encryptor = Cfb8Ciphers.encryptor(SECRET);
    byte[] data = Hex.bytes(PLAIN);
    encryptor.apply(data, 0, 0);
    encryptor.apply(data, data.length, 0);
    encryptor.apply(data, 0, data.length);
    assertThat(Hex.format(data)).isEqualTo(ENCRYPTED);
  }

  @Test
  void aRangeOutsideTheArrayIsRefused() {
    // spec: API contract of apply
    StreamCipher cipher = Cfb8Ciphers.encryptor(SECRET);
    byte[] data = new byte[4];
    assertThrows(IndexOutOfBoundsException.class, () -> cipher.apply(data, -1, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> cipher.apply(data, 0, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> cipher.apply(data, 3, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> cipher.apply(data, 0, -1));
  }

  @Test
  void aSecretThatIsNotSixteenBytesIsRefused() {
    // spec: R31 (AES-128, 16-byte secret), API contract of encryptor and decryptor
    for (int length : new int[] {0, 1, 15, 17, 24, 32}) {
      byte[] secret = new byte[length];
      assertThrows(IllegalArgumentException.class, () -> Cfb8Ciphers.encryptor(secret));
      assertThrows(IllegalArgumentException.class, () -> Cfb8Ciphers.decryptor(secret));
    }
  }

  @Test
  void theCipherCopiesTheSecret() {
    // spec: API contract of encryptor and decryptor (the cipher copies it)
    byte[] secret = Hex.ramp(16);
    StreamCipher encryptor = Cfb8Ciphers.encryptor(secret);
    Arrays.fill(secret, (byte) 0x55);
    assertThat(Hex.format(apply(encryptor, Hex.bytes(PLAIN)))).isEqualTo(ENCRYPTED);
  }

  @Test
  void aDecryptorAndAnEncryptorAreIndependentStreams() {
    // spec: R31 (one cipher per direction), API contract of Cfb8Ciphers
    StreamCipher encryptor = Cfb8Ciphers.encryptor(SECRET);
    StreamCipher decryptor = Cfb8Ciphers.decryptor(SECRET);
    byte[] first = apply(encryptor, Hex.bytes("01 02 03"));
    byte[] second = apply(encryptor, Hex.bytes("04 05"));
    assertThat(apply(decryptor, first)).containsExactly(1, 2, 3);
    assertThat(apply(decryptor, second)).containsExactly(4, 5);
    Cipher model = Cfb8Model.cipher(SECRET, Cipher.ENCRYPT_MODE);
    assertThat(Hex.format(Hex.concat(first, second)))
        .isEqualTo(Hex.format(model.update(Hex.bytes("01 02 03 04 05"))));
  }
}
