package dev.shoko.minecraft.net.buffer;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.testing.Hex;
import org.junit.jupiter.api.Test;

// spec: API contract of ByteBlob (immutable bytes with a content-based equals)
class ByteBlobTest {

  @Test
  void aBlobCopiesItsInput() {
    byte[] source = Hex.bytes("01 02 03");
    ByteBlob blob = ByteBlob.of(source);
    source[0] = 0x7f;
    assertThat(blob.toByteArray()).containsExactly(1, 2, 3);
    assertThat(blob.length()).isEqualTo(3);
  }

  @Test
  void toByteArrayReturnsACopy() {
    ByteBlob blob = ByteBlob.of(Hex.bytes("01 02 03"));
    byte[] copy = blob.toByteArray();
    copy[1] = 0;
    assertThat(blob.toByteArray()).containsExactly(1, 2, 3);
  }

  @Test
  void anEmptyBlobHasLengthZero() {
    assertThat(ByteBlob.of(new byte[0]).length()).isZero();
    assertThat(ByteBlob.of(new byte[0]).toByteArray()).isEmpty();
  }

  @Test
  void equalityIsByContent() {
    ByteBlob a = ByteBlob.of(Hex.bytes("01 02 03"));
    ByteBlob b = ByteBlob.of(Hex.bytes("01 02 03"));
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(ByteBlob.of(Hex.bytes("01 02 04")));
    assertThat(a).isNotEqualTo(ByteBlob.of(Hex.bytes("01 02")));
    assertThat(a).isNotEqualTo(ByteBlob.of(Hex.bytes("03 02 01")));
    assertThat(a).isNotEqualTo(null).isNotEqualTo("01 02 03");
    assertThat(ByteBlob.of(new byte[0])).isEqualTo(ByteBlob.of(new byte[0]));
  }

  @Test
  void theDescriptionGivesTheLength() {
    // spec: API contract of toString (the length, not the content)
    ByteBlob blob = ByteBlob.of(Hex.filled(0x5a, 1234));
    assertThat(blob.toString()).contains("1234");
  }
}
