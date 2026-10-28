package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * SNBT nesting on reading (R58): the level counter goes back down after each container and each
 * operation. Many sibling values at a shallow depth are accepted, far beyond 512 in total; a
 * counter that is never decremented would refuse them.
 */
class SnbtParserSiblingDepthTest {

  private static final int SIBLINGS = 600;

  static Stream<Arguments> siblingValues() {
    return Stream.of(
        Arguments.of("[]", ListTag.empty()),
        Arguments.of("{}", CompoundTag.empty()),
        Arguments.of("{a:1}", compound("a", intTag(1))),
        Arguments.of("[I;1]", IntArrayTag.of(1)),
        Arguments.of("bool(1)", byteTag(1)),
        Arguments.of("bool(bool(0))", byteTag(0)),
        Arguments.of(
            "uuid(\"35592e68-c4a6-428e-b15f-be52b701a331\")",
            IntArrayTag.of(895037032, -995736946, -1319125422, -1224629455)));
  }

  static String joined(String element, int times) {
    return String.join(",", Collections.nCopies(times, element));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("siblingValues")
  void readsSiblingsInAList(String element, NbtTag expected) throws NbtException {
    // spec: core-nbt R58 (the counter goes back down after each value; 600 siblings at level 2)
    NbtTag parsed = SnbtParser.parse("[" + joined(element, SIBLINGS) + "]");
    assertThat(parsed)
        .isInstanceOfSatisfying(
            ListTag.class,
            list -> {
              assertThat(list.elements()).hasSize(SIBLINGS);
              assertThat(list.elements()).allMatch(expected::equals);
            });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("siblingValues")
  void readsSiblingsInACompound(String element, NbtTag expected) throws NbtException {
    // spec: core-nbt R58 (same, as entries of a compound)
    var text = new StringBuilder("{");
    var entries = new LinkedHashMap<String, NbtTag>();
    for (int index = 0; index < SIBLINGS; index++) {
      text.append(index == 0 ? "" : ",").append('k').append(index).append(':').append(element);
      entries.put("k" + index, expected);
    }
    assertThat(SnbtParser.parse(text.append('}').toString())).isEqualTo(CompoundTag.of(entries));
  }

  @Test
  void readsSixHundredEmptyListsThenACompound() throws NbtException {
    // spec: core-nbt R58, R47: [[],[],...,{a:1}] has depth 2, the counter goes back to 1 after
    // each empty list; the list is heterogeneous (List and Compound)
    String text = "[" + joined("[]", SIBLINGS - 1) + ",{a:1}]";
    List<NbtTag> expected = new ArrayList<>();
    for (int index = 0; index < SIBLINGS - 1; index++) {
      expected.add(ListTag.empty());
    }
    expected.add(compound("a", intTag(1)));
    assertThat(SnbtParser.parse(text)).isEqualTo(ListTag.of(expected));
  }

  @Test
  void readsSiblingsAfterADeepBranch() throws NbtException {
    // spec: core-nbt R58: a first branch of 500 levels, then a second one of 512 levels next to
    // it; the counter went back to 1 between the two
    String first = "[".repeat(499) + "]".repeat(499);
    String second = "[".repeat(511) + "]".repeat(511);
    String text = "[" + first + "," + second + "]";
    assertThat(SnbtParser.parse(text)).isInstanceOf(ListTag.class);
  }
}
