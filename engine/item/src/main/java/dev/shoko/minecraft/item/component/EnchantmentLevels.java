package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The value of {@code enchantments} or {@code stored_enchantments}: enchantment identifier to level
 * (IC-R22). Only the data form lives here; definitions and effects belong to {@code loot}.
 *
 * @param levels the levels in insertion order, each from 1 to 255, unmodifiable
 */
public record EnchantmentLevels(SequencedMap<Identifier, Integer> levels) {

  /** Largest level accepted (IC-R22, provisional). */
  private static final int MAX_LEVEL = 255;

  /**
   * Copies the levels.
   *
   * @param levels the levels in insertion order
   */
  public EnchantmentLevels {
    levels = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(levels));
  }

  /**
   * Reads the value of one of the two components.
   *
   * @param value the component value, a compound of identifiers to integers
   * @return the levels; a level below 1 is dropped without a finding (IC-R22, provisional), a level
   *     above 255 is a {@code VALUE_RANGE} failure
   */
  public static Decoding<EnchantmentLevels> from(ComponentValue value) {
    if (!(value.tag() instanceof CompoundTag compound)) {
      return failure(FindingCode.TYPE_MISMATCH, "$", "expected an object");
    }
    SequencedMap<Identifier, Integer> levels = new LinkedHashMap<>();
    for (Map.Entry<String, NbtTag> entry : compound.entries().entrySet()) {
      String path = "$." + entry.getKey();
      Optional<Identifier> id = Identifier.tryParse(entry.getKey());
      if (id.isEmpty()) {
        return failure(FindingCode.INVALID_IDENTIFIER, path, "not an identifier");
      }
      if (!(entry.getValue() instanceof NumericTag number) || isDecimal(number)) {
        return failure(FindingCode.TYPE_MISMATCH, path, "expected an integer level");
      }
      long level = number.longValue();
      if (level > MAX_LEVEL) {
        return failure(FindingCode.VALUE_RANGE, path, "level above " + MAX_LEVEL);
      }
      if (level >= 1) {
        levels.put(id.get(), (int) level);
      }
    }
    return new Decoding.Success<>(new EnchantmentLevels(levels), List.of());
  }

  private static boolean isDecimal(NumericTag number) {
    return number instanceof FloatTag || number instanceof DoubleTag;
  }

  private static Decoding<EnchantmentLevels> failure(FindingCode code, String path, String text) {
    return new Decoding.Failure<>(List.of(new Finding(code, path, text)));
  }

  /**
   * Returns the component value.
   *
   * @return the compound of identifiers to integers, in the order of {@link #levels()}
   */
  public ComponentValue toValue() {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    levels.forEach((id, level) -> entries.put(id.toString(), new IntTag(level)));
    return new ComponentValue(CompoundTag.of(entries));
  }

  /**
   * Returns the levels with one enchantment set.
   *
   * @param enchantment the enchantment
   * @param level the level; below 1 removes the entry (IC-R22, provisional)
   * @return the new levels
   */
  public EnchantmentLevels with(Identifier enchantment, int level) {
    SequencedMap<Identifier, Integer> copy = new LinkedHashMap<>(levels);
    if (level < 1) {
      copy.remove(enchantment);
    } else {
      copy.put(enchantment, level);
    }
    return new EnchantmentLevels(copy);
  }

  /**
   * Returns the level of an enchantment.
   *
   * @param enchantment the enchantment
   * @return the level, 0 when absent
   */
  public int levelOf(Identifier enchantment) {
    return levels.getOrDefault(enchantment, 0);
  }
}
