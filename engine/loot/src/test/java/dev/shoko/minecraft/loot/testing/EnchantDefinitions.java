package dev.shoko.minecraft.loot.testing;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The JSON text of the made-up enchantments (field shapes of EN-R2, EN-R12, EN-R17), not copies of
 * the reference data. Each definition is built from {@code head} = max level, weight, anvil cost
 * and {@code cost} = base and per level of the minimum cost, then of the maximum cost.
 */
final class EnchantDefinitions {

  private static final String LINEAR_DAMAGE =
      "{'type':'minecraft:add','value':{'type':'minecraft:linear','base':1.0,"
          + "'per_level_above_first':0.5}}";

  private static final String NOT_BYPASSING =
      "{'type':'minecraft:inverted','term':{'type':'minecraft:damage_source_properties',"
          + "'predicate':{'tags':[{'id':'minecraft:bypasses_invulnerability','expected':true}]}}}";

  private static final String IS_ARMOR =
      "{'type':'minecraft:match_tool','predicate':{'items':'#minecraft:enchantable/armor'}}";

  private static final String ARMOR_UNBREAKING =
      "{'effect':{'type':'minecraft:remove_binomial','chance':{'type':'minecraft:fraction',"
          + "'numerator':{'type':'minecraft:linear','base':2,'per_level_above_first':2},"
          + "'denominator':{'type':'minecraft:linear','base':10,'per_level_above_first':5}}},"
          + "'requirements':"
          + IS_ARMOR
          + "}";

  private static final String OTHER_UNBREAKING =
      "{'effect':{'type':'minecraft:remove_binomial','chance':{'type':'minecraft:fraction',"
          + "'numerator':{'type':'minecraft:linear','base':1,'per_level_above_first':1},"
          + "'denominator':{'type':'minecraft:linear','base':2,'per_level_above_first':1}}},"
          + "'requirements':{'type':'minecraft:inverted','term':"
          + IS_ARMOR
          + "}}";

  private static final String DAMAGE_SET = ",'exclusive_set':'#minecraft:exclusive_set/damage'";

  private EnchantDefinitions() {}

  private static String def(int[] head, int[] cost, String slots, String supported, String extra) {
    return "{'description':{'translate':'enchantment.test'},'max_level':"
        + head[0]
        + ",'weight':"
        + head[1]
        + ",'anvil_cost':"
        + head[2]
        + ",'min_cost':{'base':"
        + cost[0]
        + ",'per_level_above_first':"
        + cost[1]
        + "},'max_cost':{'base':"
        + cost[2]
        + ",'per_level_above_first':"
        + cost[3]
        + "},'slots':"
        + slots
        + ",'supported_items':'#minecraft:"
        + supported
        + "'"
        + extra
        + "}";
  }

  /** Every definition by path of {@code minecraft}, in registry order. */
  static Map<String, String> all() {
    Map<String, String> defs = new LinkedHashMap<>();
    weapons(defs);
    armor(defs);
    tools(defs);
    return defs;
  }

  private static void weapons(Map<String, String> defs) {
    defs.put(
        "sharpness",
        def(
            new int[] {5, 10, 1},
            new int[] {1, 11, 21, 11},
            "['mainhand']",
            "enchantable/sharp_weapon",
            ",'primary_items':'#minecraft:enchantable/melee_weapon'"
                + DAMAGE_SET
                + ",'effects':{'minecraft:damage':[{'effect':"
                + LINEAR_DAMAGE
                + "}]}"));
    defs.put(
        "smite",
        def(
            new int[] {5, 5, 2},
            new int[] {5, 8, 25, 8},
            "['mainhand']",
            "enchantable/weapon",
            DAMAGE_SET));
    defs.put(
        "bane_of_arthropods",
        def(
            new int[] {5, 5, 2},
            new int[] {5, 8, 25, 8},
            "['mainhand']",
            "enchantable/weapon",
            DAMAGE_SET));
    defs.put(
        "looting",
        def(
            new int[] {3, 2, 4},
            new int[] {15, 9, 65, 9},
            "['mainhand']",
            "enchantable/weapon",
            ""));
    defs.put(
        "fire_aspect",
        def(
            new int[] {2, 2, 4},
            new int[] {10, 20, 60, 20},
            "['mainhand']",
            "enchantable/melee_weapon",
            ""));
    defs.put(
        "infinity",
        def(
            new int[] {1, 1, 8},
            new int[] {20, 0, 50, 0},
            "['mainhand']",
            "enchantable/bow",
            ",'exclusive_set':'#minecraft:exclusive_set/bow'"));
  }

  private static void armor(Map<String, String> defs) {
    defs.put(
        "protection",
        def(
            new int[] {4, 10, 1},
            new int[] {1, 11, 12, 11},
            "['armor']",
            "enchantable/armor",
            ",'exclusive_set':'#minecraft:exclusive_set/armor'"
                + ",'effects':{'minecraft:damage_protection':[{'effect':{'type':'minecraft:add',"
                + "'value':{'type':'minecraft:linear','base':1.0,'per_level_above_first':1.0}},"
                + "'requirements':"
                + NOT_BYPASSING
                + "}]}"));
    defs.put(
        "fire_protection",
        def(
            new int[] {4, 5, 2},
            new int[] {10, 8, 18, 8},
            "['armor']",
            "enchantable/armor",
            ",'exclusive_set':'#minecraft:exclusive_set/armor'"));
    defs.put(
        "binding_curse",
        def(
            new int[] {1, 1, 8},
            new int[] {25, 0, 50, 0},
            "['armor']",
            "enchantable/equippable",
            ",'effects':{'minecraft:prevent_armor_change':{}}"));
  }

  private static void tools(Map<String, String> defs) {
    defs.put(
        "unbreaking",
        def(
            new int[] {3, 5, 2},
            new int[] {5, 8, 55, 8},
            "['any']",
            "enchantable/durability",
            ",'effects':{'minecraft:item_damage':["
                + ARMOR_UNBREAKING
                + ","
                + OTHER_UNBREAKING
                + "]}"));
    defs.put(
        "mending",
        def(
            new int[] {1, 2, 4},
            new int[] {25, 25, 75, 25},
            "['any']",
            "enchantable/durability",
            ",'effects':{'minecraft:repair_with_xp':[{'effect':{'type':'minecraft:multiply',"
                + "'factor':2.0}}]}"));
    defs.put(
        "fortune",
        def(
            new int[] {3, 2, 4},
            new int[] {15, 9, 65, 9},
            "['mainhand']",
            "enchantable/mining_loot",
            ",'exclusive_set':'#minecraft:exclusive_set/mining'"));
    defs.put(
        "silk_touch",
        def(
            new int[] {1, 1, 8},
            new int[] {15, 0, 65, 0},
            "['mainhand']",
            "enchantable/mining_loot",
            ",'exclusive_set':'#minecraft:exclusive_set/mining'"));
    defs.put(
        "efficiency",
        def(
            new int[] {5, 10, 1},
            new int[] {1, 10, 51, 10},
            "['mainhand']",
            "enchantable/mining",
            ",'effects':{'minecraft:attributes':[{'id':'minecraft:enchantment.efficiency',"
                + "'attribute':'minecraft:mining_efficiency','operation':'add_value',"
                + "'amount':{'type':'minecraft:levels_squared','added':1.0}}]}"));
    tridents(defs);
  }

  private static void tridents(Map<String, String> defs) {
    defs.put(
        "riptide",
        def(
            new int[] {3, 2, 4},
            new int[] {17, 7, 50, 0},
            "['hand']",
            "enchantable/trident",
            ",'exclusive_set':'#minecraft:exclusive_set/riptide'"));
    defs.put(
        "loyalty",
        def(
            new int[] {3, 5, 2},
            new int[] {12, 7, 50, 0},
            "['mainhand']",
            "enchantable/trident",
            ""));
    defs.put(
        "channeling",
        def(
            new int[] {1, 1, 8},
            new int[] {25, 0, 50, 0},
            "['mainhand']",
            "enchantable/trident",
            ""));
  }
}
