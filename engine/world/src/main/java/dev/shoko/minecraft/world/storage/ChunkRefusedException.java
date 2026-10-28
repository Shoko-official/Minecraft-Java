package dev.shoko.minecraft.world.storage;

import java.nio.file.Path;

/**
 * A chunk is refused: it exists but must not be loaded (AR "Entrees / sorties", Refuse). The
 * message names the file, the index in the region and the faulty field or section. The chunk is
 * neither generated nor loaded, and its bytes stay as they are for a repair.
 */
public final class ChunkRefusedException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a refusal.
   *
   * @param file the region file, or the external {@code .mcc} file
   * @param index index of the chunk in the region, 0 to 1023
   * @param detail the faulty field or section, and what is wrong
   */
  public ChunkRefusedException(Path file, int index, String detail) {
    super(detail);
    Skeleton.throwNotImplemented();
  }

  /**
   * Creates a refusal caused by another failure, such as a chunk format fault.
   *
   * @param file the region file, or the external {@code .mcc} file
   * @param index index of the chunk in the region, 0 to 1023
   * @param detail the faulty field or section, and what is wrong
   * @param cause the underlying failure
   */
  public ChunkRefusedException(Path file, int index, String detail, Throwable cause) {
    super(detail, cause);
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the file concerned.
   *
   * @return the path
   */
  public Path file() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the index of the chunk in its region.
   *
   * @return the index, 0 to 1023
   */
  public int index() {
    return Skeleton.throwNotImplemented();
  }
}
