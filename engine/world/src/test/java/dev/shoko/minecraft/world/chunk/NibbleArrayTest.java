package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Hex;
import org.junit.jupiter.api.Test;

/** CF-R14 and vector LI1: 4096 values from 0 to 15, two per byte. */
class NibbleArrayTest {

  @Test
  void byteFormIs2048Bytes() {
    // spec: CF-R14
    assertThat(NibbleArray.BYTE_LENGTH).isEqualTo(2048);
    assertThat(NibbleArray.zeros().toBytes()).hasSize(2048).containsOnly((byte) 0);
  }

  @Test
  void vectorLi1() {
    // spec: LI1 (index 0 -> 15, 1 -> 1, 256 -> 7, 4095 -> 9)
    NibbleArray array = NibbleArray.zeros();
    array.set(0, 15);
    array.set(1, 1);
    array.set(256, 7);
    array.set(4095, 9);

    byte[] bytes = array.toBytes();

    assertThat(bytes[0]).isEqualTo((byte) 0x1f);
    assertThat(bytes[1]).isZero();
    assertThat(bytes[128]).isEqualTo((byte) 0x07);
    assertThat(bytes[2047]).isEqualTo((byte) 0x90).isEqualTo((byte) -112);
  }

  @Test
  void evenIndexIsTheLowNibbleAndOddIndexTheHighNibble() {
    // spec: CF-R14
    NibbleArray array = NibbleArray.ofBytes(bytesWith(10, 0xA5));

    assertThat(array.get(20)).isEqualTo(0x5);
    assertThat(array.get(21)).isEqualTo(0xA);
    assertThat(array.get(22)).isZero();
  }

  private static byte[] bytesWith(int at, int value) {
    byte[] bytes = new byte[2048];
    bytes[at] = (byte) value;
    return bytes;
  }

  @Test
  void filledSetsEveryValue() {
    // spec: NibbleArray.filled contract
    NibbleArray array = NibbleArray.filled(15);

    assertThat(array.toBytes()).containsOnly((byte) 0xff);
    assertThat(array.get(0)).isEqualTo(15);
    assertThat(array.get(4095)).isEqualTo(15);
    assertThat(NibbleArray.filled(3).toBytes()).containsOnly((byte) 0x33);
  }

  @Test
  void filledRejectsValuesOutOfRange() {
    // spec: NibbleArray.filled contract (0 to 15)
    Failures.expect(IllegalArgumentException.class, () -> NibbleArray.filled(16));
    Failures.expect(IllegalArgumentException.class, () -> NibbleArray.filled(-1));
  }

  @Test
  void setOnlyChangesItsOwnNibble() {
    NibbleArray array = NibbleArray.filled(15);

    array.set(7, 2);

    assertThat(array.get(6)).isEqualTo(15);
    assertThat(array.get(7)).isEqualTo(2);
    assertThat(array.get(8)).isEqualTo(15);
    assertThat(array.toBytes()[3]).isEqualTo((byte) 0x2f);
  }

  @Test
  void indexOutOfRangeIsRejected() {
    // spec: NibbleArray contract (IndexOutOfBoundsException)
    NibbleArray array = NibbleArray.zeros();

    Failures.expect(IndexOutOfBoundsException.class, () -> array.get(-1));
    Failures.expect(IndexOutOfBoundsException.class, () -> array.get(4096));
    Failures.expect(IndexOutOfBoundsException.class, () -> array.set(-1, 0));
    Failures.expect(IndexOutOfBoundsException.class, () -> array.set(4096, 0));
  }

  @Test
  void valueOutOfRangeIsRejected() {
    // spec: NibbleArray.set contract (IllegalArgumentException)
    NibbleArray array = NibbleArray.zeros();

    Failures.expect(IllegalArgumentException.class, () -> array.set(0, 16));
    Failures.expect(IllegalArgumentException.class, () -> array.set(0, -1));
    assertThat(array.get(0)).isZero();
  }

  @Test
  void ofBytesCopiesItsInput() {
    // spec: NibbleArray.ofBytes contract (copied)
    byte[] input = new byte[2048];
    NibbleArray array = NibbleArray.ofBytes(input);

    input[0] = 0x0f;

    assertThat(array.get(0)).isZero();
  }

  @Test
  void ofBytesRejectsAnotherLength() {
    // spec: NibbleArray.ofBytes contract (exactly 2048 bytes)
    Failures.expect(IllegalArgumentException.class, () -> NibbleArray.ofBytes(new byte[2047]));
    Failures.expect(IllegalArgumentException.class, () -> NibbleArray.ofBytes(new byte[2049]));
    Failures.expect(IllegalArgumentException.class, () -> NibbleArray.ofBytes(new byte[0]));
  }

  @Test
  void toBytesReturnsAFreshArray() {
    // spec: NibbleArray.toBytes contract (a new array)
    NibbleArray array = NibbleArray.filled(5);

    byte[] bytes = array.toBytes();
    bytes[0] = 0;

    assertThat(array.toBytes()[0]).isEqualTo((byte) 0x55);
  }

  @Test
  void copyIsIndependent() {
    NibbleArray array = NibbleArray.filled(5);

    NibbleArray copy = array.copy();
    copy.set(0, 1);
    array.set(1, 2);

    assertThat(copy.get(0)).isEqualTo(1);
    assertThat(copy.get(1)).isEqualTo(5);
    assertThat(array.get(0)).isEqualTo(5);
  }

  @Test
  void bytesRoundTripThroughTheArray() {
    // spec: CF-R14
    byte[] bytes = Hex.bytes("1f 00 07 90");
    byte[] padded = new byte[2048];
    System.arraycopy(bytes, 0, padded, 0, bytes.length);

    assertThat(NibbleArray.ofBytes(padded).toBytes()).isEqualTo(padded);
  }
}
