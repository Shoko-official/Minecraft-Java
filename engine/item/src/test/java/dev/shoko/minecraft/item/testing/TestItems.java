package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemReportEntry;
import dev.shoko.minecraft.item.registry.ItemReportSource;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A small fake item report: made-up items whose components have the shapes of the specs (IC-R3,
 * IC-R12, V-I1 to V-I6), not copies of the reference data. Component types are the registry names
 * of the specs, in alphabetical order as in the reference registry.
 */
public final class TestItems {

  public static final List<String> COMPONENT_NAMES =
      List.of(
          "attack_animation",
          "attribute_modifiers",
          "banner_patterns",
          "break_sound",
          "brewing_fuel",
          "compostable",
          "consumable",
          "container",
          "cooking_fuel",
          "custom_data",
          "custom_name",
          "damage",
          "enchantable",
          "enchantments",
          "equippable",
          "food",
          "interact_animation",
          "item_model",
          "item_name",
          "lore",
          "max_damage",
          "max_stack_size",
          "potion_contents",
          "provides_trim_material",
          "rarity",
          "repair_cost",
          "repairable",
          "stored_enchantments",
          "tool",
          "tooltip_display",
          "trim",
          "unbreakable",
          "use_effects",
          "use_remainder",
          "villager_food",
          "weapon");

  public static final List<Identifier> COMPONENT_IDS =
      COMPONENT_NAMES.stream().map(Identifier::minecraft).toList();

  /** The thirteen components every prototype has (IC-R3). */
  public static final List<String> UNIVERSAL =
      List.of(
          "attack_animation",
          "attribute_modifiers",
          "break_sound",
          "enchantments",
          "interact_animation",
          "item_model",
          "item_name",
          "lore",
          "max_stack_size",
          "rarity",
          "repair_cost",
          "tooltip_display",
          "use_effects");

  private static final String COAL_FUEL =
      "{'burn_time':'minecraft:cooking/time_coal','speed_multiplier':'minecraft:cooking/speed_default'}";

  private static final String BLAZE_FUEL =
      "{'speed_multiplier':'minecraft:brewing/speed_default','uses':'minecraft:brewing/uses_default'}";

  private static final String PICKAXE_TOOL =
      "{'rules':[{'blocks':'#minecraft:incorrect_for_stone_tool','correct_for_drops':false},"
          + "{'blocks':'#minecraft:mineable/pickaxe','correct_for_drops':true,'speed':4.0}]}";

  private static final String PICKAXE_MODIFIERS =
      "[{'type':'minecraft:attack_damage','amount':2.0,'id':'minecraft:base_attack_damage',"
          + "'operation':'add_value','slot':'mainhand'},"
          + "{'type':'minecraft:attack_speed','amount':-2.799999952316284,"
          + "'id':'minecraft:base_attack_speed','operation':'add_value','slot':'mainhand'}]";

  /** Item path to its extra components (component path to JSON text with {@code '} for quotes). */
  private static final Map<String, Map<String, String>> ITEMS = new LinkedHashMap<>();

  private static final Map<String, Integer> STACK_SIZES = new LinkedHashMap<>();

  static {
    for (String path :
        List.of(
            "air",
            "stone",
            "cobblestone",
            "dirt",
            "oak_planks",
            "birch_planks",
            "oak_log",
            "stick",
            "diamond",
            "iron_ingot",
            "gold_ingot",
            "redstone",
            "gunpowder",
            "paper",
            "map",
            "nether_wart",
            "glass_bottle",
            "bowl",
            "stone_slab",
            "stone_stairs",
            "stone_bricks",
            "potato",
            "baked_potato",
            "netherite_upgrade_smithing_template",
            "bolt_armor_trim_smithing_template",
            "netherite_ingot")) {
      define(path, 64);
    }
    for (String path : List.of("bucket", "ender_pearl", "snowball")) {
      define(path, 16);
    }
    define("white_banner", 16, "banner_patterns", "[]");
    define("chest", 64, "container", "[]");
    define("coal", 64, "cooking_fuel", COAL_FUEL);
    define("blaze_powder", 64, "brewing_fuel", BLAZE_FUEL);
    define("apple", 64, "compostable", "{'layers':'minecraft:compostable/medium_high'}");
    define(
        "bread",
        64,
        "food",
        "{'nutrition':5,'saturation':6.0}",
        "consumable",
        "{}",
        "villager_food",
        "{'nutrition':4}");
    define("emerald", 64, "provides_trim_material", "'minecraft:emerald'");
    define("milk_bucket", 1, "use_remainder", "{'id':'minecraft:bucket'}");
    define("mushroom_stew", 1, "use_remainder", "{'id':'minecraft:bowl'}");
    define(
        "potion", 1, "use_remainder", "{'id':'minecraft:glass_bottle'}", "potion_contents", "{}");
    define("splash_potion", 1, "potion_contents", "{}");
    define("lingering_potion", 1, "potion_contents", "{}");
    define("iron_helmet", 1, "equippable", equippable("head"));
    define("iron_chestplate", 1, "equippable", equippable("chest"));
    define("iron_leggings", 1, "equippable", equippable("legs"));
    define("iron_boots", 1, "equippable", equippable("feet"));
    define("diamond_axe", 1, "max_damage", "1561", "damage", "0");
    define("netherite_axe", 1, "max_damage", "2031", "damage", "0");
    define(
        "stone_pickaxe",
        1,
        "max_damage",
        "131",
        "damage",
        "0",
        "enchantable",
        "{'value':5}",
        "repairable",
        "{'items':'#minecraft:stone_tool_materials'}",
        "tool",
        PICKAXE_TOOL,
        "weapon",
        "{'item_damage_per_attack':2}",
        "attribute_modifiers",
        PICKAXE_MODIFIERS);
  }

  private TestItems() {}

  private static String equippable(String slot) {
    return "{'slot':'"
        + slot
        + "','asset_id':'minecraft:iron','equip_sound':'minecraft:item.armor.equip_iron'}";
  }

  private static void define(String path, int stackSize, String... componentsAndJson) {
    Map<String, String> extras = new LinkedHashMap<>();
    for (int i = 0; i < componentsAndJson.length; i += 2) {
      extras.put(componentsAndJson[i], componentsAndJson[i + 1]);
    }
    ITEMS.put(path, extras);
    STACK_SIZES.put(path, stackSize);
  }

  /** The item report: the fake items in insertion order, air first. */
  public static Source source() {
    List<ItemReportEntry> entries = new ArrayList<>();
    ITEMS.forEach((path, extras) -> entries.add(entry(path, STACK_SIZES.get(path), extras)));
    return new Source(COMPONENT_IDS, Identifier.minecraft("air"), List.copyOf(entries));
  }

  /** The paths of the fake items in registry order. */
  public static List<String> itemPaths() {
    return List.copyOf(ITEMS.keySet());
  }

  /** One report entry with the thirteen universal components and the given extras. */
  public static ItemReportEntry entry(String path, int stackSize, Map<String, String> extras) {
    Map<String, String> all = new LinkedHashMap<>();
    all.put("attack_animation", "{}");
    all.put("attribute_modifiers", "[]");
    all.put("break_sound", "'minecraft:entity.item.break'");
    all.put("enchantments", "{}");
    all.put("interact_animation", "{}");
    all.put("item_model", "'minecraft:" + path + "'");
    all.put("item_name", "{'translate':'block.minecraft." + path + "'}");
    all.put("lore", "[]");
    all.put("max_stack_size", Integer.toString(stackSize));
    all.put("rarity", "'common'");
    all.put("repair_cost", "0");
    all.put("tooltip_display", "{}");
    all.put("use_effects", "{}");
    all.putAll(extras);
    StringBuilder text = new StringBuilder("{");
    all.forEach(
        (name, json) -> {
          if (text.length() > 1) {
            text.append(',');
          }
          text.append("'minecraft:").append(name).append("':").append(json);
        });
    return new ItemReportEntry(Identifier.minecraft(path), Json.parse(text.append('}').toString()));
  }

  public static ItemReportEntry entry(String path, int stackSize) {
    return entry(path, stackSize, Map.of());
  }

  /** A report whose parts a test can replace. */
  public record Source(
      List<Identifier> componentTypeIds, Identifier defaultItem, List<ItemReportEntry> items)
      implements ItemReportSource {

    public Source withItems(List<ItemReportEntry> replaced) {
      return new Source(componentTypeIds, defaultItem, replaced);
    }

    public Source withDefault(Identifier replaced) {
      return new Source(componentTypeIds, replaced, items);
    }

    public Source withComponentTypes(List<Identifier> replaced) {
      return new Source(replaced, defaultItem, items);
    }

    public Source plusItem(ItemReportEntry added) {
      return withItems(Stream.concat(items.stream(), Stream.of(added)).toList());
    }

    public Source replacing(String path, ItemReportEntry replacement) {
      return withItems(
          items.stream()
              .map(e -> e.id().equals(Identifier.minecraft(path)) ? replacement : e)
              .toList());
    }
  }

  private static ItemCatalog cached;

  /**
   * The catalog of the fake items, loaded once. A plain memo rather than a class initializer, so
   * that a failing load reports its own exception in every test instead of a linkage error.
   */
  public static synchronized ItemCatalog catalog() {
    if (cached == null) {
      Decoding<ItemCatalog> decoding = ItemCatalog.load(source());
      cached = Findings.value(decoding);
    }
    return cached;
  }

  public static ComponentCodec componentCodec() {
    return ComponentCodec.of(catalog().components());
  }

  public static StackCodec stackCodec() {
    return StackCodec.of(catalog());
  }

  public static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  public static ItemType type(String path) {
    return catalog().find(id(path)).orElseThrow();
  }

  public static ItemStack stack(String path, int count) {
    return ItemStack.of(type(path), count);
  }

  public static ItemStack stack(String path) {
    return stack(path, 1);
  }

  /** A component value decoded from JSON text with {@code '} for quotes. */
  public static ComponentValue value(String component, String relaxedJson) {
    return Findings.value(componentCodec().valueFromJson(id(component), Json.parse(relaxedJson)));
  }

  /** A stack of the item with one more component, given as JSON text. */
  public static ItemStack stack(String path, int count, String component, String relaxedJson) {
    return stack(path, count).withComponent(id(component), value(component, relaxedJson));
  }

  public static ComponentPatch patchOf(String component, String relaxedJson) {
    return ComponentPatch.empty().withSet(id(component), value(component, relaxedJson));
  }
}
