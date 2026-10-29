package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentMap;

/**
 * One entry of the {@code item} registry with its prototype components (IC-R1, IC-R2).
 *
 * @param id the identifier, such as {@code minecraft:stone_pickaxe}
 * @param rawId the position in the registry, from 0
 * @param prototype the default components of the item, in file order
 */
public record ItemType(Identifier id, int rawId, ComponentMap prototype) {

  private static final Identifier AIR = Identifier.minecraft("air");

  /**
   * Tells whether this is the default entry of the registry, {@code minecraft:air} (IC-R1, IC-R15).
   *
   * @return {@code true} for air
   */
  public boolean isAir() {
    return id.equals(AIR);
  }
}
