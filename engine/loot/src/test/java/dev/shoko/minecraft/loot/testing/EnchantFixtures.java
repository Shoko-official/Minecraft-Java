package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCodec;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.EnchantmentProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.TreeMap;

/**
 * A small made-up enchantment universe with the field shapes of the enchantment spec (EN-R2,
 * EN-R12, EN-R17, EN-R34), not copies of the reference data: the numbers are chosen so that
 * expected values are easy to compute by hand.
 */
public final class EnchantFixtures {

  private static EnchantmentCatalog cached;
  private static TestAccess cachedAccess;

  private EnchantFixtures() {}

  /** The JSON text of each enchantment, by path of {@code minecraft}. */
  public static Map<String, String> definitions() {
    return EnchantDefinitions.all();
  }

  /** The base access of {@link TestAccess#standard()} plus the enchantment tags. */
  private static TestAccess tagged() {
    return TestAccess.standard()
        .withTag(
            "enchantment",
            "non_treasure",
            "sharpness",
            "smite",
            "bane_of_arthropods",
            "protection",
            "fire_protection",
            "unbreaking",
            "infinity",
            "fortune",
            "silk_touch",
            "looting",
            "fire_aspect",
            "efficiency",
            "riptide",
            "loyalty",
            "channeling")
        .withTag("enchantment", "treasure", "mending", "binding_curse")
        .withTag("enchantment", "curse", "binding_curse")
        .withTag(
            "enchantment",
            "in_enchanting_table",
            "sharpness",
            "smite",
            "bane_of_arthropods",
            "protection",
            "fire_protection",
            "unbreaking",
            "infinity",
            "fortune",
            "silk_touch",
            "looting",
            "fire_aspect",
            "efficiency",
            "riptide",
            "loyalty",
            "channeling")
        .withTag("enchantment", "smelts_loot", "fire_aspect")
        .withTag("enchantment", "exclusive_set/damage", "sharpness", "smite", "bane_of_arthropods")
        .withTag("enchantment", "exclusive_set/armor", "protection", "fire_protection")
        .withTag("enchantment", "exclusive_set/bow", "infinity", "mending")
        .withTag("enchantment", "exclusive_set/mining", "fortune", "silk_touch")
        .withTag("enchantment", "exclusive_set/riptide", "loyalty", "channeling");
  }

  /** The enchantment tag contents, for the tests that cite them. */
  public static TestAccess tags() {
    return tagged();
  }

  /** The access with the enchantment registry of the catalog. */
  public static synchronized TestAccess access() {
    if (cachedAccess == null) {
      cachedAccess = tagged().withEnchantments(catalog().definitions());
    }
    return cachedAccess;
  }

  /** The made-up catalog: definitions decoded from JSON in identifier order, and one provider. */
  public static synchronized EnchantmentCatalog catalog() {
    if (cached == null) {
      TestAccess base = tagged();
      Map<Identifier, EnchantmentDefinition> decoded = new TreeMap<>();
      definitions()
          .forEach(
              (name, json) -> {
                Identifier id = Identifier.minecraft(name);
                LootDecoding<EnchantmentDefinition> result =
                    EnchantmentCodec.decode(id, Json.parse(json), base);
                if (!(result instanceof LootDecoding.Success<EnchantmentDefinition> ok)) {
                  throw new IllegalStateException(name + " does not decode: " + result.findings());
                }
                decoded.put(id, ok.value());
              });
      SequencedMap<Identifier, EnchantmentProvider> providers = new LinkedHashMap<>();
      providers.put(
          Identifier.minecraft("test_single"),
          new EnchantmentProvider.Single(Identifier.minecraft("silk_touch"), 1));
      cached = new EnchantmentCatalog(new LinkedHashMap<>(decoded), providers);
    }
    return cached;
  }
}
