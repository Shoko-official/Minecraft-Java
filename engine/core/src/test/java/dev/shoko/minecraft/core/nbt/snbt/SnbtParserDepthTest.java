package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.nestedLists;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * SNBT nesting on reading (R58), vectors T53 to T59: at most 512 levels, root included; outside
 * quoted strings each opening brace and each opening bracket opens a level (typed arrays too);
 * beyond, {@code TOO_DEEP} at the character opening level 513, without exhausting the stack.
 */
class SnbtParserDepthTest {

  static String lists(int levels) {
    return "[".repeat(levels) + "]".repeat(levels);
  }

  static String compounds(int levels) {
    return "{a:".repeat(levels) + "1" + "}".repeat(levels);
  }

  /** {@code levels} nested compounds under "a", the deepest one being {a: 1}. */
  static CompoundTag nestedCompounds(int levels) {
    CompoundTag tree = compound("a", intTag(1));
    for (int level = 1; level < levels; level++) {
      tree = compound("a", tree);
    }
    return tree;
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "T53, 674cf3304bf7104f5ef200c1bb17b24a9b1da199f47cc76bcdc7fd030da23491",
    "T54, a9ecae6c77e13ad7240afd2d6c8aa7603375874dd33cf982251d2d533db6d32b",
    "T55, a424233baadccd66f816eefc25b8d44bb91216d9db55b5d20653c5927ac41990",
    "T56, dbdcf520bf0891d2cba8d7413dca9453f81a25a0fa3c5a505d9f8ca6b4808d56",
    "T57, 3a8ef8e5bc60fdcfab3c5552912012a3ccc625754e0b43a2c00519ae44870bef",
    "T58, ed31304f3386ed2ec9dad2f8f8cbb18f365a05c857721ec1b7e5cd789e116858",
    "T59, 76d6dde129a4906caca56dfb72a993aff9e8a65627bb839d9ecb7e1798e792ba"
  })
  void textMatchesSpecDigest(String id, String sha256) {
    // fixture check against core-nbt-vectors table T (T53 to T59)
    assertThat(Hex.sha256(text(id).getBytes(StandardCharsets.US_ASCII))).isEqualTo(sha256);
  }

  static String text(String id) {
    return switch (id) {
      case "T53" -> lists(512);
      case "T54" -> lists(513);
      case "T55" -> lists(100_000);
      case "T56" -> compounds(512);
      case "T57" -> compounds(513);
      case "T58" -> "[".repeat(512) + "[I;1]" + "]".repeat(512);
      case "T59" -> "[\"" + "[".repeat(600) + "\"]";
      default -> throw new IllegalArgumentException(id);
    };
  }

  @Test
  void reads512NestedLists() throws NbtException {
    // spec: core-nbt R58, T53
    assertThat(SnbtParser.parse(text("T53"))).isEqualTo(nestedLists(512, ListTag.empty()));
  }

  @Test
  void refuses513NestedListsAtTheOpeningBracket() {
    // spec: core-nbt R58, T54 (TOO_DEEP at offset 512)
    assertFails(() -> SnbtParser.parse(text("T54")), Reason.TOO_DEEP, 512);
  }

  @Test
  void refuses100000NestedListsWithoutStackOverflow() {
    // spec: core-nbt R58, T55 (TOO_DEEP at offset 512, the stack is not exhausted)
    assertFails(() -> SnbtParser.parse(text("T55")), Reason.TOO_DEEP, 512);
  }

  @Test
  void reads512NestedCompounds() throws NbtException {
    // spec: core-nbt R58, T56
    assertThat(SnbtParser.parse(text("T56"))).isEqualTo(nestedCompounds(512));
  }

  @Test
  void refuses513NestedCompoundsAtTheOpeningBrace() {
    // spec: core-nbt R58, T57 (TOO_DEEP at offset 1536 = 3 x 512)
    assertFails(() -> SnbtParser.parse(text("T57")), Reason.TOO_DEEP, 1536);
  }

  @Test
  void refuses100000NestedCompoundsWithoutStackOverflow() {
    // spec: core-nbt R58 (any depth, 100 000 levels included)
    assertFails(() -> SnbtParser.parse(compounds(100_000)), Reason.TOO_DEEP, 1536);
  }

  @Test
  void typedArrayOpensALevel() {
    // spec: core-nbt R58, T58 (the array opens level 513, at offset 512)
    assertFails(() -> SnbtParser.parse(text("T58")), Reason.TOO_DEEP, 512);
  }

  @Test
  void bracketsInQuotedStringsDoNotCount() throws NbtException {
    // spec: core-nbt R58, T59 (a List holding a String of 600 '[')
    assertThat(SnbtParser.parse(text("T59"))).isEqualTo(list(stringTag("[".repeat(600))));
    assertThat(SnbtParser.parse("{'" + "{".repeat(600) + "':1}"))
        .isEqualTo(compound("{".repeat(600), intTag(1)));
  }

  @Test
  void compoundArgumentReportsDepthNotType() {
    // spec: core-nbt R40, R58: a too deep compound is TOO_DEEP, not SNBT_NOT_A_COMPOUND
    assertFails(() -> SnbtParser.parseCompound(text("T57")), Reason.TOO_DEEP, 1536);
  }

  @Test
  void depthOffsetCountsLeadingBlanks() {
    // spec: core-nbt R58 (offset in UTF-16 code units of the whole text)
    assertFails(() -> SnbtParser.parse("  " + text("T54")), Reason.TOO_DEEP, 514);
  }
}
