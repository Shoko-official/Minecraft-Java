package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Set;

/**
 * The 31 registered effect components and the form of the value of each (EN-R11, EN-R12). The forms
 * are read from the data: the components in a list with a value effect, the two direct value
 * effects, the lists with an entity or location effect, the two indicators and the three other
 * shapes.
 */
final class ComponentForms {

  enum Form {
    VALUE_LIST,
    DIRECT_VALUE,
    ENTITY_LIST,
    FLAG,
    SOUNDS,
    ATTRIBUTES,
    RAW
  }

  private static final Set<String> VALUE_LIST =
      Set.of(
          "ammo_use",
          "armor_effectiveness",
          "block_experience",
          "damage",
          "damage_immunity",
          "damage_protection",
          "fishing_luck_bonus",
          "fishing_time_reduction",
          "item_damage",
          "knockback",
          "mob_experience",
          "projectile_count",
          "projectile_piercing",
          "projectile_spread",
          "repair_with_xp",
          "smash_damage_per_fallen_block",
          "trident_return_acceleration");

  private static final Set<String> ENTITY_LIST =
      Set.of(
          "equipment_drops",
          "hit_block",
          "location_changed",
          "post_attack",
          "post_piercing_attack",
          "projectile_spawned",
          "tick");

  private ComponentForms() {}

  static boolean isRegistered(Identifier id) {
    return id.namespace().equals("minecraft") && form(id.path()) != null;
  }

  static Form form(String name) {
    if (VALUE_LIST.contains(name)) {
      return Form.VALUE_LIST;
    }
    if (ENTITY_LIST.contains(name)) {
      return Form.ENTITY_LIST;
    }
    return switch (name) {
      case "crossbow_charge_time", "trident_spin_attack_strength" -> Form.DIRECT_VALUE;
      case "prevent_armor_change", "prevent_equipment_drop" -> Form.FLAG;
      case "trident_sound" -> Form.SOUNDS;
      case "attributes" -> Form.ATTRIBUTES;
      case "crossbow_charging_sounds" -> Form.RAW;
      default -> null;
    };
  }
}
