package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The loaded tables of the {@code loot_table} registry (LT-R1). Immutable.
 *
 * @param tables the tables by identifier, in identifier order, unmodifiable
 */
public record LootTables(SequencedMap<Identifier, LootTable> tables) {

  /**
   * Finds a table.
   *
   * @param id a table identifier such as {@code minecraft:blocks/stone}
   * @return the table, empty when it is not loaded
   */
  public Optional<LootTable> find(Identifier id) {
    return Skeleton.throwNotImplemented();
  }
}
