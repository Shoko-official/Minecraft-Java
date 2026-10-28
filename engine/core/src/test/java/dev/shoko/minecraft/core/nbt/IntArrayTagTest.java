package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IntArrayTagTest {

  @Test
  void typeIsIntArray() {
    // spec: core-nbt R1
    assertThat(IntArrayTag.of().type()).isEqualTo(TagType.INT_ARRAY);
  }

  @Test
  void holdsUuidAsFourIntsMostSignificantFirst() {
    // spec: core-nbt R8 (UUID 35592e68-c4a6-428e-b15f-be52b701a331)
    var tag = IntArrayTag.of(895037032, -995736946, -1319125422, -1224629455);
    assertThat(tag.size()).isEqualTo(4);
    assertThat(tag.get(0)).isEqualTo(0x35592e68);
    assertThat(tag.get(1)).isEqualTo(0xc4a6428e);
    assertThat(tag.get(2)).isEqualTo(0xb15fbe52);
    assertThat(tag.get(3)).isEqualTo(0xb701a331);
  }

  @Test
  void emptyArrayHasNoElements() {
    // spec: core-nbt "Cas limites" (Int_Array of 0 elements)
    assertThat(IntArrayTag.of().size()).isZero();
    assertThat(IntArrayTag.of().toArray()).isEmpty();
  }

  @Test
  void copiesValuesInAndOut() {
    // API contract: the tag is immutable
    int[] values = {1, 2};
    var tag = IntArrayTag.of(values);
    values[0] = 9;
    int[] out = tag.toArray();
    out[1] = 9;
    assertThat(tag.toArray()).containsExactly(1, 2);
  }

  @Test
  void rejectsIndexOutOfRange() {
    var tag = IntArrayTag.of(1);
    assertThatThrownBy(() -> tag.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void equalsElementByElement() {
    // spec: core-nbt R37
    assertThat(IntArrayTag.of(1, 2)).isEqualTo(IntArrayTag.of(1, 2));
    assertThat(IntArrayTag.of(1, 2)).hasSameHashCodeAs(IntArrayTag.of(1, 2));
    assertThat(IntArrayTag.of(1, 2)).isNotEqualTo(IntArrayTag.of(2, 1));
    assertThat(IntArrayTag.of(1)).isNotEqualTo(LongArrayTag.of(1L));
    assertThat(IntArrayTag.of(1, 2)).isNotEqualTo(Tags.list(Tags.intTag(1), Tags.intTag(2)));
  }
}
