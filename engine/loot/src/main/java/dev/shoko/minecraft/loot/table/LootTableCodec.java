package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.encode.TableJson;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import java.util.List;

/**
 * Decodes a table file (LT-R1 to LT-R10). The format of 26.3 only: {@code type} is the
 * discriminant, {@code condition} and {@code modifier} are singular (LT-R3). The decoded table
 * re-encodes to the same JSON tree, key order aside (plan of tests L1).
 */
public final class LootTableCodec {

  private LootTableCodec() {}

  /**
   * Decodes one table.
   *
   * @param node the file content
   * @param access the loaded registries and tags
   * @param items the loaded items, to resolve entries of type item
   * @return the table; an unknown entry, condition, modifier or provider type is {@code
   *     UNKNOWN_TYPE}, an unknown item {@code INVALID_IDENTIFIER}, a {@code type} outside the 31
   *     context types {@code UNKNOWN_TYPE}.
   */
  public static LootDecoding<LootTable> decode(
      JsonNode node, RegistryAccess access, ItemCatalog items) {
    Findings findings = new Findings();
    LootTable table = new TableReader(access, items, findings).table(node);
    return findings.result(table);
  }

  /**
   * Re-encodes a table to JSON text, for the round trip test (plan of tests L1). Keys in a fixed
   * order; defaults that were absent are not written.
   *
   * @param table a decoded table
   * @return compact JSON
   */
  public static String encode(LootTable table) {
    return TableJson.encode(table);
  }

  /**
   * Checks the references of a decoded table once all tables and predicates are known: a named
   * predicate that does not exist is {@code UNRESOLVED_REFERENCE} (LT-R61b), a {@code loot_table}
   * entry that points at the table itself is {@code SELF_REFERENCE} (LT-R23), a named table that is
   * not loaded is {@code UNRESOLVED_REFERENCE}. The conditions that read a parameter the context
   * type does not provide give {@code UNAVAILABLE_PARAMETER} warnings (LT-R15).
   *
   * @param self the identifier of the table, to detect self references
   * @param table the decoded table
   * @param known the tables decoded so far
   * @param predicates the named predicates
   * @return the findings in the order of the table
   */
  public static List<LootFinding> checkReferences(
      Identifier self, LootTable table, LootTables known, NamedPredicates predicates) {
    return new TableReferences(self, known, predicates).check(table);
  }
}
