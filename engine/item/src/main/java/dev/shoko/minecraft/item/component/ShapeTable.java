package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.Shape.AnyCompound;
import dev.shoko.minecraft.item.component.Shape.AnyValue;
import dev.shoko.minecraft.item.component.Shape.BoolS;
import dev.shoko.minecraft.item.component.Shape.DoubleS;
import dev.shoko.minecraft.item.component.Shape.Field;
import dev.shoko.minecraft.item.component.Shape.FloatS;
import dev.shoko.minecraft.item.component.Shape.IdSet;
import dev.shoko.minecraft.item.component.Shape.Ident;
import dev.shoko.minecraft.item.component.Shape.IntS;
import dev.shoko.minecraft.item.component.Shape.ListOf;
import dev.shoko.minecraft.item.component.Shape.MapOf;
import dev.shoko.minecraft.item.component.Shape.Obj;
import dev.shoko.minecraft.item.component.Shape.Str;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of each component type (IC-R9, IC-R12). The 69 types met in the prototypes follow the
 * fields observed there: a field present in every prototype that carries the component is required
 * when at least four prototypes carry it (a single prototype is too little evidence to refuse its
 * siblings), the others are optional. The types absent from the prototypes follow the public
 * documentation (IC-R9b, provisional) and are lenient wherever it is not certain. A type that this
 * table does not name is accepted as any value.
 */
final class ShapeTable {

  private static final Shape STR = new Str();
  private static final Shape IDENT = new Ident();
  private static final Shape ID_SET = new IdSet();
  private static final Shape INT = new IntS(Integer.MIN_VALUE, Integer.MAX_VALUE);
  private static final Shape NON_NEGATIVE = new IntS(0, Integer.MAX_VALUE);

  /** IC-R22 (provisional): an enchantment level lies in 1 to 255. */
  private static final Shape LEVEL = new IntS(1, 255);

  private static final Shape FLOAT = new FloatS();
  private static final Shape DOUBLE = new DoubleS();
  private static final Shape BOOL = new BoolS();
  private static final Shape ANY = new AnyValue();
  private static final Shape COMPOUND = new AnyCompound();
  private static final Shape ANY_LIST = new ListOf(ANY);
  private static final Shape COMPOUND_LIST = new ListOf(COMPOUND);

  private static final Map<String, Shape> BY_PATH = build();

  private ShapeTable() {}

  /**
   * Returns the shape of a component.
   *
   * @param id a component identifier
   * @return its shape, any value for a type the table does not name
   */
  static Shape of(Identifier id) {
    return id.namespace().equals(Identifier.DEFAULT_NAMESPACE)
        ? BY_PATH.getOrDefault(id.path(), ANY)
        : ANY;
  }

  private static Field req(String name, Shape shape) {
    return new Field(name, shape, true);
  }

  private static Field opt(String name, Shape shape) {
    return new Field(name, shape, false);
  }

  private static Shape obj(Field... fields) {
    return new Obj(List.of(fields));
  }

  private static Shape floats(String... names) {
    Field[] fields = new Field[names.length];
    for (int i = 0; i < names.length; i++) {
      fields[i] = req(names[i], FLOAT);
    }
    return obj(fields);
  }

  private static Map<String, Shape> build() {
    Map<String, Shape> all = new HashMap<>();
    all.putAll(measuredFirst());
    all.putAll(measuredLast());
    all.putAll(documented());
    return Map.copyOf(all);
  }

  /** Types met in the prototypes, from {@code attack_animation} to {@code food}. */
  private static Map<String, Shape> measuredFirst() {
    return Map.ofEntries(
        Map.entry("attack_animation", obj(opt("type", STR), opt("duration", INT))),
        Map.entry(
            "attack_range",
            floats(
                "hitbox_margin",
                "max_creative_reach",
                "max_reach",
                "min_creative_reach",
                "min_reach",
                "mob_factor")),
        Map.entry("attribute_modifiers", new ListOf(modifier())),
        Map.entry("banner_patterns", ANY_LIST),
        Map.entry("bees", ANY_LIST),
        Map.entry("block_state", COMPOUND),
        Map.entry("blocks_attacks", blocksAttacks()),
        Map.entry("brewing_fuel", obj(opt("speed_multiplier", IDENT), opt("uses", IDENT))),
        Map.entry("bucket_entity_data", COMPOUND),
        Map.entry("bundle_contents", ANY_LIST),
        Map.entry("charged_projectiles", ANY_LIST),
        Map.entry("compostable", obj(req("layers", IDENT))),
        Map.entry("consumable", consumable()),
        Map.entry("container", new ListOf(obj(req("slot", INT), req("item", COMPOUND)))),
        Map.entry("cooking_fuel", obj(req("burn_time", IDENT), req("speed_multiplier", IDENT))),
        Map.entry("damage", NON_NEGATIVE),
        Map.entry("damage_resistant", obj(req("types", ID_SET))),
        Map.entry("death_protection", obj(opt("death_effects", COMPOUND_LIST))),
        Map.entry("debug_stick_state", COMPOUND),
        Map.entry("enchantable", obj(req("value", INT))),
        Map.entry("enchantment_glint_override", BOOL),
        Map.entry("enchantments", new MapOf(LEVEL)),
        Map.entry("entity_data", COMPOUND),
        Map.entry("equippable", equippable()),
        Map.entry("fireworks", obj(opt("flight_duration", INT), opt("explosions", ANY_LIST))),
        Map.entry(
            "food",
            obj(req("nutrition", INT), req("saturation", FLOAT), opt("can_always_eat", BOOL))),
        Map.entry("glider", COMPOUND));
  }

  /**
   * Types met in the prototypes, from {@code interact_animation} to {@code writable_book_content}.
   */
  private static Map<String, Shape> measuredLast() {
    return Map.ofEntries(
        Map.entry("interact_animation", COMPOUND),
        Map.entry("item_model", IDENT),
        Map.entry("kinetic_weapon", kineticWeapon()),
        Map.entry("lore", ANY_LIST),
        Map.entry("map_decorations", COMPOUND),
        Map.entry("max_damage", INT),
        Map.entry("max_stack_size", INT),
        Map.entry("minimum_attack_charge", FLOAT),
        Map.entry(
            "mob_visibility", obj(req("targeting_entity_types", ID_SET), req("visibility", FLOAT))),
        Map.entry("ominous_bottle_amplifier", INT),
        Map.entry("piercing_weapon", obj(req("hit_sound", STR), req("sound", STR))),
        Map.entry("pot_decorations", potDecorations()),
        Map.entry("potion_contents", potionContents()),
        Map.entry("potion_duration_scale", FLOAT),
        Map.entry("rarity", STR),
        Map.entry("recipes", new ListOf(IDENT)),
        Map.entry("repair_cost", INT),
        Map.entry("repairable", obj(req("items", ID_SET))),
        Map.entry("sign_text_back", signText()),
        Map.entry("sign_text_front", signText()),
        Map.entry("stored_enchantments", new MapOf(LEVEL)),
        Map.entry("suspicious_stew_effects", COMPOUND_LIST),
        Map.entry("tool", tool()),
        Map.entry("tooltip_display", COMPOUND),
        Map.entry("use_cooldown", obj(opt("seconds", FLOAT))),
        Map.entry(
            "use_effects",
            obj(
                opt("can_sprint", BOOL),
                opt("interact_vibrations", BOOL),
                opt("speed_multiplier", FLOAT))),
        Map.entry(
            "use_remainder", obj(req("id", IDENT), opt("count", INT), opt("components", COMPOUND))),
        Map.entry("villager_food", obj(req("nutrition", INT))),
        Map.entry(
            "weapon",
            obj(opt("disable_blocking_for_seconds", FLOAT), opt("item_damage_per_attack", INT))),
        Map.entry("writable_book_content", COMPOUND));
  }

  /** Types absent from the prototypes (IC-R9b, provisional). */
  private static Map<String, Shape> documented() {
    return Map.ofEntries(
        Map.entry("trim", obj(req("material", ANY), req("pattern", ANY))),
        Map.entry("unbreakable", COMPOUND),
        Map.entry("custom_data", COMPOUND),
        Map.entry("custom_model_data", COMPOUND),
        Map.entry("creative_slot_lock", COMPOUND),
        Map.entry("intangible_projectile", COMPOUND),
        Map.entry("tooltip_style", IDENT),
        Map.entry("additional_trade_cost", INT),
        Map.entry("map_id", INT),
        Map.entry("written_book_content", COMPOUND),
        Map.entry("block_entity_data", COMPOUND),
        Map.entry("container_loot", COMPOUND));
  }

  private static Shape modifier() {
    return obj(
        req("type", IDENT),
        req("amount", DOUBLE),
        req("id", IDENT),
        req("operation", STR),
        req("slot", STR),
        opt("display", COMPOUND));
  }

  private static Shape blocksAttacks() {
    return obj(
        opt("block_delay_seconds", FLOAT),
        opt("block_sound", STR),
        opt("bypassed_by", STR),
        opt("disabled_sound", STR),
        opt("item_damage", obj(opt("base", FLOAT), opt("factor", FLOAT), opt("threshold", FLOAT))));
  }

  private static Shape consumable() {
    return obj(
        opt("consume_seconds", FLOAT),
        opt("animation", STR),
        opt("sound", STR),
        opt("has_consume_particles", BOOL),
        opt("on_consume_effects", COMPOUND_LIST));
  }

  private static Shape equippable() {
    return obj(
        req("slot", STR),
        opt("asset_id", STR),
        opt("equip_sound", STR),
        opt("allowed_entities", ID_SET),
        opt("can_be_sheared", BOOL),
        opt("shearing_sound", STR),
        opt("swappable", BOOL),
        opt("equip_on_interact", BOOL),
        opt("damage_on_hurt", BOOL),
        opt("camera_overlay", STR));
  }

  private static Shape kineticWeapon() {
    return obj(
        req("damage_conditions", COMPOUND),
        req("damage_multiplier", FLOAT),
        req("delay_ticks", INT),
        req("dismount_conditions", COMPOUND),
        req("forward_movement", FLOAT),
        req("hit_sound", STR),
        req("knockback_conditions", COMPOUND),
        req("sound", STR));
  }

  private static Shape potDecorations() {
    return obj(
        opt("back", COMPOUND),
        opt("front", COMPOUND),
        opt("left", COMPOUND),
        opt("right", COMPOUND));
  }

  private static Shape potionContents() {
    return obj(
        opt("potion", IDENT),
        opt("custom_color", INT),
        opt("custom_effects", COMPOUND_LIST),
        opt("custom_name", STR));
  }

  private static Shape signText() {
    return obj(req("color", STR), req("has_glowing_text", BOOL), req("messages", ANY_LIST));
  }

  private static Shape tool() {
    Shape rule = obj(req("blocks", ID_SET), opt("correct_for_drops", BOOL), opt("speed", FLOAT));
    return obj(
        req("rules", new ListOf(rule)),
        opt("can_destroy_blocks_in_creative", BOOL),
        opt("damage_per_block", INT));
  }
}
