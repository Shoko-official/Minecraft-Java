package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.function.BiFunction;

/** A codec defined by its decoding function; the building block of every combinator. */
final class FunctionCodec<T> extends Codec<T> {

  private final BiFunction<JsonNode, RegistryAccess, Outcome<T>> decoder;

  FunctionCodec(BiFunction<JsonNode, RegistryAccess, Outcome<T>> decoder) {
    this.decoder = decoder;
  }

  @Override
  public Outcome<T> decode(JsonNode node, RegistryAccess access) {
    return decoder.apply(node, access);
  }
}
