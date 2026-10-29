package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: EN-R10 (levels 1 to 255), EN-R8 (no incompatible pair chosen together), EN-R37 to EN-R44
// (adversarial): random items, enchantments (unknown identifiers, levels 0 and 255), costs, shelves
// and names fed to the table, the selection, the anvil, the grindstone and the repair.
@Timeout(120)
class EnchantmentAttackTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static final long SEED = 20261029L;
  private static final List<String> ITEMS =
      List.of(
          "diamond_sword",
          "diamond_axe",
          "iron_pickaxe",
          "bow",
          "trident",
          "diamond_chestplate",
          "book",
          "enchanted_book",
          "stone",
          "shears",
          "potion");
  private static final List<String> ENCHANTMENTS =
      new ArrayList<>(
          List.of(
              "sharpness",
              "smite",
              "protection",
              "fire_protection",
              "unbreaking",
              "mending",
              "infinity",
              "fortune",
              "silk_touch",
              "binding_curse",
              "riptide",
              "loyalty",
              "no_such"));

  private static ItemStack randomStack(Random random) {
    ItemStack stack =
        LootItems.stack(ITEMS.get(random.nextInt(ITEMS.size())), 1 + random.nextInt(3));
    if (random.nextBoolean()) {
      Map<String, Integer> levels = new LinkedHashMap<>();
      int count = 1 + random.nextInt(3);
      for (int i = 0; i < count; i++) {
        levels.put(
            "minecraft:" + ENCHANTMENTS.get(random.nextInt(ENCHANTMENTS.size())),
            random.nextInt(256));
      }
      stack =
          random.nextBoolean()
              ? LootItems.enchanted(stack, levels)
              : LootItems.storing(stack, levels);
    }
    if (random.nextInt(3) == 0 && stack.isDamageable()) {
      stack = LootItems.damaged(stack, random.nextInt(stack.maxDamage()));
    }
    if (random.nextInt(3) == 0) {
      stack = LootItems.withRepairCost(stack, random.nextInt(100));
    }
    return stack;
  }

  private static LootContext context(long seed) {
    return Ctx.builder(Lcg48Random.ofSeed(seed)).build();
  }

  private static void assertSane(Map<Identifier, Integer> levels) {
    for (Map.Entry<Identifier, Integer> entry : levels.entrySet()) {
      assertThat(entry.getValue()).isBetween(1, 255);
    }
  }

  @Test
  void theTableNeverFailsWhateverTheItemAndTheShelves() {
    // spec: EN-R37 to EN-R40
    Random random = new Random(SEED);
    for (int i = 0; i < 300; i++) {
      ItemStack stack = randomStack(random);
      int shelves = random.nextInt(60);
      List<EnchantOffer> offers =
          EnchantingTable.offers(stack, shelves, Lcg48Random.ofSeed(random.nextLong()), data());
      assertThat(offers.size()).isIn(0, 3);
      for (EnchantOffer offer : offers) {
        assertThat(offer.lineCost()).isPositive();
        assertSane(offer.enchantments().levels());
      }
    }
  }

  @Test
  void anOfferNeverHoldsTwoIncompatibleEnchantments() {
    // spec: EN-R39, EN-R8
    Random random = new Random(SEED + 1);
    for (int i = 0; i < 300; i++) {
      ItemStack stack = LootItems.stack(random.nextBoolean() ? "book" : "diamond_sword");
      EnchantOffer offer =
          EnchantingTable.offer(
              stack,
              1 + random.nextInt(60),
              random.nextInt(3),
              Lcg48Random.ofSeed(random.nextLong()),
              data());
      List<Identifier> ids = new ArrayList<>(offer.enchantments().levels().keySet());
      for (int a = 0; a < ids.size(); a++) {
        for (int b = a + 1; b < ids.size(); b++) {
          assertThat(
                  data().enchantments().areCompatible(ids.get(a), ids.get(b), data().registries()))
              .as("%s and %s", ids.get(a), ids.get(b))
              .isTrue();
        }
      }
    }
  }

  @Test
  void theSelectionToleratesAnyCostAndAnySource() {
    // spec: EN-R38, EN-R39 (costs below 0 and far above any range)
    Random random = new Random(SEED + 2);
    ItemStack book = LootItems.stack("book");
    for (int i = 0; i < 300; i++) {
      int cost = random.nextInt(700) - 100;
      List<EnchantmentInstance> candidates =
          EnchantmentSelection.possible(
              book, cost, Sets.anyEnchantment(), random.nextBoolean(), data());
      for (EnchantmentInstance candidate : candidates) {
        EnchantmentDefinition definition = candidate.enchantment();
        assertThat(candidate.level()).isBetween(1, definition.maxLevel());
        assertThat(definition.minCost(candidate.level())).isLessThanOrEqualTo(cost);
        assertThat(definition.maxCost(candidate.level())).isGreaterThanOrEqualTo(cost);
      }
      List<EnchantmentInstance> chosen =
          EnchantmentSelection.choose(
              candidates, random.nextInt(700) - 100, Lcg48Random.ofSeed(random.nextLong()), data());
      assertThat(chosen.size()).isLessThanOrEqualTo(candidates.size());
    }
  }

  @Test
  void theModifiersToleratesEveryItemAndEveryLevelCount() {
    // spec: LT-R45, EN-R35, EN-R36
    Random random = new Random(SEED + 3);
    for (int i = 0; i < 300; i++) {
      ItemStack stack = randomStack(random);
      IdOrTagSet options =
          random.nextBoolean() ? Sets.anyEnchantment() : Sets.enchantmentTag("non_treasure");
      ItemStack levels =
          EnchantmentSelection.enchantWithLevels(
              stack, random.nextInt(120) - 10, options, context(random.nextLong()));
      ItemStack randomly =
          EnchantmentSelection.enchantRandomly(
              stack, options, random.nextBoolean(), context(random.nextLong()));
      for (ItemStack result : List.of(levels, randomly)) {
        assertThat(result.isEmpty()).isFalse();
        assertSane(LootItems.enchantmentsOf(result));
        assertSane(LootItems.storedOf(result));
      }
    }
  }

  @Test
  void theAnvilNeverFailsAndNeverGivesANegativeCost() {
    // spec: EN-R41 to EN-R43
    Random random = new Random(SEED + 4);
    for (int i = 0; i < 400; i++) {
      String name = random.nextInt(4) == 0 ? "x".repeat(random.nextInt(120)) : "";
      AnvilResult result =
          AnvilCalculator.combine(
              new AnvilInput(
                  randomStack(random),
                  random.nextInt(4) == 0 ? ItemStack.empty() : randomStack(random),
                  name),
              random.nextBoolean(),
              data());
      if (result instanceof AnvilResult.Output output) {
        assertThat(output.levelCost()).isNotNegative();
        assertThat(output.materialUsed()).isNotNegative();
        assertThat(output.result().isEmpty()).isFalse();
        assertSane(LootItems.enchantmentsOf(output.result()));
        assertThat(LootItems.repairCostOf(output.result())).isNotNegative();
      }
    }
  }

  @Test
  void aSurvivalAnvilNeverGivesAnOutputAbove39Levels() {
    // spec: EN-R41d
    Random random = new Random(SEED + 5);
    for (int i = 0; i < 400; i++) {
      AnvilResult result =
          AnvilCalculator.combine(
              new AnvilInput(randomStack(random), randomStack(random), ""), false, data());
      if (result instanceof AnvilResult.Output output) {
        assertThat(output.levelCost()).isLessThanOrEqualTo(39);
      }
    }
  }

  @Test
  void theGrindstoneNeverKeepsAnEnchantmentThatIsNotACurse() {
    // spec: EN-R44a
    Random random = new Random(SEED + 6);
    for (int i = 0; i < 300; i++) {
      var ground =
          GrindstoneCalculator.grind(
              randomStack(random),
              random.nextBoolean() ? ItemStack.empty() : randomStack(random),
              data());
      ground.ifPresent(
          result -> {
            assertThat(result.experience()).isNotNegative();
            for (Identifier id : LootItems.enchantmentsOf(result.output()).keySet()) {
              if (data().enchantments().find(id).isPresent()) {
                assertThat(data().enchantments().isCurse(id, data().registries())).isTrue();
              }
            }
            assertThat(LootItems.repairCostOf(result.output())).isZero();
          });
    }
  }

  @Test
  void theRepairByExperienceNeverCreatesDurabilityOrExperience() {
    // spec: EN-R44b
    Random random = new Random(SEED + 7);
    for (int i = 0; i < 300; i++) {
      ItemStack stack = randomStack(random);
      int experience = random.nextInt(2000);
      RepairResult result = XpRepair.repair(stack, experience, context(random.nextLong()));
      assertThat(result.experienceLeft()).isBetween(0, experience);
      assertThat(result.stack().damage()).isLessThanOrEqualTo(stack.damage());
      assertThat(result.stack().damage()).isNotNegative();
    }
  }
}
