package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The cost and result of the anvil (EN-R41 to EN-R43, probe PE2, vector V-E13). A pure function: no
 * random draw, the menu and the slots are in {@code game}.
 */
public final class AnvilCalculator {

  private static final int MAX_COST = 39;
  private static final int MAX_NAME_LENGTH = 50;
  private static final int REPAIR_DIVISOR = 4;
  private static final int MERGE_BONUS_PERCENT = 12;
  private static final int MERGE_COST = 2;
  private static final Identifier ITEM_REGISTRY = Identifier.minecraft("item");

  private AnvilCalculator() {}

  /**
   * Computes the anvil output. Repair with a material: a unit restores 25 percent of {@code
   * max_damage} rounded down for one level (EN-R42a). Merge of two items: the remaining
   * durabilities plus 12 percent of {@code max_damage}, capped (EN-R42b). Enchantments of the
   * sacrifice that the target supports (EN-R43c): added at their level unless one of the target's
   * excludes them, then the loss costs one level (EN-R43a); already present: the higher level wins,
   * equal levels give +1 up to {@code max_level}, a lower level does nothing (EN-R43b); the cost of
   * each is the resulting level times {@code anvil_cost}, halved with a floor of 1 for a book
   * (EN-R43d, provisional). The output {@code repair_cost} is {@code 2 * max(costs) + 1} (EN-R41b);
   * a rename alone keeps it. The name is limited to 50 characters (EN-R42c).
   *
   * @param input the two slots and the name field
   * @param creative whether the player is in creative mode, which lifts the limit of 39 levels
   * @param data the loaded data
   * @return the output with its cost, or the reason there is none
   */
  public static AnvilResult combine(AnvilInput input, boolean creative, LootData data) {
    ItemStack target = input.target();
    ItemStack sacrifice = input.sacrifice();
    if (target.isEmpty()) {
      return new AnvilResult.Refused(AnvilRefusal.NOTHING_TO_DO);
    }
    Work work = new Work(target);
    work.cost = (long) StackEnchants.repairCost(target) + StackEnchants.repairCost(sacrifice);
    if (!sacrifice.isEmpty()) {
      combineWithSacrifice(work, sacrifice, data);
    }
    boolean renamed = rename(work, input.requestedName());
    if (!work.changed && !renamed) {
      return new AnvilResult.Refused(AnvilRefusal.NOTHING_TO_DO);
    }
    if (work.changed) {
      long penalty =
          2L
                  * Math.max(
                      StackEnchants.repairCost(target),
                      sacrifice.isEmpty() ? 0 : StackEnchants.repairCost(sacrifice))
              + 1;
      work.result = StackEnchants.withRepairCost(work.result, penalty);
    }
    long cost = Math.max(1, work.cost);
    if (cost > MAX_COST && !creative) {
      return new AnvilResult.Refused(AnvilRefusal.TOO_EXPENSIVE);
    }
    return new AnvilResult.Output(work.result, (int) Math.min(Integer.MAX_VALUE, cost), work.used);
  }

  /** The state of one anvil operation. */
  private static final class Work {
    ItemStack result;
    long cost;
    int used;
    boolean changed;

    Work(ItemStack target) {
      this.result = target;
    }
  }

  private static void combineWithSacrifice(Work work, ItemStack sacrifice, LootData data) {
    ItemStack target = work.result;
    boolean book = StackEnchants.isBook(sacrifice);
    boolean sameType = target.type().id().equals(sacrifice.type().id());
    if (!book && target.damage() > 0 && isMaterial(target, sacrifice, data)) {
      repairWithMaterial(work, sacrifice);
      return;
    }
    if (!book && !sameType) {
      return;
    }
    if (sameType && target.isDamageable() && target.damage() > 0 && !book) {
      mergeDurability(work, sacrifice);
    }
    mergeEnchantments(work, sacrifice, book, data);
  }

  private static boolean isMaterial(ItemStack target, ItemStack sacrifice, LootData data) {
    Optional<NbtTag> repairable =
        target.component(StackEnchants.REPAIRABLE).map(ComponentValue::tag);
    if (repairable.isEmpty() || !(repairable.get() instanceof CompoundTag compound)) {
      return false;
    }
    NbtTag items = compound.get("items").orElse(null);
    List<String> entries = new ArrayList<>();
    if (items instanceof StringTag text) {
      entries.add(text.value());
    } else if (items instanceof ListTag list) {
      list.elements().forEach(e -> entries.add(e instanceof StringTag s ? s.value() : ""));
    }
    List<Identifier> ids = new ArrayList<>();
    List<Identifier> tags = new ArrayList<>();
    for (String entry : entries) {
      boolean isTag = entry.startsWith("#");
      Identifier id = Identifier.tryParse(isTag ? entry.substring(1) : entry).orElse(null);
      if (id != null) {
        (isTag ? tags : ids).add(id);
      }
    }
    IdOrTagSet set = new IdOrTagSet(ITEM_REGISTRY, ids, tags, false);
    return set.contains(sacrifice.type().id(), data.registries());
  }

  private static void repairWithMaterial(Work work, ItemStack sacrifice) {
    ItemStack target = work.result;
    int quarter = target.maxDamage() / REPAIR_DIVISOR;
    if (quarter <= 0) {
      return;
    }
    int units = Math.min(sacrifice.count(), (target.damage() + quarter - 1) / quarter);
    int repaired = Math.min(target.damage(), units * quarter);
    work.result = StackEnchants.withDamage(target, target.damage() - repaired);
    work.cost += units;
    work.used = units;
    work.changed = true;
  }

  private static void mergeDurability(Work work, ItemStack sacrifice) {
    ItemStack target = work.result;
    int max = target.maxDamage();
    // A damage above the maximum counts as a broken item: no remaining durability, never negative.
    long remaining =
        (long) Math.max(0, max - target.damage())
            + Math.max(0, max - sacrifice.damage())
            + max * (long) MERGE_BONUS_PERCENT / 100;
    int damage = (int) Math.max(0, max - Math.min(max, remaining));
    if (damage != target.damage()) {
      work.result = StackEnchants.withDamage(target, damage);
      work.cost += MERGE_COST;
      work.used = sacrifice.count();
      work.changed = true;
    }
  }

  private static void mergeEnchantments(
      Work work, ItemStack sacrifice, boolean book, LootData data) {
    EnchantmentLevels offered =
        book ? ItemEnchantments.stored(sacrifice) : ItemEnchantments.of(sacrifice);
    EnchantmentLevels levels = StackEnchants.levels(work.result);
    boolean any = false;
    for (Map.Entry<Identifier, Integer> offer : offered.levels().entrySet()) {
      EnchantmentDefinition definition = data.enchantments().find(offer.getKey()).orElse(null);
      if (definition == null || !supports(work.result, definition, data)) {
        continue;
      }
      int current = levels.levelOf(offer.getKey());
      int level = mergedLevel(current, offer.getValue(), definition);
      if (current > 0 && level == current) {
        continue;
      }
      if (current == 0 && incompatible(levels, definition, data)) {
        work.cost += 1;
        continue;
      }
      levels = levels.with(offer.getKey(), level);
      int multiplier =
          book ? Math.max(1, definition.costs().anvilCost() / 2) : definition.costs().anvilCost();
      work.cost += (long) level * multiplier;
      any = true;
    }
    if (any) {
      work.result = StackEnchants.withLevels(work.result, levels, data.items());
      work.used = sacrifice.count();
      work.changed = true;
    }
  }

  private static int mergedLevel(int current, int offered, EnchantmentDefinition definition) {
    if (current == 0 || offered > current) {
      return offered;
    }
    return offered == current ? Math.min(current + 1, definition.maxLevel()) : current;
  }

  private static boolean supports(
      ItemStack target, EnchantmentDefinition definition, LootData data) {
    return StackEnchants.isBook(target)
        || definition.items().supported().contains(target.type().id(), data.registries());
  }

  private static boolean incompatible(
      EnchantmentLevels levels, EnchantmentDefinition definition, LootData data) {
    for (Identifier present : levels.levels().keySet()) {
      if (!present.equals(definition.id())
          && !data.enchantments().areCompatible(definition.id(), present, data.registries())) {
        return true;
      }
    }
    return false;
  }

  private static boolean rename(Work work, String requested) {
    if (requested.isEmpty()) {
      return false;
    }
    String name =
        requested.codePointCount(0, requested.length()) > MAX_NAME_LENGTH
            ? requested.substring(0, requested.offsetByCodePoints(0, MAX_NAME_LENGTH))
            : requested;
    ComponentValue value = new ComponentValue(new StringTag(name));
    if (work.result.component(StackEnchants.CUSTOM_NAME).filter(value::equals).isPresent()) {
      return false;
    }
    work.result = work.result.withComponent(StackEnchants.CUSTOM_NAME, value);
    work.cost += 1;
    return true;
  }
}
