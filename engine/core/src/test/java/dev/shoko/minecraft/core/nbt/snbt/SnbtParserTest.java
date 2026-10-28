package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.snbt.SnbtVectors.Accepted;
import dev.shoko.minecraft.core.nbt.snbt.SnbtVectors.Rejected;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Table T of the spec (probe PS1): 44 accepted and 11 refused inputs, all provisional. */
class SnbtParserTest {

  static List<Accepted> accepted() {
    return SnbtVectors.accepted();
  }

  static List<Rejected> rejected() {
    return SnbtVectors.rejected();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("accepted")
  void parsesTableEntry(Accepted vector) throws NbtException {
    // spec: core-nbt R41 to R50, core-nbt-vectors table T, provisional (probe PS1)
    assertThat(SnbtParser.parse(vector.input())).isEqualTo(vector.value().get());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejected")
  void rejectsTableEntry(Rejected vector) {
    // spec: core-nbt R43, R45, R46, R48, R50, R51, table T, provisional (probe PS1)
    if (vector.offset() == SnbtVectors.ANY_OFFSET) {
      assertFails(() -> SnbtParser.parse(vector.input()), vector.reason());
    } else {
      assertFails(() -> SnbtParser.parse(vector.input()), vector.reason(), vector.offset());
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("accepted")
  void parsesTableEntryInsideCompound(Accepted vector) throws NbtException {
    // spec: core-nbt R39 (data merge and summon take a compound), R42
    var parsed = SnbtParser.parseCompound("{v:" + vector.input() + "}");
    assertThat(parsed.get("v")).contains(vector.value().get());
  }
}
