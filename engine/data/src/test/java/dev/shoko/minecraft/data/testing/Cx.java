package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.codec.Codec;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;

/** Short helpers to run codecs on JSON text against the miniature universe of the spec. */
public final class Cx {

  private Cx() {}

  /** The fictional registry of blocks of the codec vectors (table before VC1). */
  public static RegistryKey<String> blocks() {
    return new RegistryKey<>(Identifier.minecraft("block"));
  }

  public static TagKey<String> tagT() {
    return new TagKey<>(blocks(), Identifier.minecraft("t"));
  }

  /**
   * Universe {@code minecraft:a, minecraft:b, minecraft:c} and tag {@code #minecraft:t = [a, b]}.
   */
  public static FakeAccess universe() {
    return FakeAccess.create().entries(blocks(), "a", "b", "c").tag(blocks(), "t", "a", "b");
  }

  public static <T> Outcome<T> decode(Codec<T> codec, String json) {
    return codec.decode(Jx.parse(json), FakeAccess.create());
  }

  public static <T> Outcome<T> decode(Codec<T> codec, String json, RegistryAccess access) {
    return codec.decode(Jx.parse(json), access);
  }

  public static <T> T value(Codec<T> codec, String json) {
    Outcome<T> outcome = decode(codec, json);
    if (outcome.hasErrors() || outcome.value().isEmpty()) {
      throw new AssertionError("unexpected result for " + json + ": " + Diags.summary(outcome));
    }
    return outcome.value().get();
  }
}
