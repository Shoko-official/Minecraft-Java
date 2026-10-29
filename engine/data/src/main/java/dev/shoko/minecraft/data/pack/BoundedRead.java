package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.io.InputStream;

/** Reads a file in full without ever holding more than the size ceiling in memory. */
final class BoundedRead {

  private BoundedRead() {}

  static byte[] readAll(InputStream in, String path) throws IOException {
    byte[] data = in.readNBytes(PackFiles.MAX_FILE_BYTES + 1);
    if (data.length > PackFiles.MAX_FILE_BYTES) {
      throw tooLarge(path);
    }
    return data;
  }

  static PackContentException tooLarge(String path) {
    return new PackContentException(
        "file " + path + " is larger than " + PackFiles.MAX_FILE_BYTES + " bytes");
  }
}
