package dev.shoko.minecraft.world.palette;

/**
 * A container, a palette entry or a packed array does not follow the disk form (CF-R6 to CF-R10,
 * BS-R19). The message names the faulty element; callers that know the section add its position.
 */
public final class PaletteFormatException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a fault.
   *
   * @param message a description for humans
   */
  public PaletteFormatException(String message) {
    super(message);
  }
}
