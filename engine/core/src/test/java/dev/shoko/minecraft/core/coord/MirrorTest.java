package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

// spec: core-math R38 is S: names and effects are provisional until probe P38.
class MirrorTest {

  // spec: core-math R38 (S): command names
  @ParameterizedTest(name = "provisional {0} is named {1}")
  @CsvSource({"NONE, none", "LEFT_RIGHT, left_right", "FRONT_BACK, front_back"})
  void provisionalNamesMatchPlaceCommand(Mirror mirror, String name) {
    assertThat(mirror.serializedName()).isEqualTo(name);
  }

  // spec: core-math R38 (S): left_right swaps north and south, front_back swaps east and west,
  // up and down are invariant
  @ParameterizedTest(name = "provisional {0} sends {1} to {2}")
  @CsvSource({
    "NONE, NORTH, NORTH",
    "NONE, SOUTH, SOUTH",
    "NONE, EAST, EAST",
    "NONE, WEST, WEST",
    "NONE, UP, UP",
    "NONE, DOWN, DOWN",
    "LEFT_RIGHT, NORTH, SOUTH",
    "LEFT_RIGHT, SOUTH, NORTH",
    "LEFT_RIGHT, EAST, EAST",
    "LEFT_RIGHT, WEST, WEST",
    "LEFT_RIGHT, UP, UP",
    "LEFT_RIGHT, DOWN, DOWN",
    "FRONT_BACK, NORTH, NORTH",
    "FRONT_BACK, SOUTH, SOUTH",
    "FRONT_BACK, EAST, WEST",
    "FRONT_BACK, WEST, EAST",
    "FRONT_BACK, UP, UP",
    "FRONT_BACK, DOWN, DOWN"
  })
  void provisionalMirrorMapsDirections(Mirror mirror, Direction from, Direction to) {
    assertThat(mirror.mirror(from)).isEqualTo(to);
  }

  // spec: core-math R38 (S): every mirror is an involution
  @ParameterizedTest(name = "provisional {0} applied twice is the identity")
  @EnumSource(Mirror.class)
  void provisionalMirrorIsAnInvolution(Mirror mirror) {
    for (Direction direction : Direction.values()) {
      assertThat(mirror.mirror(mirror.mirror(direction))).isEqualTo(direction);
    }
  }
}
