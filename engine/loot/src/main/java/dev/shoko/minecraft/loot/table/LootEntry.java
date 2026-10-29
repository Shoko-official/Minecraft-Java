package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * An entry of a pool (LT-R7 to LT-R9): the fields every type shares and the type itself.
 *
 * @param conditions the entry conditions, all must hold (LT-R19c); empty when the key is absent
 * @param weight the weight, 1 when absent (LT-R8, provisional); meaningful for leaf entries only
 * @param quality the quality, 0 when absent (LT-R8, provisional)
 * @param modifiers the modifiers applied to what the entry produces (LT-R19d)
 * @param kind the type and its own fields
 */
public record LootEntry(
    List<LootCondition> conditions,
    int weight,
    int quality,
    List<ItemModifier> modifiers,
    Kind kind) {

  /** The nine registered entry types (registry {@code loot_pool_entry_type}, LT-R7). */
  public sealed interface Kind
      permits Kind.Empty,
          Kind.Item,
          Kind.LootTableRef,
          Kind.Dynamic,
          Kind.Tag,
          Kind.Slots,
          Kind.Alternatives,
          Kind.Sequence,
          Kind.Group {

    /** {@code empty}: counts in the total weight and produces nothing (LT-R24). */
    record Empty() implements Kind {}

    /**
     * {@code item}: one stack of count 1 with the default components (LT-R24).
     *
     * @param item the item, resolved against the item catalog at load time
     */
    record Item(ItemType item) implements Kind {}

    /**
     * {@code loot_table}: evaluates another table with the same context and source (LT-R23).
     *
     * @param table the table, by identifier or inline
     */
    record LootTableRef(TableRef table) implements Kind {}

    /**
     * {@code dynamic}: stacks the context emitter provides under a name (LT-R24), such as {@code
     * minecraft:sherds} or {@code minecraft:contents}.
     *
     * @param name the source name
     */
    record Dynamic(Identifier name) implements Kind {}

    /**
     * {@code tag}: the items of an item tag (LT-R22).
     *
     * @param tag the tag identifier, without the {@code #}
     * @param expand {@code true}: one child entry per item; {@code false}: one entry producing one
     *     stack per item. Absent reads as {@code false} (provisional, probe PL8)
     */
    record Tag(Identifier tag, boolean expand) implements Kind {}

    /**
     * {@code slots}: stacks read from the slots of a source (LT-R71). No data uses it.
     *
     * @param slotSource the slot source as read
     */
    record Slots(JsonNode slotSource) implements Kind {}

    /**
     * {@code alternatives}: only the first child whose condition holds contributes (LT-R21).
     *
     * @param children the children in file order
     */
    record Alternatives(List<LootEntry> children) implements Kind {}

    /**
     * {@code sequence}: the children contribute in order until the first whose condition fails
     * (LT-R21).
     *
     * @param children the children in file order
     */
    record Sequence(List<LootEntry> children) implements Kind {}

    /**
     * {@code group}: all the children contribute (LT-R21).
     *
     * @param children the children in file order
     */
    record Group(List<LootEntry> children) implements Kind {}
  }

  /** The table a {@code loot_table} entry points at (LT-R9, LT-R10). */
  public sealed interface TableRef permits TableRef.Named, TableRef.Inline {

    /**
     * A table of the registry.
     *
     * @param id the table identifier
     */
    record Named(Identifier id) implements TableRef {}

    /**
     * A table written in place (LT-R10). Its entries are not entries of the parent pool.
     *
     * @param table the decoded table
     */
    record Inline(LootTable table) implements TableRef {}
  }
}
