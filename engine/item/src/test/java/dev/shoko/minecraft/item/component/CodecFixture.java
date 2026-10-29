package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;

/**
 * A component codec over the fake component registry plus {@code pot_decorations}, and samples of
 * the JSON shapes that the decoder must accept (IC-R12, V-I1 to V-I6). Nothing is built at class
 * initialization: a failing implementation must fail inside the test.
 */
final class CodecFixture {

  /** A component and a JSON value of one of its documented shapes ({@code '} for quotes). */
  record Sample(String component, String json) {

    @Override
    public String toString() {
      return component + "=" + json;
    }
  }

  private static final String POT =
      "{'back':{'id':'minecraft:brick'},'front':{'id':'minecraft:brick'},"
          + "'left':{'id':'minecraft:brick'},'right':{'id':'minecraft:brick'}}";

  private static final String TOOL =
      "{'rules':[{'blocks':'#minecraft:incorrect_for_stone_tool','correct_for_drops':false},"
          + "{'blocks':'#minecraft:mineable/pickaxe','correct_for_drops':true,'speed':4.0}]}";

  private static final String MODIFIERS =
      "[{'type':'minecraft:attack_damage','amount':2.0,'id':'minecraft:base_attack_damage',"
          + "'operation':'add_value','slot':'mainhand'},"
          + "{'type':'minecraft:attack_speed','amount':-2.799999952316284,"
          + "'id':'minecraft:base_attack_speed','operation':'add_value','slot':'mainhand'}]";

  private static final String MILK =
      "{'animation':'drink','has_consume_particles':false,"
          + "'on_consume_effects':[{'type':'minecraft:clear_all_effects'}],"
          + "'sound':'minecraft:entity.generic.drink'}";

  private CodecFixture() {}

  static ComponentCodec codec() {
    List<Identifier> ids = new ArrayList<>(TestItems.COMPONENT_IDS);
    ids.add(Identifier.minecraft("pot_decorations"));
    return ComponentCodec.of(ComponentCatalog.of(ids));
  }

  static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  static ComponentValue decode(ComponentCodec codec, String component, String relaxedJson) {
    return Findings.value(codec.valueFromJson(id(component), Json.parse(relaxedJson)));
  }

  static List<Sample> samples() {
    return List.of(
        new Sample("rarity", "'common'"),
        new Sample("max_stack_size", "64"),
        new Sample("max_stack_size", "1"),
        new Sample("damage", "0"),
        new Sample("max_damage", "131"),
        new Sample("repair_cost", "0"),
        new Sample("item_model", "'minecraft:stone'"),
        new Sample("break_sound", "'minecraft:entity.item.break'"),
        new Sample("item_name", "{'translate':'item.minecraft.stone'}"),
        new Sample("lore", "[]"),
        new Sample("enchantments", "{}"),
        new Sample("enchantments", "{'minecraft:sharpness':1}"),
        new Sample("stored_enchantments", "{'minecraft:sharpness':3,'minecraft:unbreaking':2}"),
        new Sample("attack_animation", "{}"),
        new Sample("interact_animation", "{}"),
        new Sample("tooltip_display", "{}"),
        new Sample("use_effects", "{}"),
        new Sample("food", "{'nutrition':5,'saturation':6.0}"),
        new Sample("food", "{'nutrition':4,'saturation':2.4,'can_always_eat':true}"),
        new Sample("villager_food", "{'nutrition':4}"),
        new Sample("consumable", "{}"),
        new Sample("consumable", MILK),
        new Sample("enchantable", "{'value':5}"),
        new Sample("repairable", "{'items':'#minecraft:stone_tool_materials'}"),
        new Sample("repairable", "{'items':['minecraft:iron_ingot','minecraft:gold_ingot']}"),
        new Sample("tool", TOOL),
        new Sample("weapon", "{'item_damage_per_attack':2}"),
        new Sample("weapon", "{'item_damage_per_attack':1,'disable_blocking_for_seconds':5.0}"),
        new Sample(
            "equippable",
            "{'slot':'head','asset_id':'minecraft:iron','equip_sound':'minecraft:item.armor.equip_iron'}"),
        new Sample("attribute_modifiers", "[]"),
        new Sample("attribute_modifiers", MODIFIERS),
        new Sample("use_remainder", "{'id':'minecraft:bucket'}"),
        new Sample("container", "[]"),
        new Sample(
            "cooking_fuel",
            "{'burn_time':'minecraft:cooking/time_coal',"
                + "'speed_multiplier':'minecraft:cooking/speed_default'}"),
        new Sample(
            "brewing_fuel",
            "{'speed_multiplier':'minecraft:brewing/speed_default',"
                + "'uses':'minecraft:brewing/uses_default'}"),
        new Sample("compostable", "{'layers':'minecraft:compostable/medium_high'}"),
        new Sample("potion_contents", "{}"),
        new Sample("potion_contents", "{'potion':'minecraft:strong_healing'}"),
        new Sample("pot_decorations", POT),
        new Sample("banner_patterns", "[]"),
        new Sample("provides_trim_material", "'minecraft:emerald'"));
  }
}
