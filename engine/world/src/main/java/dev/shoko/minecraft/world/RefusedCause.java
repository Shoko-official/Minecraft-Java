package dev.shoko.minecraft.world;

/**
 * Why a chunk is refused (AR-R26): a record exists but the engine does not interpret it. The chunk
 * is neither generated nor loaded and its bytes are preserved. The list is closed: a cause outside
 * it is a defect of the spec.
 */
public enum RefusedCause {
  /** Compression type 127 or unknown (AR-R14). */
  UNSUPPORTED_COMPRESSION,
  /** The record asks for a {@code .mcc} file that does not exist (AR-R18). */
  MISSING_EXTERNAL_FILE,
  /** The decompressed output or the NBT quota exceeds its limit (AR-R29, AR-R30 e). */
  TOO_LARGE,
  /** The NBT nesting exceeds the depth limit (AR-R30 e). */
  TOO_DEEP,
  /** {@code DataVersion} is missing, mistyped or not the supported one (CF-R35). */
  UNSUPPORTED_VERSION,
  /** The status is unknown or was removed from the game (CF-R29). */
  UNSUPPORTED_STATUS,
  /** A paletted container is inconsistent (CF-R10). */
  INVALID_CONTAINER,
  /** A section has no {@code Y}, a mistyped one, or a duplicate {@code Y} (CF-R32 b, c). */
  INVALID_SECTION,
  /** A required field of the chunk root is missing or mistyped (CF-R32 a, CF-R35). */
  INVALID_FIELD
}
