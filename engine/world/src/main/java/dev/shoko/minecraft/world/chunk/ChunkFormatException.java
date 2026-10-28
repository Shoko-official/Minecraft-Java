package dev.shoko.minecraft.world.chunk;

/**
 * A tree of an {@code entities/} or {@code poi/} chunk does not follow its schema (AR-R22, AR-R23).
 * The fault names the field: a path such as {@code Sections.-4.Records}. The chunks of {@code
 * region/} do not use it: their faults are the refusals of {@link
 * dev.shoko.minecraft.world.ReadResult}.
 */
public final class ChunkFormatException extends Exception {

  private static final long serialVersionUID = 1L;

  private final Reason reason;
  private final String path;

  /** What is wrong with the tree. */
  public enum Reason {
    /** A required field is missing. */
    MISSING_FIELD,
    /** A field has another tag type than the schema says. */
    WRONG_TYPE,
    /** A value is outside what the schema allows. */
    INVALID_VALUE
  }

  /**
   * Creates a fault.
   *
   * @param reason what is wrong
   * @param path the faulty field
   * @param message a description for humans
   */
  public ChunkFormatException(Reason reason, String path, String message) {
    super(message);
    this.reason = reason;
    this.path = path;
  }

  /**
   * Creates a fault caused by another one.
   *
   * @param reason what is wrong
   * @param path the faulty field
   * @param message a description for humans
   * @param cause the underlying failure
   */
  public ChunkFormatException(Reason reason, String path, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
    this.path = path;
  }

  /**
   * Returns what is wrong.
   *
   * @return the reason
   */
  public Reason reason() {
    return reason;
  }

  /**
   * Returns the faulty field.
   *
   * @return the path, such as {@code Sections.-4.Records}
   */
  public String path() {
    return path;
  }
}
