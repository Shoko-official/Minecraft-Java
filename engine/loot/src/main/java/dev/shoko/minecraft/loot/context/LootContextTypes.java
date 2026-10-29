package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.data.id.Identifier;
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

  private static final List<LootContextType> ALL = build();

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

  private static List<LootContextType> build() {
    // spec: LT-R13 for the names and their order, LT-R14b for the parameter sets (provisional)
    Set<ContextKey> none = Set.of();
    Set<ContextKey> block =
        keys(
            ContextKey.ORIGIN,
            ContextKey.BLOCK_STATE,
            ContextKey.TOOL,
            ContextKey.EXPLOSION_RADIUS,
            ContextKey.BLOCK_ENTITY,
            ContextKey.THIS_ENTITY);
    Set<ContextKey> entity =
        keys(
            ContextKey.ORIGIN,
            ContextKey.THIS_ENTITY,
            ContextKey.ATTACKER,
            ContextKey.DIRECT_ATTACKER,
            ContextKey.ATTACKING_PLAYER,
            ContextKey.DAMAGE_SOURCE);
    Set<ContextKey> origin = keys(ContextKey.ORIGIN);
    Set<ContextKey> originAndEntity = keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY);
    Set<ContextKey> fishing =
        keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY, ContextKey.TOOL, ContextKey.ATTACKER);
    Set<ContextKey> interact =
        keys(
            ContextKey.ORIGIN,
            ContextKey.THIS_ENTITY,
            ContextKey.TARGET_ENTITY,
            ContextKey.INTERACTING_ENTITY,
            ContextKey.BLOCK_STATE,
            ContextKey.TOOL);
    Set<ContextKey> damage = keys(ContextKey.ORIGIN, ContextKey.THIS_ENTITY, ContextKey.ATTACKER);
    Object[][] rows = {
      {"empty", none, none},
      {"generic", none, ALL_DROP_KEYS},
      {"chest", origin, originAndEntity},
      {"command", none, ALL_DROP_KEYS},
      {"command_slot_source", none, originAndEntity},
      {"command_compute_default", none, ALL_DROP_KEYS},
      {"command_compute_position", none, origin},
      {"command_compute_entity", none, originAndEntity},
      {"selector", none, originAndEntity},
      {"villager_trade", none, originAndEntity},
      {"fishing", origin, fishing},
      {"entity", origin, entity},
      {"equipment", none, originAndEntity},
      {"archaeology", none, originAndEntity},
      {"gift", origin, originAndEntity},
      {"barter", none, originAndEntity},
      {"vault", none, originAndEntity},
      {"advancement_reward", none, originAndEntity},
      {"advancement_entity", none, originAndEntity},
      {"advancement_location", none, originAndEntity},
      {"block_use", none, interact},
      {"block", origin, block},
      {"shearing", origin, originAndEntity},
      {"entity_interact", none, interact},
      {"block_interact", origin, interact},
      {"container_process", none, originAndEntity},
      {"enchanted_damage", none, union(ENCHANTED_KEYS, damage, entity)},
      {"enchanted_item", none, ENCHANTED_KEYS},
      {"enchanted_location", none, ENCHANTED_KEYS},
      {"enchanted_entity", none, ENCHANTED_KEYS},
      {"hit_block", none, union(ENCHANTED_KEYS, keys(ContextKey.BLOCK_STATE))}
    };
    List<LootContextType> types = new java.util.ArrayList<>();
    for (int i = 0; i < rows.length; i++) {
      @SuppressWarnings("unchecked")
      Set<ContextKey> required = (Set<ContextKey>) rows[i][1];
      @SuppressWarnings("unchecked")
      Set<ContextKey> allowed = (Set<ContextKey>) rows[i][2];
      Set<ContextKey> withRequired = union(allowed, required);
      types.add(
          new LootContextType(
              Identifier.minecraft((String) rows[i][0]), i, required, withRequired));
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
