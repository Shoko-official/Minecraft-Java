package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The {@code effects} table of an enchantment: effect component to value (EN-R2, EN-R11). Empty for
 * {@code fortune}, the only vanilla enchantment without effects.
 *
 * @param components the values in file order, unmodifiable
 */
public record EffectComponents(SequencedMap<Identifier, EffectComponentValue> components) {

  /**
   * Reads one component.
   *
   * @param component a component identifier such as {@code minecraft:damage}
   * @return the value, empty when the enchantment has no such component
   */
  public Optional<EffectComponentValue> get(Identifier component) {
    return Skeleton.throwNotImplemented();
  }
}
