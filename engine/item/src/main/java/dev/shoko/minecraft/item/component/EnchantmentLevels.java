package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import java.util.SequencedMap;

/**
 * The value of {@code enchantments} or {@code stored_enchantments}: enchantment identifier to level
 * (IC-R22). Only the data form lives here; definitions and effects belong to {@code loot}.
 *
 * @param levels the levels in insertion order, each from 1 to 255, unmodifiable
 */
public record EnchantmentLevels(SequencedMap<Identifier, Integer> levels) {

  /**
   * Reads the value of one of the two components.
   *
   * @param value the component value, a compound of identifiers to integers
   * @return the levels; a level below 1 is dropped without a finding (IC-R22, provisional), a level
   *     above 255 is a {@code VALUE_RANGE} failure
   */
  public static Decoding<EnchantmentLevels> from(ComponentValue value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the component value.
   *
   * @return the compound of identifiers to integers, in the order of {@link #levels()}
   */
  public ComponentValue toValue() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the levels with one enchantment set.
   *
   * @param enchantment the enchantment
   * @param level the level; below 1 removes the entry (IC-R22, provisional)
   * @return the new levels
   */
  public EnchantmentLevels with(Identifier enchantment, int level) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the level of an enchantment.
   *
   * @param enchantment the enchantment
   * @return the level, 0 when absent
   */
  public int levelOf(Identifier enchantment) {
    return Skeleton.throwNotImplemented();
  }
}
