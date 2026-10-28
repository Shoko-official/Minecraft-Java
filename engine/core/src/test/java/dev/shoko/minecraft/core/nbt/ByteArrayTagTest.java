package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ByteArrayTagTest {

  @Test
  void typeIsByteArray() {
    // spec: core-nbt R2
    assertThat(ByteArrayTag.of().type()).isEqualTo(TagType.BYTE_ARRAY);
  }

  @Test
  void exposesElementsInOrder() {
    // spec: core-nbt R9
    var tag = ByteArrayTag.of((byte) 1, (byte) -1, Byte.MIN_VALUE);
    assertThat(tag.size()).isEqualTo(3);
    assertThat(tag.get(0)).isEqualTo((byte) 1);
    assertThat(tag.get(1)).isEqualTo((byte) -1);
    assertThat(tag.get(2)).isEqualTo((byte) -128);
    assertThat(tag.toArray()).containsExactly(1, -1, -128);
  }

  @Test
  void emptyArrayHasNoElements() {
    // spec: core-nbt "Cas limites" (array of 0 elements)
    assertThat(ByteArrayTag.of().size()).isZero();
    assertThat(ByteArrayTag.of().toArray()).isEmpty();
  }

  @Test
  void copiesValuesIn() {
    // API contract: later changes to the given array do not affect the tag
    byte[] values = {1, 2};
    var tag = ByteArrayTag.of(values);
    values[0] = 9;
    assertThat(tag.get(0)).isEqualTo((byte) 1);
  }

  @Test
  void copiesValuesOut() {
    // API contract: changing the returned array does not affect the tag
    var tag = ByteArrayTag.of((byte) 1, (byte) 2);
    byte[] out = tag.toArray();
    out[0] = 9;
    assertThat(tag.toArray()).containsExactly(1, 2);
  }

  @Test
  void rejectsIndexOutOfRange() {
    var tag = ByteArrayTag.of((byte) 1);
    assertThatThrownBy(() -> tag.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> tag.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void equalsElementByElement() {
    // spec: core-nbt R37 (arrays compare element by element, the type must be the same)
    var tag = ByteArrayTag.of((byte) 1, (byte) -1);
    assertThat(tag).isEqualTo(ByteArrayTag.of((byte) 1, (byte) -1));
    assertThat(tag).hasSameHashCodeAs(ByteArrayTag.of((byte) 1, (byte) -1));
    assertThat(tag).isNotEqualTo(ByteArrayTag.of((byte) -1, (byte) 1));
    assertThat(tag).isNotEqualTo(ByteArrayTag.of((byte) 1));
    assertThat(ByteArrayTag.of()).isEqualTo(ByteArrayTag.of());
    assertThat(ByteArrayTag.of((byte) 1)).isNotEqualTo(IntArrayTag.of(1));
    assertThat(ByteArrayTag.of()).isNotEqualTo(LongArrayTag.of());
  }
}
