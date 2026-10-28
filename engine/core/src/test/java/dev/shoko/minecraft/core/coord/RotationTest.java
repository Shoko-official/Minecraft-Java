package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

// spec: core-math R38 is S: names and effects are provisional until probe P38.
class RotationTest {

  // spec: core-math R38 (S): command names
  @ParameterizedTest(name = "provisional {0} is named {1}")
  @CsvSource({
    "NONE, none",
    "CLOCKWISE_90, clockwise_90",
    "CLOCKWISE_180, 180",
    "COUNTERCLOCKWISE_90, counterclockwise_90"
  })
  void provisionalNamesMatchPlaceCommand(Rotation rotation, String name) {
    assertThat(rotation.serializedName()).isEqualTo(name);
  }

  // spec: core-math R38 (S): effect on the six directions
  @ParameterizedTest(name = "provisional {0} sends {1} to {2}")
  @CsvSource({
    "NONE, NORTH, NORTH",
    "NONE, EAST, EAST",
    "NONE, SOUTH, SOUTH",
    "NONE, WEST, WEST",
    "NONE, UP, UP",
    "NONE, DOWN, DOWN",
    "CLOCKWISE_90, NORTH, EAST",
    "CLOCKWISE_90, EAST, SOUTH",
    "CLOCKWISE_90, SOUTH, WEST",
    "CLOCKWISE_90, WEST, NORTH",
    "CLOCKWISE_90, UP, UP",
    "CLOCKWISE_90, DOWN, DOWN",
    "CLOCKWISE_180, NORTH, SOUTH",
    "CLOCKWISE_180, EAST, WEST",
    "CLOCKWISE_180, SOUTH, NORTH",
    "CLOCKWISE_180, WEST, EAST",
    "CLOCKWISE_180, UP, UP",
    "CLOCKWISE_180, DOWN, DOWN",
    "COUNTERCLOCKWISE_90, NORTH, WEST",
    "COUNTERCLOCKWISE_90, WEST, SOUTH",
    "COUNTERCLOCKWISE_90, SOUTH, EAST",
    "COUNTERCLOCKWISE_90, EAST, NORTH",
    "COUNTERCLOCKWISE_90, UP, UP",
    "COUNTERCLOCKWISE_90, DOWN, DOWN"
  })
  void provisionalRotationMapsDirections(Rotation rotation, Direction from, Direction to) {
    assertThat(rotation.rotate(from)).isEqualTo(to);
  }

  // spec: core-math R38 (S): counterclockwise_90 is the inverse of clockwise_90, 180 is two
  // quarter turns, four quarter turns are the identity
  @ParameterizedTest(name = "provisional rotations compose on {0}")
  @EnumSource(Direction.class)
  void provisionalRotationsCompose(Direction direction) {
    Direction quarter = Rotation.CLOCKWISE_90.rotate(direction);
    assertThat(Rotation.COUNTERCLOCKWISE_90.rotate(quarter)).isEqualTo(direction);
    assertThat(Rotation.CLOCKWISE_90.rotate(quarter))
        .isEqualTo(Rotation.CLOCKWISE_180.rotate(direction));
    Direction full = direction;
    for (int i = 0; i < 4; i++) {
      full = Rotation.CLOCKWISE_90.rotate(full);
    }
    assertThat(full).isEqualTo(direction);
  }

  // spec: core-math R38 (S): a clockwise quarter turn adds 4 modulo 16 to the rotation property
  @ParameterizedTest(name = "provisional {0} adds {1} sixteenths")
  @CsvSource({"NONE, 0", "CLOCKWISE_90, 4", "CLOCKWISE_180, 8", "COUNTERCLOCKWISE_90, 12"})
  void provisionalSixteenthsRotateByQuarterTurns(Rotation rotation, int steps) {
    for (int value = 0; value < 16; value++) {
      assertThat(rotation.rotateSixteenths(value)).isEqualTo((value + steps) % 16);
    }
  }

  // spec: core-math R38 (S): rotation 0..15 wraps, e.g. 15 turned clockwise is 3
  @Test
  void provisionalSixteenthsWrapAtSixteen() {
    assertThat(Rotation.CLOCKWISE_90.rotateSixteenths(15)).isEqualTo(3);
    assertThat(Rotation.COUNTERCLOCKWISE_90.rotateSixteenths(0)).isEqualTo(12);
    assertThat(Rotation.CLOCKWISE_180.rotateSixteenths(9)).isEqualTo(1);
  }

  // spec: core-math R38 (S) and the skeleton contract floorMod(value + 4 * k, 16): negative
  // values land in [0, 15], unlike the Java remainder
  @ParameterizedTest(name = "provisional {0} on negative values stays in [0, 15]")
  @CsvSource({"NONE, 0", "CLOCKWISE_90, 1", "CLOCKWISE_180, 2", "COUNTERCLOCKWISE_90, 3"})
  void provisionalSixteenthsOfNegativeValuesAreFloorModded(Rotation rotation, int quarters) {
    assertThat(Rotation.CLOCKWISE_90.rotateSixteenths(-10)).isEqualTo(10);
    for (int value = -64; value < 0; value++) {
      int result = rotation.rotateSixteenths(value);
      assertThat(result).as("%s(%d)", rotation, value).isBetween(0, 15);
      assertThat(result).isEqualTo((value + 4 * quarters) & 15);
    }
  }
}
