package dev.shoko.minecraft.core.nbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

class CompoundTagTest {

  @Test
  void typeIsCompound() {
    // spec: core-nbt R1
    assertThat(CompoundTag.empty().type()).isEqualTo(TagType.COMPOUND);
  }

  @Test
  void emptyCompoundHasNoEntries() {
    // spec: core-nbt R6
    assertThat(CompoundTag.empty().entries()).isEmpty();
    assertThat(CompoundTag.of(new LinkedHashMap<String, NbtTag>())).isEqualTo(CompoundTag.empty());
  }

  @Test
  void keepsInsertionOrder() {
    // spec: core-nbt R30 (V21: z then a), not lexicographic
    var tag = compound("z", byteTag(1), "a", byteTag(2), "m", byteTag(3));
    assertThat(tag.entries().sequencedKeySet()).containsExactly("z", "a", "m");
  }

  @Test
  void getReturnsStoredValueOfAnyType() {
    var tag = compound("a", stringTag("x"), "b", list(intTag(1)));
    assertThat(tag.get("a")).contains(stringTag("x"));
    assertThat(tag.get("b")).contains(list(intTag(1)));
    assertThat(tag.get("c")).isEmpty();
  }

  @Test
  void copiesEntriesIn() {
    // API contract: later changes to the given map do not affect the tag
    var source = new LinkedHashMap<String, NbtTag>();
    source.put("a", intTag(1));
    var tag = CompoundTag.of(source);
    source.put("b", intTag(2));
    source.put("a", intTag(9));
    assertThat(tag.entries()).containsExactly(Map.entry("a", intTag(1)));
  }

  @Test
  void entriesCannotBeModified() {
    // API contract: the compound is immutable
    SequencedMap<String, NbtTag> entries = compound("a", intTag(1)).entries();
    assertThatThrownBy(() -> entries.put("b", intTag(2)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsNullKeysAndValues() {
    // API contract: CompoundTag.of throws NullPointerException on null
    var nullKey = new LinkedHashMap<String, NbtTag>();
    nullKey.put(null, intTag(1));
    var nullValue = new LinkedHashMap<String, NbtTag>();
    nullValue.put("a", null);
    assertThatThrownBy(() -> CompoundTag.of(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CompoundTag.of(nullKey)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CompoundTag.of(nullValue)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void equalityIgnoresKeyOrder() {
    // spec: core-nbt R28, R37, probe PE1 ({c:{y:2,x:1}} matches {c:{x:1,y:2}})
    var first = compound("x", intTag(1), "y", intTag(2));
    var second = compound("y", intTag(2), "x", intTag(1));
    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void equalityRequiresSameKeysAndEqualValues() {
    // spec: core-nbt R37 (probe PE1: {a:1b} differs from {a:1}, 0.0d from -0.0d)
    assertThat(compound("a", byteTag(1))).isNotEqualTo(compound("a", intTag(1)));
    assertThat(compound("f", doubleTag(0.0))).isNotEqualTo(compound("f", doubleTag(-0.0)));
    assertThat(compound("a", intTag(1))).isNotEqualTo(compound("b", intTag(1)));
    assertThat(compound("a", intTag(1))).isNotEqualTo(compound("a", intTag(1), "b", intTag(2)));
    assertThat(compound("a", intTag(1))).isNotEqualTo(CompoundTag.empty());
    assertThat(CompoundTag.empty()).isNotEqualTo(ListTag.empty());
  }

  @Test
  void nestedEqualityIgnoresKeyOrderAtEveryLevel() {
    // spec: core-nbt R28, R37
    var first = compound("a", byteTag(1), "c", compound("x", intTag(1), "y", intTag(2)));
    var second = compound("c", compound("y", intTag(2), "x", intTag(1)), "a", byteTag(1));
    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);
  }
}
