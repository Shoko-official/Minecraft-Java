package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.ActionSink;
import dev.shoko.minecraft.loot.port.EffectIntent;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import java.util.Optional;

/**
 * Turns the entity and location effects of an enchantment into intents (EN-R16, EN-R26 to EN-R28):
 * the level-based values are evaluated, the random ones are drawn, and the result is sent to the
 * sink. Effects that need a block state (replace_block, replace_disk, set_block_properties) are not
 * turned into intents: they belong to the block rules of the jalon M5.
 */
final class EffectIntents {

  private static final float TICKS_PER_SECOND = 20f;

  private EffectIntents() {}

  static void emit(ConditionalEntityEffect entry, int level, LootContext context, ActionSink sink) {
    EntityView target = target(entry.affected(), context).orElse(null);
    if (target != null) {
      send(entry.effect(), target, level, context, sink);
    }
  }

  private static Optional<EntityView> target(AffectedTarget affected, LootContext context) {
    return switch (affected) {
      case UNSET, VICTIM -> context.entity(EntityTarget.THIS);
      case ATTACKER -> context.entity(EntityTarget.ATTACKER);
      case DAMAGING_ENTITY -> context.entity(EntityTarget.DIRECT_ATTACKER);
    };
  }

  private static void send(
      EntityEffect effect, EntityView target, int level, LootContext context, ActionSink sink) {
    switch (effect) {
      case EntityEffect.AllOf all ->
          all.effects().forEach(inner -> send(inner, target, level, context, sink));
      case EntityEffect.Typed typed -> {
        EffectIntent intent = intent(typed, target, level, context);
        if (intent != null) {
          sink.accept(intent);
        }
      }
    }
  }

  private static EffectIntent intent(
      EntityEffect.Typed effect, EntityView target, int level, LootContext context) {
    Params params = new Params(effect.parameters(), level);
    Vec3 where = target.position();
    return switch (effect.type().path()) {
      case "ignite" -> new EffectIntent.Ignite(target, params.number("duration", 0f));
      case "apply_exhaustion" ->
          new EffectIntent.ApplyExhaustion(target, params.number("amount", 0f));
      case "apply_impulse" -> impulse(target, params);
      case "apply_mob_effect" -> mobEffect(target, params, context);
      case "change_item_damage" ->
          new EffectIntent.ChangeItemDamage(
              target, EquipmentSlot.MAINHAND, Math.round(params.number("amount", 0f)));
      case "damage_entity" -> damage(target, params, context);
      case "explode" ->
          new EffectIntent.Explode(
              where, params.number("radius", 0f), params.number("knockback_multiplier", 1f));
      case "summon_entity" ->
          params
              .id("entity")
              .map(id -> (EffectIntent) new EffectIntent.SummonEntity(id, where))
              .orElse(null);
      case "play_sound" -> sound(where, params, context);
      case "spawn_particles" ->
          params
              .particle()
              .map(
                  id ->
                      (EffectIntent)
                          new EffectIntent.SpawnParticles(id, where, params.number("speed", 0f)))
              .orElse(null);
      case "run_function" ->
          params
              .id("function")
              .map(id -> (EffectIntent) new EffectIntent.RunFunction(id))
              .orElse(null);
      default -> null;
    };
  }

  private static EffectIntent impulse(EntityView target, Params params) {
    double[] direction = params.vector("direction", 0);
    double[] scale = params.vector("coordinate_scale", 1);
    double magnitude = params.number("magnitude", 1f);
    return new EffectIntent.ApplyImpulse(
        target,
        new Vec3(
            direction[0] * scale[0] * magnitude,
            direction[1] * scale[1] * magnitude,
            direction[2] * scale[2] * magnitude));
  }

  private static EffectIntent mobEffect(EntityView target, Params params, LootContext context) {
    Optional<Identifier> effect = params.id("to_apply");
    if (effect.isEmpty()) {
      return null;
    }
    float duration = between(params, "min_duration", "max_duration", 0f, context);
    float amplifier = between(params, "min_amplifier", "max_amplifier", 0f, context);
    return new EffectIntent.ApplyMobEffect(
        target, effect.get(), Math.round(duration * TICKS_PER_SECOND), (int) amplifier);
  }

  private static EffectIntent damage(EntityView target, Params params, LootContext context) {
    Optional<Identifier> type = params.id("damage_type");
    if (type.isEmpty()) {
      return null;
    }
    return new EffectIntent.DamageEntity(
        target, type.get(), between(params, "min_damage", "max_damage", 0f, context));
  }

  private static EffectIntent sound(Vec3 where, Params params, LootContext context) {
    Optional<Identifier> sound = params.id("sound");
    if (sound.isEmpty()) {
      return null;
    }
    return new EffectIntent.PlaySound(
        sound.get(), where, params.number("volume", 1f), params.pitch(context));
  }

  private static float between(
      Params params, String minKey, String maxKey, float fallback, LootContext context) {
    float min = params.number(minKey, fallback);
    float max = params.number(maxKey, min);
    return max > min ? min + context.random().nextFloat() * (max - min) : min;
  }

  /** Reads the parameters of an effect, evaluating the level-based values at the level. */
  private static final class Params {
    private final JsonObject object;
    private final int level;

    Params(JsonObject object, int level) {
      this.object = object;
      this.level = level;
    }

    float number(String key, float fallback) {
      JsonNode node = object.members().get(key);
      if (node == null) {
        return fallback;
      }
      LevelBasedValue value = new Findings().take(EffectCodec.levelBased(node));
      return value == null ? fallback : value.calculate(level);
    }

    Optional<Identifier> id(String key) {
      JsonNode node = object.members().get(key);
      if (node instanceof JsonArray array && !array.elements().isEmpty()) {
        node = array.elements().getFirst();
      }
      return node instanceof JsonString text ? Identifier.tryParse(text.value()) : Optional.empty();
    }

    Optional<Identifier> particle() {
      JsonNode node = object.members().get("particle");
      if (node instanceof JsonObject particle
          && particle.members().get("type") instanceof JsonString type) {
        return Identifier.tryParse(type.value());
      }
      return Optional.empty();
    }

    double[] vector(String key, double fallback) {
      double[] values = {fallback, fallback, fallback};
      if (object.members().get(key) instanceof JsonArray array && array.elements().size() == 3) {
        for (int i = 0; i < 3; i++) {
          if (array.elements().get(i) instanceof JsonNumber number) {
            values[i] = number.doubleValue();
          }
        }
      }
      return values;
    }

    float pitch(LootContext context) {
      JsonNode node = object.members().get("pitch");
      if (node instanceof JsonObject range) {
        float min = bound(range, "min_inclusive");
        float max = bound(range, "max_exclusive");
        return max > min ? min + context.random().nextFloat() * (max - min) : min;
      }
      return number("pitch", 1f);
    }

    private static float bound(JsonObject range, String key) {
      return range.members().get(key) instanceof JsonNumber number
          ? (float) number.doubleValue()
          : 1f;
    }
  }
}
