package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.enchant.craft.AnvilCalculator;
import dev.shoko.minecraft.loot.enchant.craft.AnvilInput;
import dev.shoko.minecraft.loot.enchant.craft.AnvilResult;
import dev.shoko.minecraft.loot.enchant.craft.EnchantOffer;
import dev.shoko.minecraft.loot.enchant.craft.EnchantingTable;
import dev.shoko.minecraft.loot.enchant.craft.GrindResult;
import dev.shoko.minecraft.loot.enchant.craft.GrindstoneCalculator;
import dev.shoko.minecraft.loot.enchant.craft.RepairResult;
import dev.shoko.minecraft.loot.enchant.craft.XpRepair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// Adversarial tests on enchantments, enchanting table, anvil, grindstone and experience repair
// with the real extracted definitions.
@Timeout(600)
@Tag("oracle")
class EnchantHuntTest {

  private static ItemStack stack(String path) {
    return ItemStack.of(RealData.items().find(Identifier.minecraft(path)).orElseThrow(), 1);
  }

  private static ItemStack enchanted(ItemStack stack, Map<String, Integer> levels) {
    EnchantmentLevels value = new EnchantmentLevels(new java.util.LinkedHashMap<>());
    for (var e : levels.entrySet()) {
      value = value.with(Identifier.parse(e.getKey()), e.getValue());
    }
    return stack.withComponent(WellKnownComponents.ENCHANTMENTS, value.toValue());
  }

  private static ItemStack damaged(ItemStack stack, int damage) {
    return stack.withComponent(WellKnownComponents.DAMAGE, new ComponentValue(new IntTag(damage)));
  }

  private static List<ItemStack> allEnchantable() {
    List<ItemStack> items = new ArrayList<>();
    for (Identifier id : RealData.items().registry().ids()) {
      ItemStack s = ItemStack.of(RealData.items().find(id).orElseThrow(), 1);
      if (s.component(Identifier.minecraft("enchantable")).isPresent()) {
        items.add(s);
      }
    }
    return items;
  }

  private static LootContext context(long seed) {
    return LootContext.builder(
            RealData.data(),
            LootContextTypes.find(Identifier.minecraft("chest")).orElseThrow(),
            Lcg48Random.ofSeed(seed))
        .build();
  }

  // spec: item-enchantments EN-R44b, EN-R19c: the final value of repair_with_xp (1 x 2.0) is the
  // number of durability points repaired per point of experience, so 2.
  @Test
  void provisionalMendingRepairsTwoDurabilityPointsPerExperiencePoint() {
    ItemStack sword =
        enchanted(damaged(stack("diamond_sword"), 100), Map.of("minecraft:mending", 1));
    RepairResult result = XpRepair.repair(sword, 10, context(1));
    assertThat(result.stack().damage()).isEqualTo(80);
    assertThat(result.experienceLeft()).isZero();
  }

  // spec: EN-R37 to EN-R40: every offer of the table respects the definitions.
  @Test
  void provisionalEnchantingTableOffersRespectTheRealDefinitions() {
    LootData data = RealData.data();
    Random random = new Random(1);
    for (ItemStack item : allEnchantable()) {
      for (int shelves : new int[] {-5, 0, 1, 7, 15, 16, 100, Integer.MAX_VALUE}) {
        for (int k = 0; k < 20; k++) {
          List<EnchantOffer> offers =
              EnchantingTable.offers(item, shelves, Lcg48Random.ofSeed(random.nextLong()), data);
          assertThat(offers).hasSize(3);
          for (int line = 0; line < 3; line++) {
            EnchantOffer offer = offers.get(line);
            assertThat(offer.lapisCost()).isEqualTo(line + 1);
            assertThat(offer.lineCost()).as("%s shelves %d", item, shelves).isPositive();
            List<Identifier> ids = new ArrayList<>(offer.enchantments().levels().keySet());
            for (Identifier id : ids) {
              EnchantmentDefinition def = data.enchantments().find(id).orElseThrow();
              assertThat(offer.enchantments().levelOf(id)).isBetween(1, def.maxLevel());
              if (!item.type().id().path().equals("book")) {
                assertThat(def.items().primary().contains(item.type().id(), data.registries()))
                    .as("%s primary for %s", id, item)
                    .isTrue();
              }
              for (Identifier other : ids) {
                assertThat(data.enchantments().areCompatible(id, other, data.registries()))
                    .as("%s vs %s", id, other)
                    .isTrue();
              }
            }
          }
        }
      }
    }
  }

  // spec: LT-R45 / EN-R35, EN-R36: enchant_with_levels and enchant_randomly never throw and keep
  // the levels within the definitions, whatever the cost.
  @Test
  void provisionalEnchantWithLevelsAndRandomlyKeepTheirInvariantsOnEveryRealItem() {
    LootData data = RealData.data();
    var all =
        new dev.shoko.minecraft.loot.predicate.match.IdOrTagSet(
            Identifier.minecraft("enchantment"), List.of(), List.of(), true);
    Random random = new Random(9);
    int[] costs = {Integer.MIN_VALUE, -1, 0, 1, 5, 30, 50, 1000, Integer.MAX_VALUE};
    List<ItemStack> items = new ArrayList<>(allEnchantable());
    items.add(stack("book"));
    items.add(stack("enchanted_book"));
    for (ItemStack item : items) {
      for (int cost : costs) {
        for (int k = 0; k < 10; k++) {
          LootContext ctx = context(random.nextLong());
          ItemStack out =
              dev.shoko.minecraft.loot.enchant.craft.EnchantmentSelection.enchantWithLevels(
                  item, cost, all, ctx);
          checkLevels(out, data);
          ItemStack rnd =
              dev.shoko.minecraft.loot.enchant.craft.EnchantmentSelection.enchantRandomly(
                  item, all, true, context(random.nextLong()));
          checkLevels(rnd, data);
        }
      }
    }
  }

  private static void checkLevels(ItemStack out, LootData data) {
    var levels = new java.util.LinkedHashMap<>(ItemEnchantments.of(out).levels());
    levels.putAll(ItemEnchantments.stored(out).levels());
    for (var e : levels.entrySet()) {
      EnchantmentDefinition def = data.enchantments().find(e.getKey()).orElseThrow();
      assertThat(e.getValue()).as("%s on %s", e.getKey(), out).isBetween(1, def.maxLevel());
    }
    List<Identifier> ids = new ArrayList<>(levels.keySet());
    for (Identifier a : ids) {
      for (Identifier b : ids) {
        assertThat(data.enchantments().areCompatible(a, b, data.registries()))
            .as("%s vs %s on %s", a, b, out)
            .isTrue();
      }
    }
  }

  // spec: EN-R41 to EN-R43: the anvil never produces incompatible enchantments from compatible
  // inputs, never above max_level, never a damage outside [0, max_damage].
  @Test
  void provisionalTheAnvilKeepsItsInvariantsOnRealItems() {
    LootData data = RealData.data();
    List<ItemStack> items = allEnchantable();
    List<Identifier> enchants = new ArrayList<>(data.enchantments().definitions().keySet());
    Random random = new Random(42);
    for (int i = 0; i < 20_000; i++) {
      ItemStack target = randomEnchanted(items, enchants, random, data);
      ItemStack sacrifice =
          random.nextInt(3) == 0
              ? bookWith(enchants, random, data)
              : (random.nextBoolean() ? target : randomEnchanted(items, enchants, random, data));
      String name = random.nextInt(4) == 0 ? "n".repeat(random.nextInt(120)) : "";
      AnvilResult result =
          AnvilCalculator.combine(new AnvilInput(target, sacrifice, name), false, data);
      if (!(result instanceof AnvilResult.Output out)) {
        continue;
      }
      assertThat(out.levelCost()).isBetween(1, 39);
      ItemStack output = out.result();
      var levels = ItemEnchantments.of(output).levels();
      List<Identifier> ids = new ArrayList<>(levels.keySet());
      for (Identifier id : ids) {
        assertThat(levels.get(id)).as("%s on %s", id, output).isBetween(1, 255);
        for (Identifier other : ids) {
          assertThat(data.enchantments().areCompatible(id, other, data.registries()))
              .as("%s vs %s from %s + %s", id, other, target, sacrifice)
              .isTrue();
        }
      }
      if (output.isDamageable() && output.maxDamage() > 0) {
        assertThat(output.damage()).as("damage of %s", output).isBetween(0, output.maxDamage());
      }
    }
  }

  // spec: EN-R42b: merging two damaged items caps the durability (damage never above max).
  @Test
  void provisionalMergingWithAnItemDamagedBeyondItsMaximumDoesNotCorruptTheDamage() {
    LootData data = RealData.data();
    ItemStack target = damaged(stack("diamond_sword"), 10);
    ItemStack broken = damaged(stack("diamond_sword"), 100_000);
    AnvilResult result = AnvilCalculator.combine(new AnvilInput(target, broken, ""), true, data);
    if (result instanceof AnvilResult.Output out) {
      assertThat(out.result().damage()).isBetween(0, out.result().maxDamage());
    }
  }

  // spec: EN-R44a: the grindstone keeps the curses of both items it merges.
  @Test
  void provisionalTheGrindstoneKeepsTheCursesOfTheSecondItemToo() {
    LootData data = RealData.data();
    ItemStack first = damaged(stack("diamond_sword"), 5);
    ItemStack second =
        enchanted(damaged(stack("diamond_sword"), 5), Map.of("minecraft:vanishing_curse", 1));
    Optional<GrindResult> result = GrindstoneCalculator.grind(first, second, data);
    assertThat(result).isPresent();
    assertThat(
            ItemEnchantments.levelOf(
                result.get().output(), Identifier.parse("minecraft:vanishing_curse")))
        .isEqualTo(1);
  }

  // spec: EN-R44a: the grindstone removes every non-curse enchantment and resets the penalty.
  @Test
  void provisionalTheGrindstoneNeverLeavesAnEnchantmentThatIsNotACurse() {
    LootData data = RealData.data();
    List<ItemStack> items = allEnchantable();
    List<Identifier> enchants = new ArrayList<>(data.enchantments().definitions().keySet());
    Random random = new Random(5);
    for (int i = 0; i < 5000; i++) {
      ItemStack a = randomEnchanted(items, enchants, random, data);
      ItemStack b = random.nextBoolean() ? a : ItemStack.empty();
      Optional<GrindResult> result = GrindstoneCalculator.grind(a, b, data);
      if (result.isEmpty()) {
        continue;
      }
      for (Identifier id : ItemEnchantments.of(result.get().output()).levels().keySet()) {
        assertThat(data.enchantments().isCurse(id, data.registries())).as(id.toString()).isTrue();
      }
      assertThat(result.get().experience()).isNotNegative();
    }
  }

  private static ItemStack bookWith(List<Identifier> enchants, Random random, LootData data) {
    ItemStack book = stack("enchanted_book");
    EnchantmentLevels levels = new EnchantmentLevels(new java.util.LinkedHashMap<>());
    for (int k = 0; k < 1 + random.nextInt(3); k++) {
      Identifier id = enchants.get(random.nextInt(enchants.size()));
      EnchantmentDefinition def = data.enchantments().find(id).orElseThrow();
      levels = levels.with(id, 1 + random.nextInt(def.maxLevel()));
    }
    return book.withComponent(WellKnownComponents.STORED_ENCHANTMENTS, levels.toValue());
  }

  private static ItemStack randomEnchanted(
      List<ItemStack> items, List<Identifier> enchants, Random random, LootData data) {
    ItemStack item = items.get(random.nextInt(items.size()));
    EnchantmentLevels levels = new EnchantmentLevels(new java.util.LinkedHashMap<>());
    for (int k = 0; k < random.nextInt(4); k++) {
      Identifier id = enchants.get(random.nextInt(enchants.size()));
      EnchantmentDefinition def = data.enchantments().find(id).orElseThrow();
      if (!def.items().supported().contains(item.type().id(), data.registries())) {
        continue;
      }
      boolean ok = true;
      for (Identifier present : levels.levels().keySet()) {
        ok &= data.enchantments().areCompatible(id, present, data.registries());
      }
      if (ok) {
        levels = levels.with(id, 1 + random.nextInt(def.maxLevel()));
      }
    }
    ItemStack s = item.withComponent(WellKnownComponents.ENCHANTMENTS, levels.toValue());
    if (s.isDamageable() && s.maxDamage() > 0 && random.nextBoolean()) {
      s = damaged(s, random.nextInt(s.maxDamage()));
    }
    return random.nextInt(3) == 0
        ? s.withComponent(
            Identifier.minecraft("repair_cost"),
            new ComponentValue(new IntTag(new int[] {0, 1, 3, 7, 15, 31, 63}[random.nextInt(7)])))
        : s;
  }
}
