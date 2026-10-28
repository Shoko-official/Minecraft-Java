package dev.shoko.minecraft.world.storage;

/**
 * A decompression would produce more bytes than the limit it was given (AR-R29). The decoder stops
 * as soon as the excess is certain, without producing or allocating more than the limit. A reader
 * turns it into a refused chunk with the cause TOO_LARGE.
 */
public final class OutputLimitExceededException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a fault.
   *
   * @param limit the limit that was exceeded, in bytes
   */
  public OutputLimitExceededException(long limit) {
    super("decompressed output exceeds the limit");
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the limit that was exceeded.
   *
   * @return the limit, in bytes
   */
  public long limit() {
    return Skeleton.throwNotImplemented();
  }
}
