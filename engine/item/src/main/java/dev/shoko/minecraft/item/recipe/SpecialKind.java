package dev.shoko.minecraft.item.recipe;

/**
 * The serializers whose result depends on the content of the stacks (RC-R43 to RC-R49e). The
 * constant names the serializer; the keys of {@link SpecialRecipe#parts()} are the field names of
 * the file.
 */
public enum SpecialKind {
  /** {@code crafting_special_bannerduplicate}: part {@code banner} (RC-R43). */
  BANNER_DUPLICATE,
  /** {@code crafting_special_bookcloning}: parts {@code source}, {@code material} (RC-R44). */
  BOOK_CLONING,
  /** {@code crafting_special_firework_rocket}: parts {@code shell}, {@code fuel}, {@code star}. */
  FIREWORK_ROCKET,
  /**
   * {@code crafting_special_firework_star}: parts {@code fuel}, {@code dye}, {@code trail}, {@code
   * twinkle} and one {@code shapes.<name>} per shape (RC-R46).
   */
  FIREWORK_STAR,
  /** {@code crafting_special_firework_star_fade}: parts {@code target}, {@code dye} (RC-R47). */
  FIREWORK_STAR_FADE,
  /** {@code crafting_special_mapextending}: parts {@code map}, {@code material} (RC-R48). */
  MAP_EXTENDING,
  /** {@code crafting_special_repairitem}: no part, no result (RC-R49a). */
  REPAIR_ITEM,
  /** {@code crafting_special_shielddecoration}: parts {@code target}, {@code banner} (RC-R49b). */
  SHIELD_DECORATION,
  /**
   * {@code crafting_decorated_pot}: parts {@code back}, {@code front}, {@code left}, {@code right}.
   */
  DECORATED_POT,
  /** {@code crafting_dye}: parts {@code target}, {@code dye} (RC-R49d). */
  DYE,
  /** {@code crafting_imbue}: parts {@code source}, {@code material} (RC-R49e). */
  IMBUE
}
