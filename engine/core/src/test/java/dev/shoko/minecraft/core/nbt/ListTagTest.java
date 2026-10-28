package dev.shoko.minecraft.core.nbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListTagTest {

  @Test
  void typeIsList() {
    // spec: core-nbt R1
    assertThat(ListTag.empty().type()).isEqualTo(TagType.LIST);
  }

  @Test
  void emptyListHasEndElementType() {
    // spec: core-nbt R21, R22
    assertThat(ListTag.empty().elementType()).isEqualTo(TagType.END);
    assertThat(ListTag.empty().elements()).isEmpty();
    assertThat(ListTag.of(List.of()).elementType()).isEqualTo(TagType.END);
  }

  @Test
  void listBuiltWithoutElementsEqualsEmptyList() {
    // spec: core-nbt R37 (two empty lists are equal whatever their element type)
    assertThat(ListTag.of(List.of())).isEqualTo(ListTag.empty());
    assertThat(ListTag.of(List.of())).hasSameHashCodeAs(ListTag.empty());
  }

  @Test
  void homogeneousListHasCommonElementType() {
    // spec: core-nbt R7
    assertThat(list(intTag(7), intTag(8)).elementType()).isEqualTo(TagType.INT);
    assertThat(list(stringTag("a")).elementType()).isEqualTo(TagType.STRING);
    assertThat(list(compound("a", intTag(1)), compound()).elementType())
        .isEqualTo(TagType.COMPOUND);
  }

  @Test
  void listOfListsOfDifferentElementTypesIsHomogeneous() {
    // spec: core-nbt R7 (each sub-list carries its own element type)
    var lists = list(list(intTag(1)), list(byteTag(2)), ListTag.empty());
    assertThat(lists.elementType()).isEqualTo(TagType.LIST);
  }

  @Test
  void keepsElementsInOrder() {
    // spec: core-nbt R7
    assertThat(list(intTag(7), intTag(8)).elements()).containsExactly(intTag(7), intTag(8));
  }

  @Test
  void heterogeneousListHasCompoundElementTypeAndUnwrappedElements() {
    // spec: core-nbt R47 (T37 [1,"a"]): elements stay unwrapped in memory
    var mixed = list(intTag(1), stringTag("a"));
    assertThat(mixed.elementType()).isEqualTo(TagType.COMPOUND);
    assertThat(mixed.elements()).containsExactly(intTag(1), stringTag("a"));
  }

  @Test
  void byteAndIntAreNotPromotedToOneType() {
    // spec: core-nbt R47 (T38 [1b,2]), provisional: heterogeneous list, no promotion
    var mixed = list(byteTag(1), intTag(2));
    assertThat(mixed.elementType()).isEqualTo(TagType.COMPOUND);
    assertThat(mixed.elements()).containsExactly(byteTag(1), intTag(2));
  }

  @Test
  void compoundMixedWithOtherTypesMakesHeterogeneousList() {
    // spec: core-nbt R47, provisional ([{a:1}, 2])
    var mixed = list(compound("a", intTag(1)), intTag(2));
    assertThat(mixed.elementType()).isEqualTo(TagType.COMPOUND);
    assertThat(mixed.elements()).containsExactly(compound("a", intTag(1)), intTag(2));
  }

  @Test
  void copiesElementsIn() {
    // API contract: later changes to the given list do not affect the tag
    var source = new ArrayList<NbtTag>(List.of(intTag(1)));
    var tag = ListTag.of(source);
    source.add(intTag(2));
    assertThat(tag.elements()).containsExactly(intTag(1));
  }

  @Test
  void elementsCannotBeModified() {
    // API contract: the list is immutable
    List<NbtTag> elements = list(intTag(1)).elements();
    assertThatThrownBy(() -> elements.add(intTag(2)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsNullElementsAndNullList() {
    // API contract: ListTag.of throws NullPointerException on null
    assertThatThrownBy(() -> ListTag.of(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ListTag.of(Arrays.asList(intTag(1), null)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void equalsSameElementsInSameOrder() {
    // spec: core-nbt R37 (lists compare elements in order)
    assertThat(list(intTag(1), intTag(2))).isEqualTo(list(intTag(1), intTag(2)));
    assertThat(list(intTag(1), intTag(2))).hasSameHashCodeAs(list(intTag(1), intTag(2)));
    assertThat(list(intTag(1), intTag(2))).isNotEqualTo(list(intTag(2), intTag(1)));
    assertThat(list(intTag(1))).isNotEqualTo(list(byteTag(1)));
    assertThat(list(intTag(1))).isNotEqualTo(ListTag.empty());
    assertThat(list(intTag(1))).isNotEqualTo(IntArrayTag.of(1));
  }
}
