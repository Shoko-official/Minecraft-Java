package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemReportEntry;
import dev.shoko.minecraft.item.registry.ItemReportSource;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small made-up item catalog with the shapes of the item spec, not copies of the reference data:
 * plain blocks and materials, damageable tools with {@code enchantable}, books and a potion.
 */
public final class LootItems {

  private static final List<String> COMPONENT_NAMES =
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

  private static final Map<String, Map<String, String>> ITEMS = new LinkedHashMap<>();
  private static final Map<String, Integer> STACK_SIZES = new LinkedHashMap<>();

  private static ItemCatalog cached;

  static {
    for (String path :
        List.of(
            "air",
            "stone",
            "cobblestone",
            "dirt",
            "stick",
            "apple",
            "diamond",
            "emerald",
            "leather",
            "beef",
            "iron_ingot",
            "book",
            "decorated_pot",
            "oak_leaves",
            "oak_sapling")) {
      define(path, 64);
    }
    define("enchanted_book", 1, "stored_enchantments", "{}");
    define("potion", 1, "potion_contents", "{}");
    define("shears", 1, "max_damage", "238", "damage", "0");
    tool("diamond_sword", 1561, 10, "diamond_tool_materials");
    tool("diamond_axe", 1561, 10, "diamond_tool_materials");
    tool("iron_pickaxe", 250, 14, "iron_tool_materials");
    tool("bow", 384, 1, "bow_materials");
    tool("trident", 250, 1, "trident_materials");
    define(
        "diamond_chestplate",
        1,
        "max_damage",
        "528",
        "damage",
        "0",
        "enchantable",
        "{'value':10}",
        "repairable",
        "{'items':'#minecraft:diamond_tool_materials'}",
        "equippable",
        "{'slot':'chest','asset_id':'minecraft:diamond','equip_sound':'minecraft:a.b'}");
  }

  private LootItems() {}

  private static void tool(String path, int maxDamage, int enchantability, String repairTag) {
    define(
        path,
        1,
        "max_damage",
        Integer.toString(maxDamage),
        "damage",
        "0",
        "enchantable",
        "{'value':" + enchantability + "}",
        "repairable",
        "{'items':'#minecraft:" + repairTag + "'}");
  }

  private static void define(String path, int stackSize, String... componentsAndJson) {
    Map<String, String> extras = new LinkedHashMap<>();
    for (int i = 0; i < componentsAndJson.length; i += 2) {
      extras.put(componentsAndJson[i], componentsAndJson[i + 1]);
    }
    ITEMS.put(path, extras);
    STACK_SIZES.put(path, stackSize);
  }

  private static ItemReportEntry entry(String path, int stackSize, Map<String, String> extras) {
    Map<String, String> all = new LinkedHashMap<>();
    all.put("attack_animation", "{}");
    all.put("attribute_modifiers", "[]");
    all.put("break_sound", "'minecraft:entity.item.break'");
    all.put("enchantments", "{}");
    all.put("interact_animation", "{}");
    all.put("item_model", "'minecraft:" + path + "'");
    all.put("item_name", "{'translate':'item.minecraft." + path + "'}");
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

  private record Source(List<ItemReportEntry> items) implements ItemReportSource {
    @Override
    public List<Identifier> componentTypeIds() {
      return COMPONENT_NAMES.stream().map(Identifier::minecraft).toList();
    }

    @Override
    public Identifier defaultItem() {
      return Identifier.minecraft("air");
    }
  }

  /** The identifiers of the made-up items, in registry order. */
  public static List<Identifier> itemIds() {
    return ITEMS.keySet().stream().map(Identifier::minecraft).toList();
  }

  /** The catalog of the made-up items, loaded once. */
  public static synchronized ItemCatalog catalog() {
    if (cached == null) {
      List<ItemReportEntry> entries = new ArrayList<>();
      ITEMS.forEach((path, extras) -> entries.add(entry(path, STACK_SIZES.get(path), extras)));
      Decoding<ItemCatalog> decoding = ItemCatalog.load(new Source(List.copyOf(entries)));
      if (!(decoding instanceof Decoding.Success<ItemCatalog> success)) {
        throw new IllegalStateException("the made-up items do not load: " + decoding.findings());
      }
      cached = success.value();
    }
    return cached;
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

  /** The stack with the given enchantments in the {@code enchantments} component. */
  public static ItemStack enchanted(ItemStack stack, Map<String, Integer> levels) {
    return stack.withComponent(WellKnownComponents.ENCHANTMENTS, levelsValue(levels));
  }

  /** The stack with the given enchantments in the {@code stored_enchantments} component. */
  public static ItemStack storing(ItemStack stack, Map<String, Integer> levels) {
    return stack.withComponent(WellKnownComponents.STORED_ENCHANTMENTS, levelsValue(levels));
  }

  private static ComponentValue levelsValue(Map<String, Integer> levels) {
    Map<Identifier, Integer> byId = new LinkedHashMap<>();
    levels.forEach((name, level) -> byId.put(Identifier.parse(name), level));
    return new EnchantmentLevels(new LinkedHashMap<>(byId)).toValue();
  }

  /** The levels held in a component, empty when the component is absent. */
  public static Map<Identifier, Integer> levelsIn(ItemStack stack, Identifier component) {
    return stack
        .component(component)
        .map(
            value ->
                EnchantmentLevels.from(value) instanceof Decoding.Success<EnchantmentLevels> ok
                    ? ok.value().levels()
                    : Map.<Identifier, Integer>of())
        .orElse(Map.of());
  }

  /** The levels of the {@code enchantments} component. */
  public static Map<Identifier, Integer> enchantmentsOf(ItemStack stack) {
    return levelsIn(stack, WellKnownComponents.ENCHANTMENTS);
  }

  /** The levels of the {@code stored_enchantments} component. */
  public static Map<Identifier, Integer> storedOf(ItemStack stack) {
    return levelsIn(stack, WellKnownComponents.STORED_ENCHANTMENTS);
  }

  /** The stack with its {@code damage} component set. */
  public static ItemStack damaged(ItemStack stack, int damage) {
    return stack.withComponent(
        WellKnownComponents.DAMAGE,
        new ComponentValue(new dev.shoko.minecraft.core.nbt.IntTag(damage)));
  }

  /** The stack with its {@code repair_cost} component set. */
  public static ItemStack withRepairCost(ItemStack stack, int cost) {
    return stack.withComponent(
        Identifier.minecraft("repair_cost"),
        new ComponentValue(new dev.shoko.minecraft.core.nbt.IntTag(cost)));
  }

  /** The {@code repair_cost} of a stack, 0 when absent. */
  public static int repairCostOf(ItemStack stack) {
    return stack
        .component(Identifier.minecraft("repair_cost"))
        .map(ComponentValue::tag)
        .filter(dev.shoko.minecraft.core.nbt.IntTag.class::isInstance)
        .map(tag -> ((dev.shoko.minecraft.core.nbt.IntTag) tag).value())
        .orElse(0);
  }
}
