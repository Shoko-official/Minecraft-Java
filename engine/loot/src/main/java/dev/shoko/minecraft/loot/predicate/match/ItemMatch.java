package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Map;
import java.util.SequencedMap;

/**
 * The item predicate of {@code match_tool.predicate}, {@code filtered.item_filter} and the
 * ingredients of brewing (LT-R58). The component predicates keep their JSON: the registry {@code
 * data_component_predicate_type} has 15 types, but the data also uses {@code minecraft:map_id} with
 * {@code {}}, which is not one of them (LT-R58, probes PL15 and PI11).
 *
 * @param items the accepted items, unrestricted when the key is absent
 * @param count the accepted count; both bounds infinite when absent
 * @param componentPredicates the {@code predicates} table, component predicate type to value
 * @param exactComponents the {@code components} table, component to exact value
 */
public record ItemMatch(
    IdOrTagSet items,
    NumberRange count,
    SequencedMap<Identifier, JsonNode> componentPredicates,
    SequencedMap<Identifier, JsonNode> exactComponents) {

  /**
   * Tells whether a stack matches. An empty stack matches nothing. A component predicate holds when
   * the component is present (hypothesis of LT-R58) and, for the typed ones such as {@code
   * minecraft:enchantments}, when its levels satisfy the value.
   *
   * @param stack a stack
   * @param access the loaded registries and tags
   * @return {@code true} when it matches
   */
  public boolean matches(ItemStack stack, RegistryAccess access) {
    if (stack.isEmpty()
        || !items.contains(stack.type().id(), access)
        || !count.contains(stack.count())) {
      return false;
    }
    for (Map.Entry<Identifier, JsonNode> entry : componentPredicates.entrySet()) {
      if (!ComponentPredicates.matches(entry.getKey(), entry.getValue(), stack, access)) {
        return false;
      }
    }
    for (Identifier component : exactComponents.keySet()) {
      if (stack.component(component).isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
