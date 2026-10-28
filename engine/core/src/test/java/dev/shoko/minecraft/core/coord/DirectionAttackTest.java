package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: core-math R35, R36, R38 (S): adversarial checks of directions, rotations and mirrors.
class DirectionAttackTest {

  /**
   * The R36 formula evaluated in double, as written: floorToInt(yaw / 90 + 0.5) & 3, with
   * floorToInt saturating per R4 (F1). Valid reference only while |yaw / 90| < 2^31.
   */
  private static Direction formulaFromYaw(double yaw) {
    int quarters = (int) Math.floor(yaw / 90 + 0.5);
    return Direction.byHorizontalId(quarters & 3).orElseThrow();
  }

  // spec: core-math R36 (S). Known limit: the largest double below 45 makes yaw / 90 + 0.5 round
  // up to exactly 1.0 in double, so the formula sends it west, not south. The formula wins over
  // the "every yaw strictly below 45 stays south" reading of the prose.
  @Test
  void provisionalKnownLimitYawJustBelowFortyFiveRoundsToWest() {
    assertThat(Direction.fromYaw(Math.nextDown(45.0))).isEqualTo(Direction.WEST);
    assertThat(Direction.fromYaw(44.999)).isEqualTo(Direction.SOUTH);
  }

  // spec: core-math R36 (S): around every boundary 45 + 90k, |k| <= 100000, the result is the
  // double evaluation of the formula (not an exact-arithmetic one)
  @Test
  @Timeout(20)
  void provisionalQuarterTurnBoundariesFollowTheDoubleFormula() {
    List<Double> mismatches = new ArrayList<>();
    for (int k = -100_000; k <= 100_000 && mismatches.size() < 20; k++) {
      double boundary = 45.0 + 90.0 * k;
      for (double yaw : new double[] {Math.nextDown(boundary), boundary, Math.nextUp(boundary)}) {
        if (Direction.fromYaw(yaw) != formulaFromYaw(yaw)) {
          mismatches.add(yaw);
        }
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // spec: core-math R36 (S): inside the floorToInt domain (|yaw| below ~1.93e11), a yaw and the
  // same yaw minus whole turns face the same way
  @Test
  void provisionalLargeYawInsideDomainIsPeriodicOverFullTurns() {
    assertThat(Direction.fromYaw(1.8e11 + 10)).isEqualTo(Direction.fromYaw(10.0));
    assertThat(Direction.fromYaw(-1.8e11 + 80)).isEqualTo(Direction.fromYaw(80.0));
    assertThat(Direction.fromYaw(-1.8e11 + 80)).isEqualTo(Direction.WEST);
  }

  // spec: core-math R4, R36 (S). Known limit: beyond |yaw| = 90 * (2^31 - 1), about 1.93e11,
  // floorToInt saturates and fromYaw is no longer periodic. 1e12 + 90 is 10 degrees modulo 360
  // (south) but saturates to Integer.MAX_VALUE & 3 = 3 (east); -1e12 is 80 degrees (west) but
  // saturates to Integer.MIN_VALUE & 3 = 0 (south). Out of domain: callers never pass such yaws.
  @Test
  void provisionalKnownLimitHugeYawSaturatesAndLosesPeriodicity() {
    assertThat(Direction.fromYaw(1e12 + 90)).isEqualTo(Direction.EAST);
    assertThat(Direction.fromYaw(-1e12)).isEqualTo(Direction.SOUTH);
    assertThat(Direction.fromYaw(Double.POSITIVE_INFINITY)).isEqualTo(Direction.EAST);
    assertThat(Direction.fromYaw(Double.NEGATIVE_INFINITY)).isEqualTo(Direction.SOUTH);
  }

  @Test
  void rotateSixteenthsMatchesFloorModOnEveryExtreme() {
    int[] values = {
      Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -17, -16, -1, 0, 15, 16, Integer.MAX_VALUE
    };
    Rotation[] rotations = {
      Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90
    };
    for (int quarters = 0; quarters < 4; quarters++) {
      Rotation rotation = rotations[quarters];
      for (int v : values) {
        long exact = Math.floorMod((long) v + 4L * quarters, 16L);
        assertThat(rotation.rotateSixteenths(v)).as("%s %d", rotation, v).isEqualTo((int) exact);
      }
    }
  }

  @Test
  void rotationsAndMirrorsFormTheExpectedGroup() {
    for (Direction d : Direction.values()) {
      assertThat(Rotation.CLOCKWISE_90.rotate(Rotation.COUNTERCLOCKWISE_90.rotate(d))).isEqualTo(d);
      assertThat(Rotation.CLOCKWISE_180.rotate(Rotation.CLOCKWISE_180.rotate(d))).isEqualTo(d);
      Direction four = d;
      for (int i = 0; i < 4; i++) {
        four = Rotation.CLOCKWISE_90.rotate(four);
      }
      assertThat(four).isEqualTo(d);
      for (Mirror m : Mirror.values()) {
        assertThat(m.mirror(m.mirror(d))).isEqualTo(d);
      }
      assertThat(d.opposite().opposite()).isEqualTo(d);
      assertThat(d.opposite().stepX()).isEqualTo(-d.stepX());
      assertThat(d.opposite().stepY()).isEqualTo(-d.stepY());
      assertThat(d.opposite().stepZ()).isEqualTo(-d.stepZ());
    }
  }

  @Test
  void specialYawsFollowTheProvisionalFormula() {
    assertThat(Direction.fromYaw(-0.0)).isEqualTo(Direction.SOUTH);
    assertThat(Direction.fromYaw(Double.NaN)).isEqualTo(Direction.SOUTH);
    assertThat(Direction.fromYaw(Math.nextDown(-45.0))).isEqualTo(Direction.EAST);
    assertThat(Direction.fromYaw(Math.nextUp(-45.0))).isEqualTo(Direction.SOUTH);
  }

  @Test
  void returnedValuesArrayIsADefensiveCopy() {
    Arrays.fill(Direction.values(), Direction.EAST);
    assertThat(Direction.values()).startsWith(Direction.DOWN);
    assertThat(Direction.byId(0)).contains(Direction.DOWN);
  }
}
