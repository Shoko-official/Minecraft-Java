package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import java.util.Optional;

/** What the enchanting procedures read and write on a stack: levels, enchantability, penalty. */
final class StackEnchants {

  static final Identifier ENCHANTABLE = Identifier.minecraft("enchantable");
  static final Identifier REPAIR_COST = Identifier.minecraft("repair_cost");
  static final Identifier CUSTOM_NAME = Identifier.minecraft("custom_name");
  static final Identifier REPAIRABLE = Identifier.minecraft("repairable");
  private static final Identifier BOOK = Identifier.minecraft("book");
  private static final Identifier ENCHANTED_BOOK = Identifier.minecraft("enchanted_book");
  private static final int BOOK_ENCHANTABILITY = 1;

  private StackEnchants() {}

  /** Tells whether a stack is a book, enchanted or not: it accepts every enchantment. */
  static boolean isBook(ItemStack stack) {
    return stack.type().id().equals(BOOK) || stack.type().id().equals(ENCHANTED_BOOK);
  }

  /** Reads the enchantability: the {@code enchantable} component, 1 for a book, else 0. */
  static int enchantability(ItemStack stack) {
    Optional<NbtTag> tag = stack.component(ENCHANTABLE).map(ComponentValue::tag);
    if (tag.isPresent() && tag.get() instanceof CompoundTag compound) {
      return compound
          .get("value")
          .filter(NumericTag.class::isInstance)
          .map(value -> ((NumericTag) value).intValue())
          .orElse(0);
    }
    return isBook(stack) ? BOOK_ENCHANTABILITY : 0;
  }

  /** Tells whether the stack can be enchanted by a table: enchantable or a book. */
  static boolean isEnchantable(ItemStack stack) {
    return !stack.isEmpty() && (stack.component(ENCHANTABLE).isPresent() || isBook(stack));
  }

  /** The component that holds the enchantments of this kind of stack. */
  static Identifier levelsComponent(ItemStack stack) {
    return stack.type().id().equals(ENCHANTED_BOOK)
            || stack.components().contains(WellKnownComponents.STORED_ENCHANTMENTS)
        ? WellKnownComponents.STORED_ENCHANTMENTS
        : WellKnownComponents.ENCHANTMENTS;
  }

  /** Reads the enchantments of a stack from the component of its kind. */
  static EnchantmentLevels levels(ItemStack stack) {
    return levelsComponent(stack).equals(WellKnownComponents.STORED_ENCHANTMENTS)
        ? ItemEnchantments.stored(stack)
        : ItemEnchantments.of(stack);
  }

  /** Writes the enchantments of a stack; a plain book becomes an enchanted book. */
  static ItemStack withLevels(ItemStack stack, EnchantmentLevels levels, ItemCatalog items) {
    ItemStack base = stack;
    if (stack.type().id().equals(BOOK)) {
      Optional<ItemType> enchanted = items.find(ENCHANTED_BOOK);
      if (enchanted.isPresent()) {
        base = ItemStack.of(enchanted.get(), stack.count(), stack.patch());
      }
    }
    return base.withComponent(levelsComponent(base), levels.toValue());
  }

  /** Reads the work penalty of a stack, 0 when absent. */
  static int repairCost(ItemStack stack) {
    return stack
        .component(REPAIR_COST)
        .map(ComponentValue::tag)
        .filter(NumericTag.class::isInstance)
        .map(tag -> ((NumericTag) tag).intValue())
        .orElse(0);
  }

  /** Writes the work penalty of a stack. */
  static ItemStack withRepairCost(ItemStack stack, long cost) {
    int bounded = (int) Math.max(0, Math.min(Integer.MAX_VALUE, cost));
    return stack.withComponent(REPAIR_COST, new ComponentValue(new IntTag(bounded)));
  }

  /** Writes the damage of a stack. */
  static ItemStack withDamage(ItemStack stack, int damage) {
    return stack.withComponent(
        WellKnownComponents.DAMAGE, new ComponentValue(new IntTag(Math.max(0, damage))));
  }
}
