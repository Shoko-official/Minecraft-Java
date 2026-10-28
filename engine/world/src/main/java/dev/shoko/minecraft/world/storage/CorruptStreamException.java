package dev.shoko.minecraft.world.storage;

/**
 * A compressed stream is malformed: it does not decode (AR-R20, AR-R30 a). Not an input or output
 * failure: the bytes are in memory. A reader turns it into an absent chunk with the cause
 * UNREADABLE_RECORD.
 */
public final class CorruptStreamException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a fault.
   *
   * @param message what is wrong with the stream
   */
  public CorruptStreamException(String message) {
    super(message);
    Skeleton.throwNotImplemented();
  }

  /**
   * Creates a fault caused by another one.
   *
   * @param message what is wrong with the stream
   * @param cause the underlying failure
   */
  public CorruptStreamException(String message, Throwable cause) {
    super(message, cause);
    Skeleton.throwNotImplemented();
  }
}
