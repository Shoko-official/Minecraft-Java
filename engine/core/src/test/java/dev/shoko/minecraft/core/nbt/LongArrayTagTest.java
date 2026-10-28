package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LongArrayTagTest {

  @Test
  void typeIsLongArray() {
    // spec: core-nbt R2
    assertThat(LongArrayTag.of().type()).isEqualTo(TagType.LONG_ARRAY);
  }

  @Test
  void exposesElementsInOrder() {
    // spec: core-nbt R9
    var tag = LongArrayTag.of(1L, Long.MIN_VALUE);
    assertThat(tag.size()).isEqualTo(2);
    assertThat(tag.get(0)).isEqualTo(1L);
    assertThat(tag.get(1)).isEqualTo(Long.MIN_VALUE);
    assertThat(LongArrayTag.of().size()).isZero();
  }

  @Test
  void copiesValuesInAndOut() {
    // API contract: the tag is immutable
    long[] values = {1L, 2L};
    var tag = LongArrayTag.of(values);
    values[0] = 9L;
    long[] out = tag.toArray();
    out[1] = 9L;
    assertThat(tag.toArray()).containsExactly(1L, 2L);
  }

  @Test
  void rejectsIndexOutOfRange() {
    var tag = LongArrayTag.of();
    assertThatThrownBy(() -> tag.get(0)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void equalsElementByElement() {
    // spec: core-nbt R37
    assertThat(LongArrayTag.of(1L)).isEqualTo(LongArrayTag.of(1L));
    assertThat(LongArrayTag.of(1L)).hasSameHashCodeAs(LongArrayTag.of(1L));
    assertThat(LongArrayTag.of(1L)).isNotEqualTo(LongArrayTag.of(1L, 1L));
    assertThat(LongArrayTag.of(1L)).isNotEqualTo(ByteArrayTag.of((byte) 1));
  }
}
