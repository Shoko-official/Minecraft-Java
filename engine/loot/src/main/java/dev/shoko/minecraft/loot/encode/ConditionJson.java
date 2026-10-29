package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;

/** Encodes conditions to a JSON tree (inverse of the condition decoder). */
final class ConditionJson {

  private ConditionJson() {}

  /** One condition: an object, or a string for a reference. */
  static Object condition(LootCondition condition) {
    return switch (condition) {
      case LootCondition.Reference reference -> reference.predicate().toString();
      case LootCondition.AllOf all -> typed("all_of", "terms", terms(all.terms()));
      case LootCondition.AnyOf any -> typed("any_of", "terms", terms(any.terms()));
      case LootCondition.Inverted inverted -> typed("inverted", "term", condition(inverted.term()));
      case LootCondition.KilledByPlayer killed -> typed("killed_by_player");
      case LootCondition.SurvivesExplosion survives -> typed("survives_explosion");
      case LootCondition.RandomChance chance ->
          typed("random_chance", "chance", ProviderJson.floatProvider(chance.chance()));
      case LootCondition.RandomChanceWithEnchantedBonus bonus ->
          typed(
              "random_chance_with_enchanted_bonus",
              "unenchanted_chance",
              JsonOut.number(bonus.unenchantedChance()),
              "enchanted_chance",
              ProviderJson.levelBased(bonus.enchantedChance()),
              "enchantment",
              bonus.enchantment().toString());
      case LootCondition.TableBonus bonus ->
          typed(
              "table_bonus",
              "enchantment",
              bonus.enchantment().toString(),
              "chances",
              bonus.chances().stream().map(c -> (Object) JsonOut.number(c.floatValue())).toList());
      case LootCondition.MatchBlock match -> {
        SequencedMap<String, Object> object = typed("match_block");
        object.putAll(MatchJson.block(match.match()));
        yield object;
      }
      case LootCondition.MatchTool tool ->
          typed("match_tool", "predicate", MatchJson.item(tool.predicate()));
      default -> other(condition);
    };
  }

  private static Object other(LootCondition condition) {
    return switch (condition) {
      case LootCondition.EntityProperties properties ->
          typed(
              "entity_properties",
              "entity",
              properties.entity().key(),
              "predicate",
              MatchJson.entity(properties.predicate()));
      case LootCondition.DamageSourceProperties damage ->
          typed(
              "damage_source_properties", "predicate", MatchJson.damageSource(damage.predicate()));
      case LootCondition.IntValueCheck check ->
          typed(
              "int_value_check",
              "value",
              ProviderJson.intProvider(check.value()),
              "test",
              MatchJson.range(check.test()));
      case LootCondition.FloatValueCheck check ->
          typed(
              "float_value_check",
              "value",
              ProviderJson.floatProvider(check.value()),
              "test",
              MatchJson.range(check.test()));
      case LootCondition.EntityScores scores ->
          typed(
              "entity_scores",
              "entity",
              scores.entity().key(),
              "scores",
              JsonOut.tree(scores.scores()));
      case LootCondition.EnvironmentAttributeCheck check -> {
        SequencedMap<String, Object> object = typed("environment_attribute_check");
        check.fields().members().forEach((key, value) -> object.put(key, JsonOut.tree(value)));
        yield object;
      }
      default -> placed(condition);
    };
  }

  private static Object placed(LootCondition condition) {
    return switch (condition) {
      case LootCondition.LocationCheck location -> {
        SequencedMap<String, Object> object = typed("location_check");
        if (location.offsetX() != 0) {
          object.put("offsetX", JsonOut.number(location.offsetX()));
        }
        if (location.offsetY() != 0) {
          object.put("offsetY", JsonOut.number(location.offsetY()));
        }
        if (location.offsetZ() != 0) {
          object.put("offsetZ", JsonOut.number(location.offsetZ()));
        }
        object.put("predicate", MatchJson.location(location.predicate()));
        yield object;
      }
      case LootCondition.EnchantmentActiveCheck active ->
          typed("enchantment_active_check", "active", active.active());
      case LootCondition.WeatherCheck weather -> {
        SequencedMap<String, Object> object = typed("weather_check");
        if (weather.raining() != Tristate.UNSET) {
          object.put("raining", weather.raining() == Tristate.TRUE);
        }
        if (weather.thundering() != Tristate.UNSET) {
          object.put("thundering", weather.thundering() == Tristate.TRUE);
        }
        yield object;
      }
      case LootCondition.TimeCheck time -> {
        SequencedMap<String, Object> object = typed("time_check");
        if (time.clock() != null) {
          object.put("clock", time.clock().toString());
        }
        object.put("value", MatchJson.range(time.value()));
        if (time.period() > 0) {
          object.put("period", JsonOut.number(time.period()));
        }
        yield object;
      }
      default -> throw new IllegalStateException("unhandled condition " + condition);
    };
  }

  /** The value of a condition key: one condition as itself, several as a list. */
  static Object conditions(List<LootCondition> conditions) {
    if (conditions.size() == 1) {
      return condition(conditions.getFirst());
    }
    return terms(conditions);
  }

  private static List<Object> terms(List<LootCondition> terms) {
    List<Object> list = new ArrayList<>();
    terms.forEach(term -> list.add(condition(term)));
    return list;
  }

  private static SequencedMap<String, Object> typed(String type, Object... keyValues) {
    SequencedMap<String, Object> object = JsonOut.object();
    object.put("type", "minecraft:" + type);
    for (int i = 0; i < keyValues.length; i += 2) {
      object.put((String) keyValues[i], keyValues[i + 1]);
    }
    return object;
  }
}
