package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The loot table a container still has to unpack, if any (IC-R32). Unpacking is described in the
 * loot spec and done by {@code loot}; this is only the data that block entities store.
 */
public sealed interface LootBinding permits LootBinding.None, LootBinding.Bound {

  /** No table: a materialized container, or one that never had loot. */
  record None() implements LootBinding {}

  /**
   * A table to unpack on first opening.
   *
   * @param table the table identifier ({@code LootTable}, a String)
   * @param seeded whether {@code LootTableSeed} is present
   * @param seed the seed ({@code LootTableSeed}, a Long), meaningful only when {@code seeded}
   */
  record Bound(Identifier table, boolean seeded, long seed) implements LootBinding {}
}
