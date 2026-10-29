package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The enchantment tags that select a channel (EN-R9a, EN-R9b): the table, loot, trades and mob
 * equipment each read their own tag, never a global list. Identifiers read in {@code
 * data/minecraft/tags/enchantment}.
 */
public final class EnchantmentTags {

  /** Enchantments offered by the enchanting table (EN-R38). */
  public static final Identifier IN_ENCHANTING_TABLE = Identifier.minecraft("in_enchanting_table");

  /** Enchantments that are not treasure: 36 in the data. */
  public static final Identifier NON_TREASURE = Identifier.minecraft("non_treasure");

  /** Treasure enchantments: 7 in the data. */
  public static final Identifier TREASURE = Identifier.minecraft("treasure");

  /** The curses: {@code binding_curse} and {@code vanishing_curse}. */
  public static final Identifier CURSE = Identifier.minecraft("curse");

  /** Enchantments that random loot may give ({@code enchant_randomly}, LT-R45). */
  public static final Identifier ON_RANDOM_LOOT = Identifier.minecraft("on_random_loot");

  /** Enchantments of the mob spawn equipment provider (EN-R34). */
  public static final Identifier ON_MOB_SPAWN_EQUIPMENT =
      Identifier.minecraft("on_mob_spawn_equipment");

  /** Enchantments whose loot is smelted: {@code fire_aspect} (EN-R9a). */
  public static final Identifier SMELTS_LOOT = Identifier.minecraft("smelts_loot");

  private EnchantmentTags() {}
}
