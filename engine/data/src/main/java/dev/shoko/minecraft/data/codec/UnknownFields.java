package dev.shoko.minecraft.data.codec;

/**
 * What an object codec does with a field it does not read (R63). The module that owns the codec
 * chooses, in its own spec; the default is {@link #IGNORE}.
 */
public enum UnknownFields {
  /**
   * The field is skipped, with no diagnostic (R63 a). The default, also for tags and {@code
   * pack.mcmeta}.
   */
  IGNORE,
  /**
   * Each unknown field gives a {@code CODEC_UNKNOWN_FIELD} error at {@code $.name}, in text order;
   * the known fields are decoded normally (R63 b).
   */
  REJECT
}
