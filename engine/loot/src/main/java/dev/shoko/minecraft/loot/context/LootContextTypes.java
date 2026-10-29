package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The registry of context types (LT-R13): the 31 identifiers, in the order of their numeric ids,
 * with the provisional parameter sets of LT-R14b. A table whose {@code type} is not here is refused
 * at load time ({@code UNKNOWN_TYPE}); an absent {@code type} reads as {@code minecraft:generic}
 * (LT-R2, provisional).
 */
public final class LootContextTypes {

  private static final Set<ContextKey> ALL_DROP_KEYS =
      keys(
          ContextKey.ORIGIN,
          ContextKey.THIS_ENTITY,
          ContextKey.ATTACKER,
          ContextKey.DIRECT_ATTACKER,
          ContextKey.ATTACKING_PLAYER,
          ContextKey.DAMAGE_SOURCE,
          ContextKey.TOOL,
          ContextKey.BLOCK_STATE,
          ContextKey.BLOCK_ENTITY,
          ContextKey.EXPLOSION_RADIUS);

  private static final Set<ContextKey> ENCHANTED_KEYS =
      keys(
          ContextKey.ENCHANTMENT_LEVEL,
          ContextKey.ENCHANTMENT_ACTIVE,
          ContextKey.TOOL,
          ContextKey.THIS_ENTITY,
          ContextKey.ORIGIN);

  private LootContextTypes() {}

  /**
   * Returns the 31 types.
   *
   * @return the types ordered by {@link LootContextType#rawId()}, unmodifiable
   */
  public static List<LootContextType> all() {
    return ALL;
  }

  /**
   * Finds a type.
   *
   * @param id a context type identifier
   * @return the type, empty when it is not one of the 31
   */
  public static Optional<LootContextType> find(Identifier id) {
    return ALL.stream().filter(type -> type.id().equals(id)).findFirst();
  }

  /**
   * Returns the type of a table that has no {@code type} (LT-R2, provisional).
   *
   * @return {@code minecraft:generic}
   */
  public static LootContextType generic() {
    return ALL.get(1);
  }

  private static Set<ContextKey> keys(ContextKey... keys) {
    Set<ContextKey> set = EnumSet.noneOf(ContextKey.class);
    set.addAll(List.of(keys));
    return Set.copyOf(set);
  }

  private static final Set<ContextKey> NONE = Set.of();

  private static final Set<ContextKey> ORIGIN = keys(ContextKey.ORIGIN);

  private static final Set<ContextKey> ORIGIN_AND_ENTITY =
      keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY);

  private static final Set<ContextKey> BLOCK =
      keys(
          ContextKey.ORIGIN,
          ContextKey.BLOCK_STATE,
          ContextKey.TOOL,
          ContextKey.EXPLOSION_RADIUS,
          ContextKey.BLOCK_ENTITY,
          ContextKey.THIS_ENTITY);

  private static final Set<ContextKey> ENTITY =
      keys(
          ContextKey.ORIGIN,
          ContextKey.THIS_ENTITY,
          ContextKey.ATTACKER,
          ContextKey.DIRECT_ATTACKER,
          ContextKey.ATTACKING_PLAYER,
          ContextKey.DAMAGE_SOURCE);

  private static final Set<ContextKey> FISHING =
      keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY, ContextKey.TOOL, ContextKey.ATTACKER);

  private static final Set<ContextKey> INTERACT =
      keys(
          ContextKey.ORIGIN,
          ContextKey.THIS_ENTITY,
          ContextKey.TARGET_ENTITY,
          ContextKey.INTERACTING_ENTITY,
          ContextKey.BLOCK_STATE,
          ContextKey.TOOL);

  private static final Set<ContextKey> DAMAGE =
      keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY, ContextKey.ATTACKER);

  private static final List<LootContextType> ALL = build();

  /** One entry of the registry: the name, the required keys and the keys the set may carry. */
  private record Row(String name, Set<ContextKey> required, Set<ContextKey> allowed) {}

  private static List<LootContextType> build() {
    // spec: LT-R13 for the names and their order, LT-R14b for the parameter sets (provisional)
    List<Row> rows =
        List.of(
            new Row("empty", NONE, NONE),
            new Row("generic", NONE, ALL_DROP_KEYS),
            new Row("chest", ORIGIN, ORIGIN_AND_ENTITY),
            new Row("command", NONE, ALL_DROP_KEYS),
            new Row("command_slot_source", NONE, ORIGIN_AND_ENTITY),
            new Row("command_compute_default", NONE, ALL_DROP_KEYS),
            new Row("command_compute_position", NONE, ORIGIN),
            new Row("command_compute_entity", NONE, ORIGIN_AND_ENTITY),
            new Row("selector", NONE, ORIGIN_AND_ENTITY),
            new Row("villager_trade", NONE, ORIGIN_AND_ENTITY),
            new Row("fishing", ORIGIN, FISHING),
            new Row("entity", ORIGIN, ENTITY),
            new Row("equipment", NONE, ORIGIN_AND_ENTITY),
            new Row("archaeology", NONE, ORIGIN_AND_ENTITY),
            new Row("gift", ORIGIN, ORIGIN_AND_ENTITY),
            new Row("barter", NONE, ORIGIN_AND_ENTITY),
            new Row("vault", NONE, ORIGIN_AND_ENTITY),
            new Row("advancement_reward", NONE, ORIGIN_AND_ENTITY),
            new Row("advancement_entity", NONE, ORIGIN_AND_ENTITY),
            new Row("advancement_location", NONE, ORIGIN_AND_ENTITY),
            new Row("block_use", NONE, INTERACT),
            new Row("block", ORIGIN, BLOCK),
            new Row("shearing", ORIGIN, ORIGIN_AND_ENTITY),
            new Row("entity_interact", NONE, INTERACT),
            new Row("block_interact", ORIGIN, INTERACT),
            new Row("container_process", NONE, ORIGIN_AND_ENTITY),
            new Row("enchanted_damage", NONE, union(ENCHANTED_KEYS, DAMAGE, ENTITY)),
            new Row("enchanted_item", NONE, ENCHANTED_KEYS),
            new Row("enchanted_location", NONE, ENCHANTED_KEYS),
            new Row("enchanted_entity", NONE, ENCHANTED_KEYS),
            new Row("hit_block", NONE, union(ENCHANTED_KEYS, keys(ContextKey.BLOCK_STATE))));
    List<LootContextType> types = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      Row row = rows.get(i);
      types.add(
          new LootContextType(
              Identifier.minecraft(row.name()),
              i,
              row.required(),
              union(row.allowed(), row.required())));
    }
    return List.copyOf(types);
  }

  @SafeVarargs
  private static Set<ContextKey> union(Set<ContextKey>... sets) {
    Set<ContextKey> result = EnumSet.noneOf(ContextKey.class);
    for (Set<ContextKey> set : sets) {
      result.addAll(set);
    }
    return Set.copyOf(result);
  }
}
