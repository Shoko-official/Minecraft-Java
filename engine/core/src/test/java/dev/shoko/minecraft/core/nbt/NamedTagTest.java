package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NamedTagTest {

  @Test
  void keepsNameAsGiven() {
    // spec: core-nbt R14 (empty name), R15 (other names kept so that rewriting is identical)
    assertThat(new NamedTag("", new IntTag(1)).name()).isEmpty();
    assertThat(new NamedTag("x", new IntTag(1)).name()).isEqualTo("x");
  }

  @Test
  void rootsWithDifferentNamesAreDifferent() {
    assertThat(new NamedTag("x", new IntTag(1))).isEqualTo(new NamedTag("x", new IntTag(1)));
    assertThat(new NamedTag("x", new IntTag(1))).isNotEqualTo(new NamedTag("", new IntTag(1)));
  }

  @Test
  void rejectsNullComponents() {
    // API contract of NamedTag: constructing with null throws NullPointerException
    assertThatThrownBy(() -> new NamedTag(null, new IntTag(1)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new NamedTag("", null)).isInstanceOf(NullPointerException.class);
  }
}
