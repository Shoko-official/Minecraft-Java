package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;

/**
 * The value of one effect component of an enchantment (EN-R11, EN-R12): the three forms of the spec
 * plus a raw fallback.
 */
public sealed interface EffectComponentValue
    permits EffectComponentValue.ValueEffects,
        EffectComponentValue.DirectValue,
        EffectComponentValue.EntityEffects,
        EffectComponentValue.Flag,
        EffectComponentValue.Sounds,
        EffectComponentValue.Attributes,
        EffectComponentValue.Raw {

  /**
   * A list of entries with a value effect: {@code damage}, {@code damage_protection}, {@code
   * knockback}, {@code item_damage}, {@code ammo_use}...
   *
   * @param entries the entries in file order
   */
  record ValueEffects(List<ConditionalValueEffect> entries) implements EffectComponentValue {}

  /**
   * A value effect without a list, always applied: {@code crossbow_charge_time}, {@code
   * trident_spin_attack_strength} (EN-R12 form b).
   *
   * @param effect the value effect
   */
  record DirectValue(ValueEffect effect) implements EffectComponentValue {}

  /**
   * A list of entries with an entity effect: {@code post_attack}, {@code tick}, {@code
   * location_changed}, {@code hit_block}...
   *
   * @param entries the entries in file order
   */
  record EntityEffects(List<ConditionalEntityEffect> entries) implements EffectComponentValue {}

  /**
   * An indicator written {@code {}}: {@code prevent_armor_change}, {@code prevent_equipment_drop}.
   */
  record Flag() implements EffectComponentValue {}

  /**
   * A list of sound identifiers: {@code trident_sound}.
   *
   * @param sounds the sounds in file order
   */
  record Sounds(List<Identifier> sounds) implements EffectComponentValue {}

  /**
   * A list of attribute modifiers: {@code attributes}.
   *
   * @param attributes the modifiers in file order
   */
  record Attributes(List<AttributeEffect> attributes) implements EffectComponentValue {}

  /**
   * A form not typed above, such as the {@code {start, end}} list of {@code
   * crossbow_charging_sounds}. Kept as read.
   *
   * @param value the JSON value
   */
  record Raw(JsonNode value) implements EffectComponentValue {}
}
