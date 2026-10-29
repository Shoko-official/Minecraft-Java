package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;

/**
 * The registry of context types (LT-R13): the 31 identifiers, in the order of their numeric ids,
 * with the provisional parameter sets of LT-R14b. A table whose {@code type} is not here is refused
 * at load time ({@code UNKNOWN_TYPE}); an absent {@code type} reads as {@code minecraft:generic}
 * (LT-R2, provisional).
 */
public final class LootContextTypes {

  private LootContextTypes() {}

  /**
   * Returns the 31 types.
   *
   * @return the types ordered by {@link LootContextType#rawId()}, unmodifiable
   */
  public static List<LootContextType> all() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds a type.
   *
   * @param id a context type identifier
   * @return the type, empty when it is not one of the 31
   */
  public static Optional<LootContextType> find(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the type of a table that has no {@code type} (LT-R2, provisional).
   *
   * @return {@code minecraft:generic}
   */
  public static LootContextType generic() {
    return Skeleton.throwNotImplemented();
  }
}
