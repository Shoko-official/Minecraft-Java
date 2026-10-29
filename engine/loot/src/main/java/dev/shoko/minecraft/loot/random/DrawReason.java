package dev.shoko.minecraft.loot.random;

/** Which decision of the algorithm consumed random steps (LT-R66, hypothesis H-draw). */
public enum DrawReason {
  /** The number of rolls of a pool, including the bonus rolls (LT-R19b). */
  POOL_ROLLS,
  /** A random condition of a pool (LT-R19a). */
  POOL_CONDITION,
  /** A random condition of an entry, evaluated while the entries are expanded (LT-R20). */
  ENTRY_CONDITION,
  /** The weighted choice among the candidates of a roll (LT-R26). */
  ENTRY_SELECTION,
  /** A random condition of a modifier (LT-R38). */
  MODIFIER_CONDITION,
  /** The effect of a modifier on the chosen stack, such as {@code set_count} (LT-R39 to LT-R46). */
  MODIFIER,
  /** A number provider evaluated outside the cases above (LT-R50 to LT-R53). */
  NUMBER_PROVIDER,
  /** The selection of enchantments (EN-R35, EN-R38, EN-R39). */
  ENCHANTMENT_SELECTION,
  /** The choice of the slots of an unpacked container (LT-R70). */
  CONTAINER_PLACEMENT
}
