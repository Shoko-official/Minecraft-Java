package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.Findings;
import java.util.Map;
import java.util.Optional;

/**
 * The component predicates of an item predicate (LT-R58). The enchantment predicates are typed;
 * every other key means that the component of the same name is present (hypothesis of probe PL15).
 */
final class ComponentPredicates {

  private static final Identifier ENCHANTMENTS = Identifier.minecraft("enchantments");
  private static final Identifier STORED_ENCHANTMENTS = Identifier.minecraft("stored_enchantments");
  private static final Identifier ENCHANTMENT_REGISTRY = Identifier.minecraft("enchantment");

  private ComponentPredicates() {}

  static boolean matches(Identifier key, JsonNode value, ItemStack stack, RegistryAccess access) {
    Optional<ComponentValue> component = stack.component(key);
    if (component.isEmpty()) {
      return false;
    }
    boolean enchantments = key.equals(ENCHANTMENTS) || key.equals(STORED_ENCHANTMENTS);
    if (enchantments && value instanceof JsonArray entries) {
      return enchantmentsMatch(component.get(), entries, access);
    }
    return true;
  }

  private static boolean enchantmentsMatch(
      ComponentValue component, JsonArray entries, RegistryAccess access) {
    if (!(EnchantmentLevels.from(component) instanceof Decoding.Success<EnchantmentLevels> ok)) {
      return false;
    }
    Map<Identifier, Integer> levels = ok.value().levels();
    for (JsonNode entry : entries.elements()) {
      if (!anyEnchantmentMatches(entry, levels, access)) {
        return false;
      }
    }
    return true;
  }

  private static boolean anyEnchantmentMatches(
      JsonNode entry, Map<Identifier, Integer> levels, RegistryAccess access) {
    if (!(entry instanceof JsonObject object)) {
      return false;
    }
    Findings findings = new Findings();
    IdOrTagSet wanted = MatchReaders.unrestricted(ENCHANTMENT_REGISTRY);
    JsonNode wantedNode = object.members().get("enchantments");
    if (wantedNode != null) {
      wanted = findings.take(MatchCodecs.idOrTagSet(wantedNode, ENCHANTMENT_REGISTRY, access));
    }
    NumberRange range = MatchReaders.anyNumber();
    JsonNode rangeNode = object.members().get("levels");
    if (rangeNode != null) {
      range = findings.take(MatchCodecs.range(rangeNode));
    }
    if (findings.failed()) {
      return false;
    }
    for (Map.Entry<Identifier, Integer> present : levels.entrySet()) {
      if (wanted.contains(present.getKey(), access) && range.contains(present.getValue())) {
        return true;
      }
    }
    return false;
  }
}
