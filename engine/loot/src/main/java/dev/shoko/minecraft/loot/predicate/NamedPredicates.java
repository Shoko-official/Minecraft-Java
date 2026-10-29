package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The named predicates of the loaded data: the elements of the "predicate" registry, 3 in the
 * vanilla data (LT-R61a). Immutable.
 *
 * @param predicates the predicates by identifier, in identifier order, unmodifiable
 */
public record NamedPredicates(SequencedMap<Identifier, LootCondition> predicates) {

  /**
   * Finds a predicate.
   *
   * @param id an identifier such as {@code minecraft:tool/can_silk_touch}
   * @return the condition, empty when it is not loaded (a reference to it is an error, LT-R61b)
   */
  public Optional<LootCondition> find(Identifier id) {
    return Optional.ofNullable(predicates.get(id));
  }
}
