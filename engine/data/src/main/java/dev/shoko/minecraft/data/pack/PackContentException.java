package dev.shoko.minecraft.data.pack;

import java.io.IOException;

/**
 * The content of a file of a pack cannot be used although the file system works: the file is larger
 * than {@link PackFiles#MAX_FILE_BYTES}, or its entry in an archive is damaged. This is a problem
 * of the data, which the loader reports as a diagnostic (R54), unlike a plain {@link IOException}.
 */
public final class PackContentException extends IOException {

  private static final long serialVersionUID = 1L;

  PackContentException(String message) {
    super(message);
  }

  PackContentException(String message, Throwable cause) {
    super(message, cause);
  }
}
