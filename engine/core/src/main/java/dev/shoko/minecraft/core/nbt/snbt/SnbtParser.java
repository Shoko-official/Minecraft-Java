package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;

/**
 * Parses SNBT text into a value (R41 to R50). The whole text must be one value; blanks (space, tab,
 * carriage return, line feed) are allowed between tokens and around the value (R41). Invalid text
 * fails with an {@link NbtException} whose reason starts with {@code SNBT_} and gives no result
 * (R51).
 *
 * <p>Grammar summary, every rule provisional until probe PS1 (vectors T1 to T52):
 *
 * <ul>
 *   <li>Compound {@code {}} or {@code {key: value, ...}}, key quoted or unquoted; a duplicate key
 *       keeps the last value (R42).
 *   <li>Unquoted string: only {@code 0-9 A-Z a-z _ - . +}, and a value does not start with a digit,
 *       {@code -}, {@code .} or {@code +}; {@code :} always needs quotes (R43).
 *   <li>Unquoted {@code true} and {@code false} are Byte 1 and 0; other words are strings (R44).
 *   <li>Quoted string with {@code "} or {@code '} and the escapes of R45.
 *   <li>Numbers with type, sign suffixes, {@code 0x} and {@code 0b} prefixes and {@code _}
 *       separators; out-of-range values fail (R46).
 *   <li>List {@code []} or {@code [v, ...]}, heterogeneous allowed without promotion (R47).
 *   <li>Typed arrays {@code [B; ...]}, {@code [I; ...]}, {@code [L; ...]}, possibly empty (R48).
 *   <li>Trailing comma allowed in compounds, lists and arrays (R49).
 *   <li>Operations {@code bool(x)} and {@code uuid("...")} (R50).
 * </ul>
 */
public final class SnbtParser {

  private SnbtParser() {}

  /**
   * Parses any value, as the {@code minecraft:nbt_tag} argument does (R39, R40).
   *
   * @param text the SNBT text
   * @return the value
   * @throws NbtException when the text is not one valid value (R51)
   */
  public static NbtTag parse(String text) throws NbtException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Parses a compound, as the {@code minecraft:nbt_compound_tag} argument does (R39, R40): a valid
   * value of another type fails with {@link NbtException.Reason#SNBT_NOT_A_COMPOUND}.
   *
   * @param text the SNBT text
   * @return the compound
   * @throws NbtException when the text is not one valid compound (R40, R51)
   */
  public static CompoundTag parseCompound(String text) throws NbtException {
    return Skeleton.throwNotImplemented();
  }
}
