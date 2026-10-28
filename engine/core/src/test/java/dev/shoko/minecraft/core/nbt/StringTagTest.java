package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StringTagTest {

  @Test
  void typeIsString() {
    // spec: core-nbt R1
    assertThat(new StringTag("").type()).isEqualTo(TagType.STRING);
  }

  @Test
  void rejectsNull() {
    // API contract of StringTag: constructing with null throws NullPointerException
    assertThatThrownBy(() -> new StringTag(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void keepsUnpairedSurrogates() {
    // spec: core-nbt R12 (an isolated surrogate is accepted as is), provisional
    assertThat(new StringTag("\uD83D").value()).isEqualTo("\uD83D");
  }

  @Test
  void canHoldTextTooLongToWrite() {
    // spec: core-nbt R13 (only writing fails); 70000 bytes once encoded
    assertThat(new StringTag("a".repeat(70000)).value()).hasSize(70000);
  }

  @Test
  void equalsStringWithSameTextOnly() {
    // spec: core-nbt R37
    assertThat(new StringTag("1")).isEqualTo(new StringTag("1"));
    assertThat(new StringTag("1")).isNotEqualTo(new StringTag("1 "));
    assertThat(new StringTag("1")).isNotEqualTo(new IntTag(1));
  }
}
