package dev.shoko.minecraft.world.chunk;

import java.util.Locale;
import java.util.Optional;

/**
 * Generation status of a chunk, in generation order (CF-R27, CF-R28). Only {@link #FULL} is a
 * complete chunk; every other status is a proto-chunk. The statuses {@code noise}, {@code surface}
 * and {@code carvers} of earlier versions do not exist (CF-R29).
 */
public enum ChunkStatus {
  EMPTY,
  STRUCTURE_STARTS,
  STRUCTURE_REFERENCES,
  BIOMES,
  TERRAIN,
  FEATURES,
  INITIALIZE_LIGHT,
  LIGHT,
  SPAWN,
  FULL;

  /**
   * Returns the identifier written in the {@code Status} field, such as {@code minecraft:full}
   * (CF-R20).
   *
   * @return the identifier with namespace
   */
  public String id() {
    return "minecraft:" + name().toLowerCase(Locale.ROOT);
  }

  /**
   * Finds a status by identifier.
   *
   * @param id identifier with namespace
   * @return the status, or empty for an unknown one, including the removed statuses (CF-R29)
   */
  public static Optional<ChunkStatus> byId(String id) {
    for (ChunkStatus status : values()) {
      if (status.id().equals(id)) {
        return Optional.of(status);
      }
    }
    return Optional.empty();
  }

  /**
   * Tells whether this is the complete status.
   *
   * @return {@code true} for {@link #FULL}
   */
  public boolean isFull() {
    return this == FULL;
  }
}
