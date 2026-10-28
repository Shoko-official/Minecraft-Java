package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

// spec: core-math R40 to R42 are S: provisional until probe P41.
class AabbTest {

  private static final Aabb UNIT = new Aabb(0, 0, 0, 1, 1, 1);

  // spec: core-math R40, R42: the canonical constructor stores its arguments without normalizing
  @Test
  void constructorKeepsArgumentsAsGiven() {
    Aabb inverted = new Aabb(1, 2, 3, -1, -2, -3);
    assertThat(inverted.minX()).isEqualTo(1.0);
    assertThat(inverted.minY()).isEqualTo(2.0);
    assertThat(inverted.minZ()).isEqualTo(3.0);
    assertThat(inverted.maxX()).isEqualTo(-1.0);
    assertThat(inverted.maxY()).isEqualTo(-2.0);
    assertThat(inverted.maxZ()).isEqualTo(-3.0);
  }

  // spec: core-math R40 (S): built from two arbitrary corners, the box takes min and max per axis
  @Test
  void provisionalOfCornersNormalizesEachAxis() {
    assertThat(Aabb.ofCorners(1, -2, 3, -1, 2, -3)).isEqualTo(new Aabb(-1, -2, -3, 1, 2, 3));
    assertThat(Aabb.ofCorners(-1, -2, -3, 1, 2, 3)).isEqualTo(new Aabb(-1, -2, -3, 1, 2, 3));
    assertThat(Aabb.ofCorners(0.5, 7, -16, 0.25, 7, -17))
        .isEqualTo(new Aabb(0.25, 7, -17, 0.5, 7, -16));
  }

  // spec: core-math R40: a full block (x, y, z) is [x, x + 1] x [y, y + 1] x [z, z + 1]
  @Test
  void blockBoxSpansOneUnitFromThePosition() {
    assertThat(Aabb.ofBlock(new BlockPos(0, 0, 0))).isEqualTo(UNIT);
    assertThat(Aabb.ofBlock(new BlockPos(-1, -64, -17)))
        .isEqualTo(new Aabb(-1, -64, -17, 0, -63, -16));
  }

  static Stream<Arguments> intersectionVectors() {
    return Stream.of(
        Arguments.of(
            "shared x face", new Aabb(0, 0, 0, 0.5, 1, 1), new Aabb(-1, 0, 0, 0, 1, 1), false),
        Arguments.of(
            "overlap", new Aabb(0, 0, 0, 0.5, 1, 1), new Aabb(-1, 0, 0, 0.0001, 1, 1), true),
        Arguments.of("shared edge", UNIT, new Aabb(1, 1, 0, 2, 2, 1), false),
        Arguments.of("inside", UNIT, new Aabb(0.25, 0.25, 0.25, 0.75, 0.75, 0.75), true),
        Arguments.of("identical", UNIT, new Aabb(0, 0, 0, 1, 1, 1), true),
        Arguments.of("flat inside", UNIT, new Aabb(0.5, 0, 0, 0.5, 1, 1), true),
        Arguments.of("flat on face", UNIT, new Aabb(1, 0, 0, 1, 1, 1), false),
        Arguments.of("shared y face", UNIT, new Aabb(0, 1, 0, 1, 2, 1), false),
        Arguments.of("shared z face", UNIT, new Aabb(0, 0, -1, 1, 1, 0), false),
        Arguments.of("shared corner", UNIT, new Aabb(1, 1, 1, 2, 2, 2), false),
        Arguments.of("apart", UNIT, new Aabb(2, 0, 0, 3, 1, 1), false),
        Arguments.of("overlap on x only", UNIT, new Aabb(0.5, 2, 0, 1.5, 3, 1), false));
  }

  // spec: core-math R41 (S), vector V18: strict inequalities on all three axes
  @ParameterizedTest(name = "provisional {0}: intersects = {3}")
  @MethodSource("intersectionVectors")
  void provisionalIntersectionIsStrict(String label, Aabb a, Aabb b, boolean expected) {
    assertThat(a.intersects(b)).as(label).isEqualTo(expected);
    assertThat(b.intersects(a)).as(label + ", swapped").isEqualTo(expected);
  }

  // spec: core-math R41 (S), V18 and edge case table: probe P41 geometry, x in [0, 0.5] against
  // the selector volume x in [-1, 0]
  @Test
  void provisionalTouchingBoxesFromProbeP41DoNotIntersect() {
    Aabb armorStand = new Aabb(0.0, 64, 0, 0.5, 65, 1);
    Aabb volume = new Aabb(-1, 64, 0, 0, 65, 1);
    assertThat(armorStand.intersects(volume)).isFalse();
  }

  // spec: core-math R41 (S), vector V18: point containment is half-open, min <= p < max
  @ParameterizedTest(name = "provisional unit box contains ({0}, {1}, {2}) = {3}")
  @CsvSource({
    "0, 0, 0, true",
    "1, 0.5, 0.5, false",
    "0.999999, 0.5, 0.5, true",
    "-0.0, 0.5, 0.5, true",
    "0.5, 1, 0.5, false",
    "0.5, 0.5, 1, false",
    "0.5, -0.000001, 0.5, false",
    "1, 1, 1, false",
    "NaN, 0.5, 0.5, false"
  })
  void provisionalContainsIsHalfOpen(double x, double y, double z, boolean expected) {
    assertThat(UNIT.contains(new Vec3(x, y, z))).isEqualTo(expected);
  }

  // spec: core-math R42, vector V18: move adds the vector to both corners
  @Test
  void moveTranslatesBothCorners() {
    assertThat(UNIT.move(new Vec3(-16, 0, 16))).isEqualTo(new Aabb(-16, 0, 16, -15, 1, 17));
  }

  // spec: core-math R42 (S), vector V18: symmetric inflation
  @Test
  void provisionalInflateGrowsEverySide() {
    assertThat(UNIT.inflate(0.5)).isEqualTo(new Aabb(-0.5, -0.5, -0.5, 1.5, 1.5, 1.5));
    assertThat(UNIT.inflate(-0.25)).isEqualTo(new Aabb(0.25, 0.25, 0.25, 0.75, 0.75, 0.75));
  }

  // spec: core-math R42 (S): a box contracted past zero keeps min > max, no renormalization
  @Test
  void provisionalOverContractedBoxIsNotRenormalized() {
    Aabb contracted = UNIT.inflate(-1);
    assertThat(contracted).isEqualTo(new Aabb(1, 1, 1, 0, 0, 0));
    assertThat(contracted.minX()).isGreaterThan(contracted.maxX());
  }

  // spec: core-math R42 (S), vector V18: negative component extends the min, positive the max
  @Test
  void provisionalExpandTowardsExtendsOnlyTheFacingSide() {
    assertThat(UNIT.expandTowards(new Vec3(-2, 0, 3))).isEqualTo(new Aabb(-2, 0, 0, 1, 1, 4));
    assertThat(UNIT.expandTowards(new Vec3(0.5, -0.5, 0)))
        .isEqualTo(new Aabb(0, -0.5, 0, 1.5, 1, 1));
  }

  // spec: core-math R40 to R42: operations return new boxes and leave the receiver unchanged
  @Test
  void operationsDoNotMutateTheReceiver() {
    Aabb box = new Aabb(0, 0, 0, 1, 1, 1);
    box.move(new Vec3(1, 1, 1));
    box.inflate(2);
    box.expandTowards(new Vec3(-1, 0, 1));
    assertThat(box).isEqualTo(UNIT);
  }

  // records: value semantics
  @Test
  void isAValueObject() {
    Aabb a = new Aabb(-1, -64, -17, 0, -63, -16);
    Aabb b = new Aabb(-1, -64, -17, 0, -63, -16);
    assertThat(a)
        .isEqualTo(b)
        .hasSameHashCodeAs(b)
        .isNotEqualTo(new Aabb(-1, -64, -17, 0, -63, -15));
  }
}
