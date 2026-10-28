package dev.shoko.minecraft.world.chunk;

/**
 * A chunk tree does not follow the schema, so the chunk is refused (CF-R10, CF-R20 to CF-R29). The
 * fault names the field: a path such as {@code sections[3].block_states}.
 */
public final class ChunkFormatException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Why the chunk is refused. */
  public enum Reason {
    /** A required field is missing. */
    MISSING_FIELD,
    /** A field has another tag type than the schema says. */
    WRONG_TYPE,
    /** {@code DataVersion} is not the supported one (chunk spec, section Entrees / sorties). */
    UNSUPPORTED_DATA_VERSION,
    /** The status is unknown or was removed (CF-R29). */
    UNSUPPORTED_STATUS,
    /** A container is inconsistent: empty palette, wrong data length, index too large (CF-R10). */
    INVALID_CONTAINER,
    /** A value is outside what the schema allows. */
    INVALID_VALUE
  }

  /**
   * Creates a fault.
   *
   * @param reason why the chunk is refused
   * @param path the faulty field
   * @param message a description for humans
   */
  public ChunkFormatException(Reason reason, String path, String message) {
    super(message);
    Skeleton.throwNotImplemented();
  }

  /**
   * Creates a fault caused by another one.
   *
   * @param reason why the chunk is refused
   * @param path the faulty field
   * @param message a description for humans
   * @param cause the underlying failure
   */
  public ChunkFormatException(Reason reason, String path, String message, Throwable cause) {
    super(message, cause);
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns why the chunk is refused.
   *
   * @return the reason
   */
  public Reason reason() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the faulty field.
   *
   * @return the path, such as {@code sections[3].block_states.data}
   */
  public String path() {
    return Skeleton.throwNotImplemented();
  }
}
