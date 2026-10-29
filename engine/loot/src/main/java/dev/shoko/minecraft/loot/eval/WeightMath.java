package dev.shoko.minecraft.loot.eval;

/** The arithmetic of weights, separate so that its boundaries can be probed (LT-R25, PL2). */
public final class WeightMath {

  private WeightMath() {}

  /**
   * Computes the effective weight of an entry (LT-R25): {@code weight + floor(quality * luck)}
   * bounded below by 0. The numeric precision of the product (float or double) is open and only
   * observable at integer boundaries (PL1, PL2); provisional: float.
   *
   * @param weight the weight of the entry
   * @param quality the quality of the entry
   * @param luck the luck of the context
   * @return the effective weight; 0 means the entry is never chosen
   */
  public static int effectiveWeight(int weight, int quality, float luck) {
    return Skeleton.throwNotImplemented();
  }
}
