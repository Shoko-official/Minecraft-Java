package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockPropertyTest {

  private static final BlockProperty FACING =
      new BlockProperty("facing", List.of("down", "north", "east"));

  @Test
  void rankIsThePositionInTheList() {
    // spec: BS-R6, BS-R9 (the rank of a value is its position, not its numeric value)
    assertThat(FACING.rankOf("down")).isZero();
    assertThat(FACING.rankOf("north")).isEqualTo(1);
    assertThat(FACING.rankOf("east")).isEqualTo(2);
  }

  @Test
  void booleanTrueHasRankZero() {
    // spec: BS-R9 (booleans are listed true, false)
    BlockProperty waterlogged = new BlockProperty("waterlogged", List.of("true", "false"));

    assertThat(waterlogged.rankOf("true")).isZero();
    assertThat(waterlogged.rankOf("false")).isEqualTo(1);
  }

  @Test
  void integerPropertyThatStartsAtOneRanksFromZero() {
    // spec: BS-R9 (level of cauldrons runs from 1 to 3)
    BlockProperty level = new BlockProperty("level", List.of("1", "2", "3"));

    assertThat(level.rankOf("1")).isZero();
    assertThat(level.rankOf("3")).isEqualTo(2);
  }

  @Test
  void valueOutsideTheListIsRejected() {
    // spec: BlockProperty.rankOf contract (IllegalArgumentException)
    BlockProperty level = new BlockProperty("level", List.of("1", "2", "3"));

    Failures.expect(IllegalArgumentException.class, () -> level.rankOf("0"));
    Failures.expect(IllegalArgumentException.class, () -> level.rankOf("4"));
    Failures.expect(IllegalArgumentException.class, () -> level.rankOf(""));
    Failures.expect(IllegalArgumentException.class, () -> level.rankOf("01"));
  }

  @Test
  void valueMatchingIsCaseSensitive() {
    // spec: BS-R1 (values are exact strings)
    BlockProperty waterlogged = new BlockProperty("waterlogged", List.of("true", "false"));

    Failures.expect(IllegalArgumentException.class, () -> waterlogged.rankOf("True"));
  }

  @Test
  void sizeIsTheNumberOfValues() {
    // spec: BS-R6 (n_j, the size of the list)
    assertThat(FACING.size()).isEqualTo(3);
    assertThat(new BlockProperty("level", List.of("0")).size()).isEqualTo(1);
  }
}
