package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The grindstone (EN-R44a, probe PE4). A pure function: no random draw. */
public final class GrindstoneCalculator {

  private static final int MERGE_BONUS_PERCENT = 5;
  private static final Identifier BOOK = Identifier.minecraft("book");
  private static final Identifier ENCHANTED_BOOK = Identifier.minecraft("enchanted_book");

  private GrindstoneCalculator() {}

  /**
   * Removes the enchantments that are not curses ({@code binding_curse} and {@code vanishing_curse}
   * stay), resets the work penalty to 0, keeps the name, and returns the experience that the
   * removed levels give. With two items, they are merged first as the grindstone does (open point
   * of the spec).
   *
   * @param first the top slot, possibly empty
   * @param second the bottom slot, possibly empty
   * @param data the loaded data
   * @return the output, empty when both slots are empty or the grindstone cannot take the items
   */
  public static Optional<GrindResult> grind(ItemStack first, ItemStack second, LootData data) {
    if (first.isEmpty() && second.isEmpty()) {
      return Optional.empty();
    }
    ItemStack base = first.isEmpty() ? second : first;
    if (!first.isEmpty() && !second.isEmpty() && !first.type().id().equals(second.type().id())) {
      return Optional.empty();
    }
    ItemStack merged = first.isEmpty() || second.isEmpty() ? base : mergeDurability(first, second);
    long removed = 0;
    ItemStack output = merged;
    for (ItemStack source : new ItemStack[] {first, second}) {
      removed += removedCost(source, data);
    }
    output = strip(output, WellKnownComponents.ENCHANTMENTS, List.of(first, second), data);
    output = strip(output, WellKnownComponents.STORED_ENCHANTMENTS, List.of(first, second), data);
    output = StackEnchants.withRepairCost(output, 0);
    output = plainBook(output, data);
    int experience = (int) Math.min(Integer.MAX_VALUE, (removed + 1) / 2);
    return Optional.of(new GrindResult(output, experience));
  }

  private static ItemStack mergeDurability(ItemStack first, ItemStack second) {
    if (!first.isDamageable() || first.maxDamage() <= 0) {
      return first;
    }
    int max = first.maxDamage();
    long remaining =
        (long) Math.max(0, max - first.damage())
            + Math.max(0, max - second.damage())
            + max * (long) MERGE_BONUS_PERCENT / 100;
    return StackEnchants.withDamage(first, (int) Math.max(0, max - Math.min(max, remaining)));
  }

  private static long removedCost(ItemStack stack, LootData data) {
    long total = 0;
    for (EnchantmentLevels levels :
        new EnchantmentLevels[] {ItemEnchantments.of(stack), ItemEnchantments.stored(stack)}) {
      for (Map.Entry<Identifier, Integer> entry : levels.levels().entrySet()) {
        EnchantmentDefinition definition = data.enchantments().find(entry.getKey()).orElse(null);
        if (definition != null && !data.enchantments().isCurse(entry.getKey(), data.registries())) {
          total += definition.minCost(entry.getValue());
        }
      }
    }
    return total;
  }

  /**
   * Replaces the enchantments of a component by the curses of the entries, once each (EN-R47,
   * H-meule-a: the output carries the curses of both entries).
   */
  private static ItemStack strip(
      ItemStack stack, Identifier component, List<ItemStack> sources, LootData data) {
    if (stack.component(component).isEmpty()) {
      return stack;
    }
    Map<Identifier, Integer> kept = new LinkedHashMap<>();
    for (ItemStack source : sources) {
      EnchantmentLevels levels =
          component.equals(WellKnownComponents.ENCHANTMENTS)
              ? ItemEnchantments.of(source)
              : ItemEnchantments.stored(source);
      levels
          .levels()
          .forEach(
              (id, level) -> {
                if (data.enchantments().isCurse(id, data.registries())) {
                  kept.putIfAbsent(id, level);
                }
              });
    }
    return stack.withComponent(
        component, new EnchantmentLevels(new LinkedHashMap<>(kept)).toValue());
  }

  private static ItemStack plainBook(ItemStack stack, LootData data) {
    if (!stack.type().id().equals(ENCHANTED_BOOK)
        || !ItemEnchantments.stored(stack).levels().isEmpty()) {
      return stack;
    }
    Optional<ItemType> book = data.items().find(BOOK);
    return book.isPresent() ? ItemStack.of(book.get(), stack.count()) : stack;
  }
}
