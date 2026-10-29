package dev.shoko.minecraft.loot.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: LT-R41 (explosion_decay), LT-R42 (apply_bonus), LT-R43 (enchanted_count_increase),
// LT-R44a to LT-R44h (set_potion, set_name, set_damage, set_enchantments, set_components)
class ModifierEffectsTest {

  private static final Identifier SHARPNESS = Identifier.minecraft("sharpness");

  private static ItemStack apply(ModifierAction action, ItemStack stack, LootContext context) {
    return ModifierApplier.apply(Tables.modifier(action), stack, context).stack();
  }

  private static LootContext withFortune(int level, double... draws) {
    ItemStack pick =
        LootItems.enchanted(
            LootItems.stack("iron_pickaxe"),
            level == 0 ? Map.of() : Map.of("minecraft:fortune", level));
    return Ctx.builder(ScriptedRandom.of(draws)).tool(pick).build();
  }

  private static ModifierAction bonus(BonusFormula formula) {
    return new ModifierAction.ApplyBonus(Identifier.minecraft("fortune"), formula);
  }

  private static LootContext withLooting(int level, double... draws) {
    FakeEntity attacker =
        FakeEntity.of("player")
            .wearing(
                EquipmentSlot.MAINHAND,
                LootItems.enchanted(
                    LootItems.stack("diamond_sword"), Map.of("minecraft:looting", level)));
    return Ctx.builder(ScriptedRandom.of(draws)).entity(EntityTarget.ATTACKER, attacker).build();
  }

  @Test
  void provisionalExplosionDecayWithoutRadiusLeavesTheStack() {
    // spec: LT-R41
    ScriptedRandom random = ScriptedRandom.of();
    ItemStack result =
        apply(
            new ModifierAction.ExplosionDecay(), LootItems.stack("stone", 5), Ctx.context(random));
    assertThat(result.count()).isEqualTo(5);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalExplosionDecayDrawsOncePerUnitAndNeverAddsUnits() {
    // spec: LT-R41
    ScriptedRandom random = ScriptedRandom.repeating(0.5, 5);
    LootContext context = Ctx.builder(random).explosionRadius(4f).build();
    ItemStack result =
        apply(new ModifierAction.ExplosionDecay(), LootItems.stack("stone", 5), context);
    assertThat(random.steps()).isEqualTo(5);
    assertThat(result.count()).isBetween(0, 5);
  }

  @Test
  void provisionalOreDropsLeavesTheCountAtLevelZero() {
    // spec: LT-R42
    ItemStack result =
        apply(
            bonus(new BonusFormula.OreDrops()),
            LootItems.stack("diamond", 2),
            withFortune(0, 0.999, 0.999));
    assertThat(result.count()).isEqualTo(2);
  }

  @Test
  void provisionalOreDropsMultipliesByTheDrawBoundedBelowByOne() {
    // spec: LT-R42: quantite x max(1, nextInt(niveau + 2))
    ModifierAction ore = bonus(new BonusFormula.OreDrops());
    assertThat(apply(ore, LootItems.stack("diamond", 2), withFortune(2, 0.999)).count())
        .isEqualTo(6);
    assertThat(apply(ore, LootItems.stack("diamond", 2), withFortune(2, 0.0)).count()).isEqualTo(2);
    assertThat(apply(ore, LootItems.stack("diamond", 2), withFortune(2, 0.3)).count()).isEqualTo(2);
  }

  @Test
  void provisionalUniformBonusCountAddsABoundedDraw() {
    // spec: LT-R42: quantite + nextInt(niveau x mult + 1)
    ModifierAction uniform = bonus(new BonusFormula.UniformBonusCount(2));
    assertThat(apply(uniform, LootItems.stack("diamond"), withFortune(2, 0.999)).count())
        .isEqualTo(5);
    assertThat(apply(uniform, LootItems.stack("diamond"), withFortune(2, 0.0)).count())
        .isEqualTo(1);
  }

  @Test
  void provisionalBinomialBonusCountAddsTheSuccessesOfLevelPlusExtraTrials() {
    // spec: LT-R42: niveau + extra tirages flottants < probability
    ModifierAction binomial = bonus(new BonusFormula.BinomialWithBonusCount(3, 0.5f));
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.9, 0.2, 0.7);
    ItemStack pick =
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", 1));
    LootContext context = Ctx.builder(random).tool(pick).build();
    assertThat(apply(binomial, LootItems.stack("diamond"), context).count()).isEqualTo(3);
    assertThat(random.steps()).isEqualTo(4);
  }

  @Test
  void provisionalEnchantedCountIncreaseAddsTheCountPerLevelOfTheAttackerWeapon() {
    // spec: LT-R43 (looting 2, count 1.0 per level)
    ModifierAction looting =
        new ModifierAction.EnchantedCountIncrease(
            new FloatProvider.Constant(1.0f), Identifier.minecraft("looting"), 0);
    assertThat(apply(looting, LootItems.stack("beef"), withLooting(2)).count()).isEqualTo(3);
  }

  @Test
  void provisionalEnchantedCountIncreaseIsNeutralWithoutAnAttacker() {
    // spec: LT-R43
    ModifierAction looting =
        new ModifierAction.EnchantedCountIncrease(
            new FloatProvider.Constant(1.0f), Identifier.minecraft("looting"), 0);
    assertThat(apply(looting, LootItems.stack("beef"), Ctx.context(ScriptedRandom.of())).count())
        .isEqualTo(1);
  }

  @Test
  void provisionalEnchantedCountIncreaseHonoursItsLimit() {
    // spec: LT-R43
    ModifierAction limited =
        new ModifierAction.EnchantedCountIncrease(
            new FloatProvider.Constant(1.0f), Identifier.minecraft("looting"), 2);
    assertThat(apply(limited, LootItems.stack("beef"), withLooting(3)).count())
        .isLessThanOrEqualTo(2);
  }

  @Test
  void provisionalSetNameWritesTheCustomNameOrTheItemName() {
    // spec: LT-R44e (target item_name or custom_name)
    ItemStack input = LootItems.stack("stone");
    ItemStack custom =
        apply(
            new ModifierAction.SetName(Json.parse("'Gem'"), ModifierAction.NameTarget.CUSTOM_NAME),
            input,
            Ctx.context(ScriptedRandom.of()));
    assertThat(custom.component(Identifier.minecraft("custom_name"))).isPresent();
    assertThat(input.component(Identifier.minecraft("custom_name"))).isEmpty();
    ItemStack named =
        apply(
            new ModifierAction.SetName(Json.parse("'Gem'"), ModifierAction.NameTarget.ITEM_NAME),
            input,
            Ctx.context(ScriptedRandom.of()));
    assertThat(named.component(Identifier.minecraft("item_name")))
        .isNotEqualTo(input.component(Identifier.minecraft("item_name")));
    assertThat(named.component(Identifier.minecraft("custom_name"))).isEmpty();
  }

  @Test
  void provisionalSetPotionChangesThePotionContents() {
    // spec: LT-R44a
    ItemStack input = LootItems.stack("potion");
    ItemStack result =
        apply(
            new ModifierAction.SetPotion(Identifier.minecraft("healing")),
            input,
            Ctx.context(ScriptedRandom.of()));
    assertThat(result.component(Identifier.minecraft("potion_contents"))).isPresent();
    assertThat(result.component(Identifier.minecraft("potion_contents")))
        .isNotEqualTo(input.component(Identifier.minecraft("potion_contents")));
  }

  @Test
  void provisionalSetDamageSetsTheFractionOfDurabilityLeft() {
    // spec: LT-R44f (1.0 = neuf; a fraction of the remaining durability)
    ItemStack shears = LootItems.stack("shears");
    ItemStack half =
        apply(
            new ModifierAction.SetDamage(new FloatProvider.Constant(0.5f), false),
            shears,
            Ctx.context(ScriptedRandom.of()));
    assertThat(half.damage()).isEqualTo(119);
    ItemStack fresh =
        apply(
            new ModifierAction.SetDamage(new FloatProvider.Constant(1.0f), false),
            LootItems.damaged(shears, 100),
            Ctx.context(ScriptedRandom.of()));
    assertThat(fresh.damage()).isZero();
  }

  private static ModifierAction setEnchantments(int level, boolean add) {
    SequencedMap<Identifier, IntProvider> levels = new LinkedHashMap<>();
    levels.put(SHARPNESS, new IntProvider.Constant(level));
    return new ModifierAction.SetEnchantments(levels, add);
  }

  @Test
  void provisionalSetEnchantmentsReplacesTheLevelOrAddsToIt() {
    // spec: LT-R44g (add defaults to false)
    ItemStack sword =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 2));
    LootContext context = Ctx.context(ScriptedRandom.of());
    assertThat(LootItems.enchantmentsOf(apply(setEnchantments(3, false), sword, context)))
        .containsEntry(SHARPNESS, 3);
    assertThat(LootItems.enchantmentsOf(apply(setEnchantments(3, true), sword, context)))
        .containsEntry(SHARPNESS, 5);
  }

  @Test
  void provisionalSetEnchantmentsAddsAnEnchantmentTheItemDoesNotHave() {
    // spec: LT-R44g
    ItemStack sword = LootItems.stack("diamond_sword");
    ItemStack result = apply(setEnchantments(4, false), sword, Ctx.context(ScriptedRandom.of()));
    assertThat(LootItems.enchantmentsOf(result)).containsEntry(SHARPNESS, 4).hasSize(1);
  }

  @Test
  void provisionalSetEnchantmentsBoundsTheLevelTo255() {
    // spec: LT-R44g (borne a 255)
    ItemStack result =
        apply(
            setEnchantments(1000, false),
            LootItems.stack("diamond_sword"),
            Ctx.context(ScriptedRandom.of()));
    assertThat(LootItems.enchantmentsOf(result).get(SHARPNESS)).isLessThanOrEqualTo(255);
  }

  @Test
  void provisionalSetComponentsAppliesThePatch() {
    // spec: LT-R44h (components set, "!" removes)
    ComponentPatch patch =
        ComponentPatch.empty()
            .withSet(Identifier.minecraft("custom_name"), new ComponentValue(new StringTag("Gem")))
            .withRemoved(Identifier.minecraft("lore"));
    ItemStack result =
        apply(
            new ModifierAction.SetComponents(patch),
            LootItems.stack("stone"),
            Ctx.context(ScriptedRandom.of()));
    assertThat(result.component(Identifier.minecraft("custom_name")))
        .contains(new ComponentValue(new StringTag("Gem")));
    assertThat(result.component(Identifier.minecraft("lore"))).isEmpty();
  }
}
