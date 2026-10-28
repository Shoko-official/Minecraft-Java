package dev.shoko.minecraft.world.storage;

/**
 * A chunk to save has more NBT bytes than the decompression limit of the store (AR-R29). Writing it
 * would give a record that the same store refuses as TOO_LARGE on the next load, and the position
 * would then be marked and locked (AR-R28). Nothing was written. Unchecked: the limit is a choice
 * of the caller, who must save only chunks within it or open the store with a higher one.
 */
public final class ChunkTooLargeException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the error.
   *
   * @param message names the chunk, its size and the limit
   */
  public ChunkTooLargeException(String message) {
    super(message);
  }
}
