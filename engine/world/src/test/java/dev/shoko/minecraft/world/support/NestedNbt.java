package dev.shoko.minecraft.world.support;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Hand-made NBT bytes whose nesting depth is the point. */
public final class NestedNbt {

  private NestedNbt() {}

  /**
   * The chunk NBT with one more key, {@code deep}, that holds Compounds one inside the other so
   * that the whole tree has {@code totalLevels} levels of nesting, the root included (at least 3).
   */
  public static byte[] chunkWithDepth(byte[] chunkNbt, int totalLevels) {
    int chain = totalLevels - 2;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(chunkNbt, 0, chunkNbt.length - 1);
    out.write(0x0a);
    out.write(0);
    out.write(4);
    out.writeBytes("deep".getBytes(StandardCharsets.US_ASCII));
    for (int level = 0; level < chain; level++) {
      out.write(0x0a);
      out.write(0);
      out.write(1);
      out.write('a');
    }
    for (int level = 0; level <= chain; level++) {
      out.write(0);
    }
    out.write(0);
    return out.toByteArray();
  }
}
