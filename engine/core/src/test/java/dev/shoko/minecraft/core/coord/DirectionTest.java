package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: core-math R35 and R36 are S: identifiers and yaw boundaries are provisional until P35, P36.
class DirectionTest {

  // spec: core-math R35 (S), vector V9: identifier, name, unit vector, axis
  @ParameterizedTest(name = "provisional {0}: id {1}, name {2}, vector ({3}, {4}, {5})")
  @CsvSource({
    "DOWN, 0, down, 0, -1, 0",
    "UP, 1, up, 0, 1, 0",
    "NORTH, 2, north, 0, 0, -1",
    "SOUTH, 3, south, 0, 0, 1",
    "WEST, 4, west, -1, 0, 0",
    "EAST, 5, east, 1, 0, 0"
  })
  void provisionalIdentifiersNamesAndVectorsMatchVectors(
      Direction direction, int id, String name, int stepX, int stepY, int stepZ) {
    assertThat(direction.id()).isEqualTo(id);
    assertThat(direction.serializedName()).isEqualTo(name);
    assertThat(new int[] {direction.stepX(), direction.stepY(), direction.stepZ()})
        .containsExactly(stepX, stepY, stepZ);
    assertThat(Direction.byId(id)).contains(direction);
  }

  // spec: core-math R35 (S): enum constants are declared in identifier order
  @Test
  void provisionalDeclarationOrderIsIdentifierOrder() {
    assertThat(Direction.values())
        .containsExactly(
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST);
  }

  // spec: core-math R35: identifiers outside 0..5 have no direction
  @ParameterizedTest(name = "byId({0}) is empty")
  @ValueSource(ints = {-1, 6, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void unknownIdentifierHasNoDirection(int id) {
    assertThat(Direction.byId(id)).isEmpty();
  }

  // spec: core-math R34, R35: axis of the unit vector, +X east, +Z south
  @ParameterizedTest(name = "{0} is along {1}")
  @CsvSource({"DOWN, Y", "UP, Y", "NORTH, Z", "SOUTH, Z", "WEST, X", "EAST, X"})
  void axisFollowsTheUnitVector(Direction direction, Axis axis) {
    assertThat(direction.axis()).isEqualTo(axis);
  }

  // spec: core-math R35 (C from the order): opposite has identifier id ^ 1 and negated vector
  @ParameterizedTest(name = "opposite of {0}")
  @EnumSource(Direction.class)
  void oppositeFlipsTheLowBitAndNegatesTheVector(Direction direction) {
    Direction opposite = direction.opposite();
    assertThat(opposite.id()).isEqualTo(direction.id() ^ 1);
    assertThat(opposite.stepX()).isEqualTo(-direction.stepX());
    assertThat(opposite.stepY()).isEqualTo(-direction.stepY());
    assertThat(opposite.stepZ()).isEqualTo(-direction.stepZ());
    assertThat(opposite.opposite()).isEqualTo(direction);
  }

  // spec: core-math R36 (S), vector V9: 2D identifier and yaw of horizontal directions
  @ParameterizedTest(name = "provisional {0}: 2D id {1}, yaw {2}")
  @CsvSource({"SOUTH, 0, 0.0", "WEST, 1, 90.0", "NORTH, 2, 180.0", "EAST, 3, -90.0"})
  void provisionalHorizontalIdentifiersAndYawsMatchVectors(
      Direction direction, int horizontalId, double yaw) {
    assertThat(direction.horizontalId()).isEqualTo(horizontalId);
    assertThat(direction.yaw()).isEqualTo(yaw);
    assertThat(Direction.byHorizontalId(horizontalId)).contains(direction);
  }

  // spec: core-math R36: 2D identifiers outside 0..3 have no direction
  @ParameterizedTest(name = "byHorizontalId({0}) is empty")
  @ValueSource(ints = {-1, 4, Integer.MIN_VALUE})
  void unknownHorizontalIdentifierHasNoDirection(int horizontalId) {
    assertThat(Direction.byHorizontalId(horizontalId)).isEmpty();
  }

  // spec: core-math R36; the exception type is the skeleton's documented contract
  @ParameterizedTest(name = "{0} has no 2D identifier nor yaw")
  @EnumSource(
      value = Direction.class,
      names = {"UP", "DOWN"})
  void verticalDirectionsHaveNoHorizontalIdentifierNorYaw(Direction direction) {
    assertThatThrownBy(direction::horizontalId).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(direction::yaw).isInstanceOf(IllegalStateException.class);
  }

  // spec: core-math R36 (S), vector V9 and derived values of floorToInt(yaw / 90 + 0.5) & 3
  @ParameterizedTest(name = "provisional fromYaw({0}) = {1}")
  @CsvSource({
    "0.0, SOUTH",
    "44.999, SOUTH",
    "45.0, WEST",
    "90.0, WEST",
    "134.999, WEST",
    "135.0, NORTH",
    "180.0, NORTH",
    "-180.0, NORTH",
    "-135.0, EAST",
    "-135.001, NORTH",
    "-90.0, EAST",
    "-45.001, EAST",
    "-45.0, SOUTH",
    "270.0, EAST",
    "360.0, SOUTH",
    "-360.0, SOUTH",
    "720.0, SOUTH"
  })
  void provisionalFromYawRoundsToNearestQuarterTurn(double yaw, Direction expected) {
    assertThat(Direction.fromYaw(yaw)).isEqualTo(expected);
  }

  // spec: core-math R36 (S): each horizontal direction is found again from its own yaw
  @ParameterizedTest(name = "provisional fromYaw(yaw of {0})")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "WEST", "EAST"})
  void provisionalFromYawInvertsYaw(Direction direction) {
    assertThat(Direction.fromYaw(direction.yaw())).isEqualTo(direction);
  }
}
