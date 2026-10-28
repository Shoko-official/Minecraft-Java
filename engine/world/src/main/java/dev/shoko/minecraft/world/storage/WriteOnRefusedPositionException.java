package dev.shoko.minecraft.world.storage;

/**
 * A write was requested at a position that a read of the same store refused (AR-R28, category
 * WRITE_ON_REFUSED_POSITION). It is a write error, not one of the causes of a read (AR-R26). It is
 * unchecked because it can only come from a logic error of the caller, which it makes visible: a
 * refused chunk is never loaded, so there is nothing legitimate to save. No byte was written.
 */
public final class WriteOnRefusedPositionException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the error.
   *
   * @param message names the region file, the index and the refusal that marked the position
   */
  public WriteOnRefusedPositionException(String message) {
    super(message);
    Skeleton.throwNotImplemented();
  }
}
