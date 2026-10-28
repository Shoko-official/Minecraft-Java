package dev.shoko.minecraft.world;

/**
 * The kinds of non-fatal finding a reader reports (a warning has no effect on the outcome of a
 * read, AR-R26). Each code names the situation, never the wording of its message. The rule that
 * raises each code is given on the constant.
 */
public enum WarningCode {
  /** A location of the region header is invalid; the chunk is absent (AR-R20). */
  INVALID_LOCATION,
  /** The record does not decompress or its NBT is unreadable; the chunk is absent (AR-R20, R30). */
  UNREADABLE_RECORD,
  /** {@code xPos} or {@code zPos} differ from the expected chunk; the chunk is absent (AR-R21). */
  POSITION_MISMATCH,
  /** The NBT root has a non-empty name, which is ignored (AR-R30 c). */
  ROOT_NAME_IGNORED,
  /** Bytes follow the NBT root and are ignored (AR-R30 d). */
  TRAILING_BYTES,
  /** A palette entry is neither a String nor a Compound with a String {@code id} (BS-R22 a). */
  MALFORMED_PALETTE_ENTRY,
  /** A block identifier is unknown and became {@code air} (BS-R21). */
  UNKNOWN_BLOCK,
  /** A property is unknown for its block and was ignored (BS-R21). */
  UNKNOWN_PROPERTY,
  /** A property value is not in the list of its block and took the default's value (BS-R21). */
  INVALID_PROPERTY_VALUE,
  /** A property is missing and took the value of the default state (BS-R20). */
  DEFAULTED_PROPERTY,
  /** A biome identifier is unknown and became {@code minecraft:plains} (CF-R11). */
  UNKNOWN_BIOME,
  /** A {@code PostProcessing} Short with a non-zero high nibble was ignored (CF-R24). */
  POST_PROCESSING_SHORT_IGNORED,
  /** A {@code PostProcessing} list beyond the sections of the world height was ignored (CF-R24). */
  POST_PROCESSING_LIST_IGNORED,
  /** A block entity lies outside the chunk and was ignored (CF-R26). */
  BLOCK_ENTITY_OUTSIDE_CHUNK,
  /** A section is too far from the world height and was ignored (CF-R32 e). */
  SECTION_IGNORED,
  /** A section of the world height was missing and was synthesized empty (CF-R32 f). */
  SECTION_MISSING,
  /** The {@code sections} list is empty: one warning for the whole list (CF-R32 g). */
  SECTIONS_EMPTY,
  /** {@code yPos} differs from the lowest section of the dimension and is ignored (CF-R33). */
  YPOS_MISMATCH,
  /** {@code yPos} is not an Int and is ignored (CF-R33). */
  YPOS_WRONG_TYPE,
  /** A heightmap type is present but invalid, so it is unknown (CF-R34). */
  HEIGHTMAP_INVALID,
  /** An optional field of the chunk root has another tag type than the schema says (CF-R35). */
  FIELD_WRONG_TYPE
}
