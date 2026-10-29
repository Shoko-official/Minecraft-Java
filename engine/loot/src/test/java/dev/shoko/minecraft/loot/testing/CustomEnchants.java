package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCodec;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loot data whose enchantment catalog holds only the given made-up enchantments, all in the {@code
 * in_enchanting_table} tag, so that a test controls the candidates of the table exactly.
 */
public final class CustomEnchants {

  private final Map<String, String> definitions = new LinkedHashMap<>();

  /**
   * Adds an enchantment of one level, accepted by every item with {@code enchantable/durability},
   * with the same minimum and maximum cost.
   */
  public CustomEnchants window(String name, int weight, int minCost, int maxCost) {
    return window(name, weight, 1, minCost, maxCost);
  }

  /** Adds an enchantment with this maximum level; its costs do not depend on the level. */
  public CustomEnchants window(String name, int weight, int maxLevel, int minCost, int maxCost) {
    definitions.put(
        name,
        "{'description':{'translate':'enchantment.test'},'max_level':"
            + maxLevel
            + ",'weight':"
            + weight
            + ",'anvil_cost':1,'min_cost':{'base':"
            + minCost
            + ",'per_level_above_first':0},'max_cost':{'base':"
            + maxCost
            + ",'per_level_above_first':0},'slots':['mainhand'],"
            + "'supported_items':'#minecraft:enchantable/durability'}");
    return this;
  }

  /** The data: the standard tables, predicates and items, and only these enchantments. */
  public LootData data() {
    TestAccess tagged =
        EnchantFixtures.tags().withTag("enchantment", "in_enchanting_table", names());
    Map<Identifier, EnchantmentDefinition> decoded = new TreeMap<>();
    definitions.forEach(
        (name, json) -> {
          Identifier id = Identifier.minecraft(name);
          LootDecoding<EnchantmentDefinition> result =
              EnchantmentCodec.decode(id, Json.parse(json), tagged);
          if (!(result instanceof LootDecoding.Success<EnchantmentDefinition> ok)) {
            throw new IllegalStateException(name + " does not decode: " + result.findings());
          }
          decoded.put(id, ok.value());
        });
    LinkedHashMap<Identifier, EnchantmentDefinition> ordered = new LinkedHashMap<>(decoded);
    EnchantmentCatalog catalog = new EnchantmentCatalog(ordered, new LinkedHashMap<>());
    LootData base = Ctx.data();
    return new LootData(
        base.tables(),
        base.predicates(),
        base.numbers(),
        catalog,
        base.items(),
        base.recipes(),
        tagged.withEnchantments(ordered));
  }

  private String[] names() {
    return definitions.keySet().toArray(new String[0]);
  }
}
