package dev.shoko.minecraft.world;

/**
 * Why a chunk is absent (AR-R26): no usable chunk at the position, and not an error of the world.
 * The engine generates the chunk. The list is closed: a cause outside it is a defect of the spec.
 */
public enum AbsentCause {
  /** The region file does not exist; no warning (AR-R19). */
  NO_REGION_FILE,
  /** The location is zero; no warning (AR-R19). */
  EMPTY_SLOT,
  /** The location is invalid; one warning (AR-R20). */
  INVALID_LOCATION,
  /** The stream does not decompress, or its NBT is not a Compound document (AR-R20, AR-R30). */
  UNREADABLE_RECORD,
  /** {@code xPos} or {@code zPos} are missing, mistyped or differ from the position (AR-R21). */
  POSITION_MISMATCH
}
