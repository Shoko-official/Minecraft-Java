package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LevelFacts;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Evaluates conditions (LT-R29 to LT-R37). */
final class ConditionEvaluator {

  /** Bound on chained named predicates, so that a cycle ends in a plain failure. */
  private static final int MAX_REFERENCE_DEPTH = 64;

  private static final ConditionResult MATCHED = new ConditionResult(true, ConditionCause.MATCHED);
  private static final ConditionResult NOT_MATCHED =
      new ConditionResult(false, ConditionCause.NOT_MATCHED);
  private static final ConditionResult ABSENT =
      new ConditionResult(false, ConditionCause.PARAMETER_ABSENT);

  private ConditionEvaluator() {}

  static ConditionResult test(LootCondition condition, LootContext context, int depth) {
    return switch (condition) {
      case LootCondition.AllOf all -> allOf(all.terms(), context, depth);
      case LootCondition.AnyOf any -> anyOf(any.terms(), context, depth);
      case LootCondition.Inverted inverted -> of(!test(inverted.term(), context, depth).passed());
      case LootCondition.Reference reference -> reference(reference, context, depth);
      case LootCondition.RandomChance chance -> randomChance(chance, context);
      case LootCondition.RandomChanceWithEnchantedBonus bonus -> enchantedBonus(bonus, context);
      case LootCondition.TableBonus bonus -> tableBonus(bonus, context);
      case LootCondition.SurvivesExplosion survives -> survives(context);
      case LootCondition.KilledByPlayer killed ->
          context.entity(EntityTarget.ATTACKING_PLAYER).isPresent() ? MATCHED : ABSENT;
      case LootCondition.EntityProperties properties -> entityProperties(properties, context);
      case LootCondition.DamageSourceProperties properties ->
          context
              .damageSource()
              .map(source -> of(properties.predicate().matches(context, source)))
              .orElse(ABSENT);
      case LootCondition.MatchBlock match ->
          context
              .blockState()
              .map(state -> of(match.match().matches(state, context.data().registries())))
              .orElse(ABSENT);
      case LootCondition.MatchTool match ->
          context
              .tool()
              .map(tool -> of(match.predicate().matches(tool, context.data().registries())))
              .orElse(ABSENT);
      case LootCondition.LocationCheck location -> locationCheck(location, context);
      case LootCondition.EnchantmentActiveCheck active ->
          context.enchantmentActive().map(state -> of(state == active.active())).orElse(ABSENT);
      case LootCondition.WeatherCheck weather -> weather(weather, context);
      case LootCondition.TimeCheck time -> time(time, context);
      case LootCondition.IntValueCheck check ->
          of(check.test().contains(check.value().sample(context)));
      case LootCondition.FloatValueCheck check ->
          of(check.test().contains(check.value().sample(context)));
      case LootCondition.EntityScores scores -> NOT_MATCHED;
      case LootCondition.EnvironmentAttributeCheck check -> NOT_MATCHED;
    };
  }

  private static ConditionResult of(boolean passed) {
    return passed ? MATCHED : NOT_MATCHED;
  }

  private static ConditionResult allOf(List<LootCondition> terms, LootContext context, int depth) {
    for (LootCondition term : terms) {
      if (!test(term, context, depth).passed()) {
        return NOT_MATCHED;
      }
    }
    return MATCHED;
  }

  private static ConditionResult anyOf(List<LootCondition> terms, LootContext context, int depth) {
    for (LootCondition term : terms) {
      if (test(term, context, depth).passed()) {
        return MATCHED;
      }
    }
    return NOT_MATCHED;
  }

  private static ConditionResult reference(
      LootCondition.Reference reference, LootContext context, int depth) {
    if (depth >= MAX_REFERENCE_DEPTH) {
      return NOT_MATCHED;
    }
    Optional<LootCondition> named = context.data().predicates().find(reference.predicate());
    if (named.isEmpty()) {
      return NOT_MATCHED;
    }
    return test(named.get(), context, depth + 1);
  }

  private static ConditionResult randomChance(
      LootCondition.RandomChance chance, LootContext context) {
    float probability = chance.chance().sample(context);
    return of(context.random().nextFloat() < probability);
  }

  private static ConditionResult enchantedBonus(
      LootCondition.RandomChanceWithEnchantedBonus bonus, LootContext context) {
    int level = attackerLevel(context, bonus.enchantment());
    float probability =
        level > 0 ? bonus.enchantedChance().calculate(level) : bonus.unenchantedChance();
    return of(context.random().nextFloat() < probability);
  }

  private static int attackerLevel(LootContext context, Identifier id) {
    return context
        .entity(EntityTarget.ATTACKER)
        .map(attacker -> attacker.equipment(EquipmentSlot.MAINHAND))
        .map(weapon -> ItemEnchantments.levelOf(weapon, id))
        .orElse(0);
  }

  private static ConditionResult tableBonus(LootCondition.TableBonus bonus, LootContext context) {
    int level =
        context.tool().map(tool -> ItemEnchantments.levelOf(tool, bonus.enchantment())).orElse(0);
    if (bonus.chances().isEmpty()) {
      return NOT_MATCHED;
    }
    float probability = bonus.chances().get(Math.min(level, bonus.chances().size() - 1));
    return of(context.random().nextFloat() < probability);
  }

  private static ConditionResult survives(LootContext context) {
    OptionalDouble radius = context.explosionRadius();
    if (radius.isEmpty()) {
      return new ConditionResult(true, ConditionCause.PARAMETER_ABSENT);
    }
    if (radius.getAsDouble() <= 0) {
      return MATCHED;
    }
    float probability = 1.0f / (float) radius.getAsDouble();
    return of(context.random().nextFloat() < probability);
  }

  private static ConditionResult entityProperties(
      LootCondition.EntityProperties properties, LootContext context) {
    Optional<EntityView> entity = context.entity(properties.entity());
    if (entity.isEmpty()) {
      return ABSENT;
    }
    return of(properties.predicate().matches(entity.get(), context));
  }

  private static ConditionResult locationCheck(
      LootCondition.LocationCheck check, LootContext context) {
    Optional<Vec3> origin = context.origin();
    if (origin.isEmpty()) {
      return ABSENT;
    }
    Vec3 at = origin.get();
    // Long arithmetic: a block outside the int range is not looked up, it never wraps (LT-R35).
    long x = (long) Math.floor(at.x()) + check.offsetX();
    long y = (long) Math.floor(at.y()) + check.offsetY();
    long z = (long) Math.floor(at.z()) + check.offsetZ();
    if (!fitsInt(x) || !fitsInt(y) || !fitsInt(z)) {
      return NOT_MATCHED;
    }
    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
    return of(check.predicate().matches(context, pos));
  }

  private static boolean fitsInt(long value) {
    return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
  }

  private static ConditionResult weather(LootCondition.WeatherCheck check, LootContext context) {
    Optional<LevelFacts> facts = context.level();
    if (facts.isEmpty()) {
      return ABSENT;
    }
    return of(
        holds(check.raining(), facts.get().raining())
            && holds(check.thundering(), facts.get().thundering()));
  }

  private static boolean holds(Tristate expected, boolean actual) {
    return switch (expected) {
      case UNSET -> true;
      case TRUE -> actual;
      case FALSE -> !actual;
    };
  }

  private static ConditionResult time(LootCondition.TimeCheck check, LootContext context) {
    Optional<LevelFacts> facts = context.level();
    if (facts.isEmpty()) {
      return ABSENT;
    }
    long time = facts.get().dayTime();
    if (check.period() > 0) {
      time = Math.floorMod(time, check.period());
    }
    return of(check.value().contains((double) time));
  }
}
