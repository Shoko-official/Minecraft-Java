package dev.shoko.minecraft.core.nbt;

import java.util.Objects;

/**
 * String value: any Java string, never {@code null} (constructing with {@code null} throws {@link
 * NullPointerException}). It is written in modified UTF-8 behind an unsigned 16-bit byte length
 * (R10, R11). Unpaired surrogates are allowed (R12). A value whose encoding exceeds 65535 bytes can
 * exist in memory; only writing it fails (R13).
 *
 * @param value the text
 */
public record StringTag(String value) implements NbtTag {

  public StringTag {
    Objects.requireNonNull(value, "value");
  }

  /**
   * Returns {@link TagType#STRING} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return TagType.STRING;
  }
}
