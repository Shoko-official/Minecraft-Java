package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import java.util.List;

/**
 * An entity or location effect (registries {@code enchantment_entity_effect_type}, 15 types, and
 * {@code enchantment_location_based_effect_type}, 16 types: EN-R26 to EN-R28). The parameters keep
 * their JSON form: the fields are listed in EN-R28, their execution belongs to the jalon M7, and
 * the evaluation into intents reads them when the effect runs.
 */
public sealed interface EntityEffect permits EntityEffect.Typed, EntityEffect.AllOf {

  /**
   * An effect other than {@code all_of}.
   *
   * @param type the type identifier, such as {@code minecraft:ignite}
   * @param parameters the object as read, {@code type} excluded
   */
  record Typed(Identifier type, JsonObject parameters) implements EntityEffect {}

  /**
   * {@code all_of}: the effects run in series (EN-R17).
   *
   * @param effects the effects in file order
   */
  record AllOf(List<EntityEffect> effects) implements EntityEffect {}
}
