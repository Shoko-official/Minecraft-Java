package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * Identifiers of the components that a rule of this module reads by name. The names are the
 * registry entries of the reference data (IC-R7); every other component is handled generically.
 */
public final class WellKnownComponents {

  /** {@code minecraft:max_stack_size}: the stack limit of an item (IC-R5, IC-R16). */
  public static final Identifier MAX_STACK_SIZE = Identifier.minecraft("max_stack_size");

  /** {@code minecraft:max_damage}: the durability of an item (IC-R21). */
  public static final Identifier MAX_DAMAGE = Identifier.minecraft("max_damage");

  /** {@code minecraft:damage}: the wear of a stack, from 0 to {@code max_damage} (IC-R21). */
  public static final Identifier DAMAGE = Identifier.minecraft("damage");

  /** {@code minecraft:unbreakable}: prevents wear (IC-R21). */
  public static final Identifier UNBREAKABLE = Identifier.minecraft("unbreakable");

  /** {@code minecraft:enchantments}: enchantment levels (IC-R22). */
  public static final Identifier ENCHANTMENTS = Identifier.minecraft("enchantments");

  /** {@code minecraft:stored_enchantments}: enchantment levels of a book (IC-R22). */
  public static final Identifier STORED_ENCHANTMENTS = Identifier.minecraft("stored_enchantments");

  /** {@code minecraft:use_remainder}: the stack left in a crafting grid (RC-R22). */
  public static final Identifier USE_REMAINDER = Identifier.minecraft("use_remainder");

  /** {@code minecraft:equippable}: its {@code slot} gates the armor slots (IC-R38c). */
  public static final Identifier EQUIPPABLE = Identifier.minecraft("equippable");

  /** {@code minecraft:cooking_fuel}: references the fuel providers (RC-R32). */
  public static final Identifier COOKING_FUEL = Identifier.minecraft("cooking_fuel");

  /** {@code minecraft:compostable}: references the compost provider (RC-R34). */
  public static final Identifier COMPOSTABLE = Identifier.minecraft("compostable");

  /** {@code minecraft:brewing_fuel}: references the brewing fuel providers (RC-R35). */
  public static final Identifier BREWING_FUEL = Identifier.minecraft("brewing_fuel");

  /** {@code minecraft:potion_contents}: matched by the brewing recipes (RC-R10). */
  public static final Identifier POTION_CONTENTS = Identifier.minecraft("potion_contents");

  /** {@code minecraft:trim}: set by trim smithing recipes (RC-R37). */
  public static final Identifier TRIM = Identifier.minecraft("trim");

  /** {@code minecraft:container}: nested stacks as a list of slot and item (IC-R23). */
  public static final Identifier CONTAINER = Identifier.minecraft("container");

  private WellKnownComponents() {}
}
