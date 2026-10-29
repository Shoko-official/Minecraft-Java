package dev.shoko.minecraft.data.testing;

import java.util.List;
import java.util.Random;

/**
 * Generates random JSON text from a pool of keys and scalar texts. Objects may repeat a key, so the
 * result is read with warnings allowed. The generator only uses the seeded {@link Random} it is
 * given.
 */
public final class RandomJson {

  private final Random random;
  private final List<String> keys;
  private final List<String> scalars;
  private final int maxDepth;

  public RandomJson(Random random, List<String> keys, List<String> scalars, int maxDepth) {
    this.random = random;
    this.keys = keys;
    this.scalars = scalars;
    this.maxDepth = maxDepth;
  }

  public String next() {
    StringBuilder out = new StringBuilder();
    value(0, out);
    return out.toString();
  }

  private void value(int depth, StringBuilder out) {
    int kind = random.nextInt(depth >= maxDepth ? 4 : 7);
    if (kind < 4) {
      out.append(scalars.get(random.nextInt(scalars.size())));
    } else if (kind < 6) {
      out.append('{');
      int count = random.nextInt(4);
      for (int i = 0; i < count; i++) {
        out.append(i == 0 ? "" : ",");
        out.append('"').append(keys.get(random.nextInt(keys.size()))).append("\":");
        value(depth + 1, out);
      }
      out.append('}');
    } else {
      out.append('[');
      int count = random.nextInt(4);
      for (int i = 0; i < count; i++) {
        out.append(i == 0 ? "" : ",");
        value(depth + 1, out);
      }
      out.append(']');
    }
  }
}
