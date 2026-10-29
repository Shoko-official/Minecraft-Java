package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.LinkedHashMap;
import java.util.Optional;

/** Reads the enchantment levels a stack carries. */
public final class ItemEnchantments {

  private ItemEnchantments() {}

  /**
   * Reads the {@code enchantments} component.
   *
   * @param stack a stack
   * @return the levels, empty when the stack has none or the component is malformed
   */
  public static EnchantmentLevels of(ItemStack stack) {
    return read(stack, WellKnownComponents.ENCHANTMENTS);
  }

  /**
   * Reads the {@code stored_enchantments} component of an enchanted book.
   *
   * @param stack a stack
   * @return the levels, empty when the stack has none or the component is malformed
   */
  public static EnchantmentLevels stored(ItemStack stack) {
    return read(stack, WellKnownComponents.STORED_ENCHANTMENTS);
  }

  /**
   * Reads the level of one enchantment in the enchantments component of a stack.
   *
   * @param stack a stack
   * @param enchantment an enchantment identifier
   * @return the level, 0 when the stack does not carry the enchantment
   */
  public static int levelOf(ItemStack stack, Identifier enchantment) {
    return of(stack).levelOf(enchantment);
  }

  private static EnchantmentLevels read(ItemStack stack, Identifier component) {
    Optional<ComponentValue> value = stack.component(component);
    if (value.isPresent()
        && EnchantmentLevels.from(value.get()) instanceof Decoding.Success<EnchantmentLevels> ok) {
      return ok.value();
    }
    return new EnchantmentLevels(new LinkedHashMap<>());
  }
}
