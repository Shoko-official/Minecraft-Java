package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes item modifiers from their JSON form (LT-R3, LT-R38 to LT-R47). The discriminant is {@code
 * type}; every type accepts {@code condition}. The 21 types of LT-R47 decode to {@code Deferred}
 * and add one {@code NOT_IMPLEMENTED_MODIFIER} warning per type per call. The field of the villager
 * trade modifier {@code set_random_dyes} is {@code number_of_dyes} (LT-R47, the data prevail over
 * the documentation).
 */
public final class ModifierCodec {

  private ModifierCodec() {}

  /**
   * Decodes one modifier.
   *
   * @param node a JSON object
   * @param access the loaded registries and tags
   * @param items the loaded items, for the components of {@code set_components}
   * @return the modifier; {@code UNKNOWN_TYPE} for a type outside the 42, {@code
   *     UNKNOWN_ENCHANTMENT} when a named enchantment is not loaded
   */
  public static LootDecoding<ItemModifier> decode(
      JsonNode node, RegistryAccess access, ItemCatalog items) {
    Findings findings = new Findings();
    ItemModifier modifier = new ModifierReader(access, items, findings).modifier(node);
    return findings.result(modifier);
  }

  /**
   * Decodes the value of a {@code modifier} key: one object or a list, applied in order (LT-R4a,
   * LT-R4b).
   *
   * @param node an object or an array
   * @param access the loaded registries and tags
   * @param items the loaded items
   * @return the modifiers in file order
   */
  public static LootDecoding<List<ItemModifier>> decodeList(
      JsonNode node, RegistryAccess access, ItemCatalog items) {
    Findings findings = new Findings();
    List<ItemModifier> modifiers = new ModifierReader(access, items, findings).list(node);
    return findings.result(List.copyOf(modifiers));
  }

  /** Reads a list of modifiers with a reader that shares its warnings. */
  static List<ItemModifier> readList(ModifierReader reader, JsonNode node) {
    List<ItemModifier> modifiers = new ArrayList<>();
    if (node instanceof JsonArray array) {
      for (JsonNode element : array.elements()) {
        modifiers.add(reader.modifier(element));
      }
    } else {
      modifiers.add(reader.modifier(node));
    }
    return modifiers;
  }
}
