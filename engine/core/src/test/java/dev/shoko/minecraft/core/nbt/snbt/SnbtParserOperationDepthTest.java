package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SNBT nesting on reading (R58), extended to the operations: the opening parenthesis of {@code
 * bool(} and {@code uuid(} opens a level, counted with the braces and brackets. The 513th level is
 * refused with {@code TOO_DEEP} at the character that opens it, whatever the text length, without
 * exhausting the stack.
 */
class SnbtParserOperationDepthTest {

  private static final String UUID_TEXT = "\"35592e68-c4a6-428e-b15f-be52b701a331\"";

  /** The offset of the parenthesis of the 513th {@code bool(} or {@code uuid(}: 5 x 513 - 1. */
  private static final int OFFSET_OF_513TH_PARENTHESIS = 2564;

  /**
   * {@code opening} repeated, the core, then {@code closing} repeated, the same number of times.
   */
  static String wrapped(String opening, int times, String core, String closing) {
    return opening.repeat(times) + core + closing.repeat(times);
  }

  static String bools(int levels, String core) {
    return wrapped("bool(", levels, core, ")");
  }

  /**
   * Parses on a thread with the default stack size: the test thread of the build may have a larger
   * one. A {@link StackOverflowError} is rethrown by the caller and fails the test.
   */
  static NbtTag parseOnDefaultStack(String text) throws Throwable {
    var thrown = new AtomicReference<Throwable>();
    var result = new AtomicReference<NbtTag>();
    var thread =
        new Thread(
            null,
            () -> {
              try {
                result.set(SnbtParser.parse(text));
              } catch (Throwable failure) {
                thrown.set(failure);
              }
            },
            "snbt-default-stack",
            0);
    thread.start();
    thread.join();
    if (thrown.get() != null) {
      throw thrown.get();
    }
    return result.get();
  }

  @ParameterizedTest(name = "bool({0})")
  @CsvSource({"1, 1", "0, 0", "7, 1", "false, 0", "true, 1"})
  void reads512NestedBoolOperations(String core, int expected) throws NbtException {
    // spec: core-nbt R58, R50: bool(bool(1)) is valid, 512 chained operations are 512 levels
    assertThat(SnbtParser.parse(bools(512, core))).isEqualTo(byteTag(expected));
  }

  @Test
  void refuses513NestedBoolOperationsAtTheOpeningParenthesis() {
    // spec: core-nbt R58 (the parenthesis of the 513th bool( is at offset 5 x 513 - 1)
    assertFails(
        () -> SnbtParser.parse(bools(513, "1")), Reason.TOO_DEEP, OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refuses513NestedUuidOperationsAtTheOpeningParenthesis() {
    // spec: core-nbt R58: uuid( opens a level like bool(; the depth is refused before the
    // argument kind is checked
    assertFails(
        () -> SnbtParser.parse(wrapped("uuid(", 513, UUID_TEXT, ")")),
        Reason.TOO_DEEP,
        OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refuses513AlternatingUuidAndBoolOperations() {
    // spec: core-nbt R58: the two operations share one counter; 256 pairs then one more bool(
    String text = wrapped("bool(uuid(", 256, "bool(1)", "))");
    assertFails(() -> SnbtParser.parse(text), Reason.TOO_DEEP, OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refusesUnbalancedOperationPrefixAtTheSameOffset() {
    // spec: core-nbt R58: the refusal does not depend on what follows the 513th parenthesis
    assertFails(
        () -> SnbtParser.parse("bool(".repeat(513)), Reason.TOO_DEEP, OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refuses100000NestedBoolOperationsWithoutStackOverflow() {
    // spec: core-nbt R58 (any depth, 100 000 levels included, default thread stack)
    assertFails(
        () -> parseOnDefaultStack("bool(".repeat(100_000)),
        Reason.TOO_DEEP,
        OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refuses100000NestedUuidOperationsWithoutStackOverflow() {
    // spec: core-nbt R58 (same, with uuid)
    assertFails(
        () -> parseOnDefaultStack("uuid(".repeat(100_000)),
        Reason.TOO_DEEP,
        OFFSET_OF_513TH_PARENTHESIS);
  }

  @Test
  void refuses100000ListsOfBoolOperationsWithoutStackOverflow() {
    // spec: core-nbt R58: [bool( x 100 000: level 513 is the bracket of the 257th unit, 6 x 256
    assertFails(() -> parseOnDefaultStack("[bool(".repeat(100_000)), Reason.TOO_DEEP, 1536);
  }

  @Test
  void refuses100000CompoundsOfBoolOperationsWithoutStackOverflow() {
    // spec: core-nbt R58: {v:bool( x 100 000: level 513 is the brace of the 257th unit, 8 x 256
    assertFails(() -> parseOnDefaultStack("{v:bool(".repeat(100_000)), Reason.TOO_DEEP, 2048);
  }

  @Test
  void bracketsAndOperationsShareTheCounterInsideAList() {
    // spec: core-nbt R58: [bool( x 257 is 514 levels, the 513th is the bracket at 6 x 256
    assertFails(() -> SnbtParser.parse(wrapped("[bool(", 257, "1", ")]")), Reason.TOO_DEEP, 1536);
  }

  @Test
  void bracketAfter256ListOfBoolPairsOpensLevel513() {
    // spec: core-nbt R58: "[" then 256 x "[bool(" is 1 + 2 x 256 = 513 levels, the last
    // parenthesis (offset 1 + 6 x 255 + 5) opens level 513
    String text = "[" + wrapped("[bool(", 256, "1", ")]") + "]";
    assertFails(() -> SnbtParser.parse(text), Reason.TOO_DEEP, 1536);
  }

  @Test
  void bracesAndOperationsShareTheCounterInsideACompound() {
    // spec: core-nbt R58: {v:bool( x 257, the 513th level is the brace at 8 x 256
    assertFails(() -> SnbtParser.parse(wrapped("{v:bool(", 257, "1", ")}")), Reason.TOO_DEEP, 2048);
  }

  @ParameterizedTest(name = "{0} compounds, {1} lists, {2} operations")
  @CsvSource({"256, 128, 128", "511, 0, 1", "0, 511, 1", "1, 510, 1", "0, 0, 512", "100, 200, 212"})
  void readsMixedNestingAt512Levels(int compounds, int lists, int operations) throws NbtException {
    // spec: core-nbt R58: braces, brackets and parentheses are summed; 512 is accepted. The
    // operations are innermost: bool( takes a number, not a container (R50).
    assertThat(SnbtParser.parse(mixed(compounds, lists, operations)))
        .isEqualTo(mixedTree(compounds, lists));
  }

  @ParameterizedTest(name = "{0} compounds, {1} lists, {2} operations, offset {3}")
  @CsvSource({
    "256, 128, 129, 1540",
    "511, 0, 2, 1542",
    "0, 511, 2, 520",
    "1, 510, 2, 522",
    "0, 0, 513, 2564",
    "100, 200, 213, 1564"
  })
  void refusesMixedNestingAt513LevelsAtTheOpeningParenthesis(
      int compounds, int lists, int operations, int offset) {
    // spec: core-nbt R58: the offset is the parenthesis of the operation that opens level 513:
    // 3 x compounds + lists + 5 x (operations - 1) + 4
    assertFails(
        () -> SnbtParser.parse(mixed(compounds, lists, operations)), Reason.TOO_DEEP, offset);
  }

  @ParameterizedTest(name = "{0} operations then {1} lists, offset {2}")
  @CsvSource({"100, 413, 912", "1, 512, 516", "512, 1, 2560"})
  void refusesBracketThatOpensLevel513AfterOperations(int operations, int lists, int offset) {
    // spec: core-nbt R58: the 513th level is a bracket here; 5 x operations + (513 - operations -
    // 1) is its offset. The argument kind is not checked: the depth is refused first.
    String text = "bool(".repeat(operations) + "[".repeat(lists) + "]".repeat(lists);
    assertFails(() -> SnbtParser.parse(text + ")".repeat(operations)), Reason.TOO_DEEP, offset);
  }

  @Test
  void readsBoolOperationInsideStructuresAtLevel512() throws NbtException {
    // spec: core-nbt R58: the operation opens the 512th level inside 511 lists
    assertThat(SnbtParser.parse(wrapped("[", 511, "bool(1)", "]")))
        .isEqualTo(nestedLists(511, byteTag(1)));
  }

  @Test
  void readsUuidOperationInsideStructuresAtLevel512() throws NbtException {
    // spec: core-nbt R58, R50, R8: uuid( opens the 512th level inside 511 lists
    NbtTag expected = IntArrayTag.of(895037032, -995736946, -1319125422, -1224629455);
    assertThat(SnbtParser.parse(wrapped("[", 511, "uuid(" + UUID_TEXT + ")", "]")))
        .isEqualTo(nestedLists(511, expected));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"bool(1)", "uuid(" + UUID_TEXT + ")"})
  void refusesOperationInsideStructuresAtLevel513(String operation) {
    // spec: core-nbt R58: 512 lists, then the parenthesis opens level 513 at offset 512 + 4
    assertFails(() -> SnbtParser.parse(wrapped("[", 512, operation, "]")), Reason.TOO_DEEP, 516);
  }

  /**
   * The compounds {@code {v:}}, then the lists, then the operations {@code bool(}, around a 1 and
   * their closers.
   */
  static String mixed(int compounds, int lists, int operations) {
    return "{v:".repeat(compounds)
        + "[".repeat(lists)
        + bools(operations, "1")
        + "]".repeat(lists)
        + "}".repeat(compounds);
  }

  static NbtTag mixedTree(int compounds, int lists) {
    NbtTag tree = byteTag(1);
    for (int level = 0; level < lists; level++) {
      tree = list(tree);
    }
    for (int level = 0; level < compounds; level++) {
      tree = compound("v", tree);
    }
    return tree;
  }

  static NbtTag nestedLists(int levels, NbtTag innermost) {
    NbtTag tree = innermost;
    for (int level = 0; level < levels; level++) {
      tree = list(tree);
    }
    return tree;
  }
}
