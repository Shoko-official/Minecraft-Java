package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.NodeBudget;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.predicate.match.NumberRange;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import java.util.List;

/**
 * A loot condition (LT-R4a, registry {@code loot_condition_type}, 20 types), or a reference to a
 * named predicate. Immutable tree. A list of conditions in the data (a pool, an entry, a modifier,
 * the {@code requirements} of an effect) holds when every one holds, in order (LT-R4b).
 */
public sealed interface LootCondition
    permits LootCondition.AllOf,
        LootCondition.AnyOf,
        LootCondition.Inverted,
        LootCondition.DamageSourceProperties,
        LootCondition.EnchantmentActiveCheck,
        LootCondition.EntityProperties,
        LootCondition.EntityScores,
        LootCondition.EnvironmentAttributeCheck,
        LootCondition.FloatValueCheck,
        LootCondition.IntValueCheck,
        LootCondition.KilledByPlayer,
        LootCondition.LocationCheck,
        LootCondition.MatchBlock,
        LootCondition.MatchTool,
        LootCondition.RandomChance,
        LootCondition.RandomChanceWithEnchantedBonus,
        LootCondition.SurvivesExplosion,
        LootCondition.TableBonus,
        LootCondition.TimeCheck,
        LootCondition.WeatherCheck,
        LootCondition.Reference {

  /**
   * Evaluates the condition. Terms of {@code all_of} and {@code any_of} are evaluated in order and
   * the evaluation stops at the first term that decides, without drawing for the skipped terms
   * (LT-R29, probe PL9). A random condition draws one float in [0, 1[ and passes when it is
   * strictly below the chance (LT-R30 to LT-R33); {@code survives_explosion} with a radius {@code r
   * > 0} passes with probability {@code 1 / r} (LT-R33). A parameter absent from the context
   * follows LT-R15 and gives the cause {@code PARAMETER_ABSENT}. Each random decision is reported
   * to the observer of the context.
   *
   * @param context the context; its random source is consumed by random conditions
   * @return the answer and its cause
   */
  default ConditionResult test(LootContext context) {
    NodeBudget budget = context.budget();
    budget.enter();
    try {
      return ConditionEvaluator.test(this, context, 0);
    } finally {
      budget.exit();
    }
  }

  /**
   * {@code all_of}: every term holds; the empty list holds (LT-R29).
   *
   * @param terms the terms in file order
   */
  record AllOf(List<LootCondition> terms) implements LootCondition {}

  /**
   * {@code any_of}: at least one term holds; the empty list does not hold (LT-R29).
   *
   * @param terms the terms in file order
   */
  record AnyOf(List<LootCondition> terms) implements LootCondition {}

  /**
   * {@code inverted}: the negation of its term (LT-R29).
   *
   * @param term the negated condition
   */
  record Inverted(LootCondition term) implements LootCondition {}

  /**
   * {@code damage_source_properties} (LT-R35, LT-R60).
   *
   * @param predicate the damage source predicate
   */
  record DamageSourceProperties(DamageSourcePredicate predicate) implements LootCondition {}

  /**
   * {@code enchantment_active_check} (LT-R15, LT-R37): fails outside an enchantment context.
   *
   * @param active the expected state
   */
  record EnchantmentActiveCheck(boolean active) implements LootCondition {}

  /**
   * {@code entity_properties} (LT-R35, LT-R57): fails when the entity is absent.
   *
   * @param entity which entity of the context
   * @param predicate the entity predicate
   */
  record EntityProperties(EntityTarget entity, EntityPredicate predicate)
      implements LootCondition {}

  /**
   * {@code entity_scores} (LT-R37): no data uses it, so the scores stay as read.
   *
   * @param entity which entity of the context
   * @param scores the {@code scores} table as read
   */
  record EntityScores(EntityTarget entity, JsonNode scores) implements LootCondition {}

  /**
   * {@code environment_attribute_check} (LT-R37): the shape is unknown, kept as read.
   *
   * @param fields the object as read
   */
  record EnvironmentAttributeCheck(JsonObject fields) implements LootCondition {}

  /**
   * {@code float_value_check} (LT-R37).
   *
   * @param value the provider
   * @param test the accepted range
   */
  record FloatValueCheck(FloatProvider value, NumberRange test) implements LootCondition {}

  /**
   * {@code int_value_check} (LT-R37).
   *
   * @param value the provider
   * @param test the accepted range
   */
  record IntValueCheck(IntProvider value, NumberRange test) implements LootCondition {}

  /** {@code killed_by_player}: holds when {@code attacking_player} is present (LT-R35). */
  record KilledByPlayer() implements LootCondition {}

  /**
   * {@code location_check} (LT-R35, LT-R59): fails without {@code origin}.
   *
   * @param predicate the position predicate
   * @param offsetX the offset added to the origin block, in blocks
   * @param offsetY the offset, the {@code offsetY} key seen in the data
   * @param offsetZ the offset
   */
  record LocationCheck(LocationPredicate predicate, int offsetX, int offsetY, int offsetZ)
      implements LootCondition {}

  /**
   * {@code match_block} (LT-R34): fails without {@code block_state}.
   *
   * @param match the block predicate
   */
  record MatchBlock(BlockMatch match) implements LootCondition {}

  /**
   * {@code match_tool} (LT-R35, LT-R58): fails without {@code tool}.
   *
   * @param predicate the item predicate
   */
  record MatchTool(ItemMatch predicate) implements LootCondition {}

  /**
   * {@code random_chance} (LT-R30): one draw.
   *
   * @param chance the probability, a constant or a provider
   */
  record RandomChance(FloatProvider chance) implements LootCondition {}

  /**
   * {@code random_chance_with_enchanted_bonus} (LT-R31): one draw; the level is the one of the
   * enchantment on the weapon of the {@code attacker}, 0 without attacker.
   *
   * @param enchantment the enchantment identifier
   * @param unenchantedChance the chance at level 0
   * @param enchantedChance the chance at a level above 0
   */
  record RandomChanceWithEnchantedBonus(
      Identifier enchantment, float unenchantedChance, LevelBasedValue enchantedChance)
      implements LootCondition {}

  /** {@code survives_explosion} (LT-R33): one draw with a radius, none without. */
  record SurvivesExplosion() implements LootCondition {}

  /**
   * {@code table_bonus} (LT-R32): one draw; the level is read on {@code tool}, 0 without tool; an
   * index beyond the list is clamped to the last case (provisional, probe PL10).
   *
   * @param enchantment the enchantment identifier
   * @param chances the probability by level, in file order
   */
  record TableBonus(Identifier enchantment, List<Float> chances) implements LootCondition {}

  /**
   * {@code time_check} (LT-R36): no data uses it.
   *
   * @param clock the clock identifier as read
   * @param value the accepted range
   * @param period the modulo applied to the time of day, 0 when the key is absent
   */
  record TimeCheck(Identifier clock, NumberRange value, long period) implements LootCondition {}

  /**
   * {@code weather_check} (LT-R36).
   *
   * @param raining the expected rain, a thunderstorm counts as rain
   * @param thundering the expected thunderstorm
   */
  record WeatherCheck(Tristate raining, Tristate thundering) implements LootCondition {}

  /**
   * A reference to a named predicate (LT-R4a, LT-R61a), such as {@code minecraft:tool/can_shear}.
   *
   * @param predicate the identifier of an element of the {@code predicate} registry
   */
  record Reference(Identifier predicate) implements LootCondition {}
}
