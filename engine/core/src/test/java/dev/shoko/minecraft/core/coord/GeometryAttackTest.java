package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class GeometryAttackTest {

  private static final double[] COORDS = {
    -1, -0.5, -0.0, 0.0, 0.5, 1, 2, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
  };

  /** Independent overlap test: open intervals (amin, amax) and (bmin, bmax) share a point. */
  private static boolean overlap(double amin, double amax, double bmin, double bmax) {
    return Math.max(amin, bmin) < Math.min(amax, bmax) && amin < bmax && bmin < amax;
  }

  @Test
  @Timeout(20)
  void intersectsIsSymmetricAndMatchesStrictOverlap() {
    SplittableRandom rnd = new SplittableRandom(41);
    for (int i = 0; i < 200_000; i++) {
      double[] v = new double[12];
      for (int j = 0; j < 12; j++) {
        v[j] = rnd.nextInt(10) == 0 ? COORDS[rnd.nextInt(COORDS.length)] : rnd.nextInt(-4, 5) * 0.5;
      }
      Aabb a = new Aabb(v[0], v[1], v[2], v[3], v[4], v[5]);
      Aabb b = new Aabb(v[6], v[7], v[8], v[9], v[10], v[11]);
      boolean ab = a.intersects(b);
      assertThat(b.intersects(a)).as("%s %s", a, b).isEqualTo(ab);
      boolean nonEmpty =
          a.minX() < a.maxX()
              && a.minY() < a.maxY()
              && a.minZ() < a.maxZ()
              && b.minX() < b.maxX()
              && b.minY() < b.maxY()
              && b.minZ() < b.maxZ();
      if (nonEmpty) {
        boolean expected =
            overlap(a.minX(), a.maxX(), b.minX(), b.maxX())
                && overlap(a.minY(), a.maxY(), b.minY(), b.maxY())
                && overlap(a.minZ(), a.maxZ(), b.minZ(), b.maxZ());
        assertThat(ab).as("%s %s", a, b).isEqualTo(expected);
        assertThat(a.intersects(a)).isTrue();
      }
    }
  }

  @Test
  void containsIsHalfOpenAndRejectsNaN() {
    Aabb unit = new Aabb(0, 0, 0, 1, 1, 1);
    assertThat(unit.contains(new Vec3(-0.0, 0.5, 0.5))).isTrue();
    assertThat(unit.contains(new Vec3(Math.nextDown(1.0), 0, 0))).isTrue();
    assertThat(unit.contains(new Vec3(1, 0, 0))).isFalse();
    assertThat(unit.contains(new Vec3(Double.NaN, 0.5, 0.5))).isFalse();
    assertThat(new Aabb(0, 0, 0, Double.NaN, 1, 1).contains(new Vec3(0.5, 0.5, 0.5))).isFalse();
    assertThat(new Aabb(1, 0, 0, 0, 1, 1).contains(new Vec3(0.5, 0.5, 0.5))).isFalse();
  }

  @Test
  void movingAndExtendingWithSpecialValuesFollowsTheLiteralFormulas() {
    Aabb unit = new Aabb(0, 0, 0, 1, 1, 1);
    Aabb moved = unit.move(new Vec3(Double.POSITIVE_INFINITY, Double.NaN, -0.0));
    assertThat(moved)
        .isEqualTo(
            new Aabb(
                Double.POSITIVE_INFINITY,
                Double.NaN,
                0.0,
                Double.POSITIVE_INFINITY,
                Double.NaN,
                1.0));
    assertThat(unit.expandTowards(new Vec3(Double.NaN, -0.0, 0.0))).isEqualTo(unit);
    assertThat(unit.expandTowards(new Vec3(Double.NEGATIVE_INFINITY, 0, 0)).minX())
        .isEqualTo(Double.NEGATIVE_INFINITY);
    assertThat(unit.inflate(-0.75)).isEqualTo(new Aabb(0.75, 0.75, 0.75, 0.25, 0.25, 0.25));
    // Literal R41 on a box left inverted by R42: [0.75, 0.25] "intersects" [0, 1] on every axis,
    // although it is empty. Conforms to the spec formulas; noted, not reported as a defect.
    assertThat(unit.inflate(-0.75).intersects(unit)).isTrue();
    assertThat(unit.inflate(-0.75).intersects(unit.inflate(-0.75))).isFalse();
    assertThat(Aabb.ofBlock(new BlockPos(Integer.MAX_VALUE, Integer.MIN_VALUE, -1)))
        .isEqualTo(new Aabb(2147483647.0, -2147483648.0, -1.0, 2147483648.0, -2147483647.0, 0.0));
    assertThat(Aabb.ofCorners(1, Double.NaN, 0.0, 0, 0, -0.0).minX()).isEqualTo(0.0);
  }

  // spec: core-math R39: length is sqrt((x*x + y*y) + z*z), literally; the squared length
  // overflows for components above about 1.34e154, which the formula keeps.
  @Test
  void lengthKeepsTheLiteralFormulaEvenWhenTheSquareOverflows() {
    assertThat(new Vec3(1e200, 0, 0).length()).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(new Vec3(1e-200, 0, 0).length()).isEqualTo(0.0);
    assertThat(new Vec3(3, 4, 12).length()).isEqualTo(13.0);
    assertThat(new Vec3(Double.NaN, 0, 0).length()).isNaN();
    assertThat(new Vec3(Double.POSITIVE_INFINITY, Double.NaN, 0).lengthSquared()).isNaN();
  }
}
