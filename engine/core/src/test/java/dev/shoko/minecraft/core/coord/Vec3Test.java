package dev.shoko.minecraft.core.coord;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Vec3Test {

  // spec: core-math R39 (C): component-wise addition in double
  @Test
  void addIsComponentWise() {
    Vec3 sum = new Vec3(0.1, -16.0, 1e20).add(new Vec3(0.2, 0.5, 1.0));
    assertThat(sum).isEqualTo(new Vec3(0.30000000000000004, -15.5, 1e20));
  }

  // spec: core-math R39 (C): component-wise subtraction in double
  @Test
  void subtractIsComponentWise() {
    Vec3 difference = new Vec3(0.3, 0.0, -1.0).subtract(new Vec3(0.1, 0.5, -1.0));
    assertThat(difference).isEqualTo(new Vec3(0.19999999999999998, -0.5, 0.0));
  }

  // spec: core-math R39 (C): product by a scalar, component-wise
  @Test
  void scaleMultipliesEachComponent() {
    assertThat(new Vec3(0.1, -2.0, 3.0).scale(3.0))
        .isEqualTo(new Vec3(0.30000000000000004, -6.0, 9.0));
    assertThat(new Vec3(1.0, -2.0, 0.5).scale(-0.5)).isEqualTo(new Vec3(-0.5, 1.0, -0.25));
  }

  // spec: core-math R39, V19: (0.1, 0.2, 0.3) has squared length 0.14 (0x3fc1eb851eb851ec)
  @Test
  void lengthSquaredMatchesVector() {
    assertThat(Double.doubleToRawLongBits(new Vec3(0.1, 0.2, 0.3).lengthSquared()))
        .isEqualTo(0x3fc1eb851eb851ecL);
  }

  // spec: core-math R39 (S order): provisional (x*x + y*y) + z*z. For this triple, the first one of
  // java.util.Random(3) where the groupings differ, x*x + (y*y + z*z) gives 5439.898416456556.
  @Test
  void provisionalLengthSquaredGroupsXAndYFirst() {
    Vec3 v = new Vec3(73.1057369148862, 7.099203475193139, 6.712000939049956);
    assertSameDouble(v.lengthSquared(), 5439.898416456555);
  }

  // spec: core-math R39: length is the square root of the squared length (sqrt is exact, R2)
  @Test
  void lengthIsSquareRootOfLengthSquared() {
    assertSameDouble(new Vec3(-3.0, -4.0, 0.0).length(), 5.0);
    assertSameDouble(new Vec3(0.0, 0.0, 0.0).length(), 0.0);
    Vec3 v = new Vec3(73.1057369148862, 7.099203475193139, 6.712000939049956);
    assertSameDouble(v.length(), StrictMath.sqrt(5439.898416456555));
    assertSameDouble(new Vec3(0.1, 0.2, 0.3).length(), StrictMath.sqrt(0.14));
  }

  // spec: core-math R39: operations return new vectors and leave operands unchanged
  @Test
  void operationsDoNotMutateOperands() {
    Vec3 a = new Vec3(1.0, 2.0, 3.0);
    Vec3 b = new Vec3(4.0, 5.0, 6.0);
    a.add(b);
    a.subtract(b);
    a.scale(2.0);
    assertThat(a).isEqualTo(new Vec3(1.0, 2.0, 3.0));
    assertThat(b).isEqualTo(new Vec3(4.0, 5.0, 6.0));
  }

  // spec: core-math R39: equality is exact equality of components
  @Test
  void equalityIsExactOnComponents() {
    Vec3 a = new Vec3(0.30000000000000004, -1.0, 2.0);
    Vec3 b = new Vec3(0.1 + 0.2, -1.0, 2.0);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new Vec3(0.3, -1.0, 2.0));
    assertThat(a.x()).isEqualTo(0.30000000000000004);
    assertThat(a.y()).isEqualTo(-1.0);
    assertThat(a.z()).isEqualTo(2.0);
  }
}
