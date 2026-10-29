package dev.shoko.minecraft.world.chunk;

import java.util.Objects;
import java.util.Optional;

/**
 * The stored light of one section: {@code BlockLight} and {@code SkyLight}, each optional (CF-R14,
 * CF-R15, CF-R30). Absent block light means no block light in the section. Absent sky light is a
 * state of its own, distinct from an array of zeros: it is kept as it is on reading and on writing,
 * and {@link Chunk#effectiveSkyLight(int)} derives what it means. Mutable, confined to the thread
 * that ticks the world.
 */
public final class SectionLight {

  private NibbleArray blockLight;
  private NibbleArray skyLight;

  private SectionLight() {}

  /**
   * Creates light with neither array.
   *
   * @return the light
   */
  public static SectionLight none() {
    return new SectionLight();
  }

  /**
   * Returns the block light.
   *
   * @return the array, or empty when absent
   */
  public Optional<NibbleArray> blockLight() {
    return Optional.ofNullable(blockLight);
  }

  /**
   * Returns the sky light.
   *
   * @return the array, or empty when absent
   */
  public Optional<NibbleArray> skyLight() {
    return Optional.ofNullable(skyLight);
  }

  /**
   * Sets the block light.
   *
   * @param light the array, held without copy
   */
  public void setBlockLight(NibbleArray light) {
    this.blockLight = Objects.requireNonNull(light, "light");
  }

  /** Removes the block light. */
  public void clearBlockLight() {
    this.blockLight = null;
  }

  /**
   * Sets the sky light.
   *
   * @param light the array, held without copy
   */
  public void setSkyLight(NibbleArray light) {
    this.skyLight = Objects.requireNonNull(light, "light");
  }

  /** Removes the sky light. */
  public void clearSkyLight() {
    this.skyLight = null;
  }
}
