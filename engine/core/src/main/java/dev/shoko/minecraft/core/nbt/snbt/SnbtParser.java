package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

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

  private static final ByteTag TRUE = new ByteTag((byte) 1);
  private static final ByteTag FALSE = new ByteTag((byte) 0);

  /** Canonical UUID text: 8-4-4-4-12 hexadecimal digits. */
  private static final Pattern UUID_TEXT =
      Pattern.compile(
          "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

  private final SnbtCursor cursor;

  private SnbtParser(String text) {
    this.cursor = new SnbtCursor(Objects.requireNonNull(text, "text"));
  }

  /**
   * Parses any value, as the {@code minecraft:nbt_tag} argument does (R39, R40).
   *
   * @param text the SNBT text
   * @return the value
   * @throws NbtException when the text is not one valid value (R51)
   */
  public static NbtTag parse(String text) throws NbtException {
    return new SnbtParser(text).readWhole();
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
    var parser = new SnbtParser(text);
    parser.cursor.skipBlanks();
    int start = parser.cursor.position();
    if (parser.readWhole() instanceof CompoundTag compound) {
      return compound;
    }
    throw parser.cursor.error(Reason.SNBT_NOT_A_COMPOUND, start, "expected a compound");
  }

  private NbtTag readWhole() throws NbtException {
    NbtTag value = readValue();
    cursor.skipBlanks();
    if (!cursor.atEnd()) {
      throw cursor.syntax("text after the value");
    }
    return value;
  }

  private NbtTag readValue() throws NbtException {
    cursor.skipBlanks();
    if (cursor.atEnd()) {
      throw cursor.syntax("expected a value");
    }
    return switch (cursor.peek()) {
      case '{' -> readCompound();
      case '[' -> readListOrArray();
      case '"', '\'' -> new StringTag(SnbtStrings.readQuoted(cursor));
      default -> readWord();
    };
  }

  /** An unquoted value: number, boolean, operation or string (R43, R44, R46, R50). */
  private NbtTag readWord() throws NbtException {
    int start = cursor.position();
    String word = cursor.readUnquoted();
    if (word.isEmpty()) {
      throw cursor.syntax("expected a value");
    }
    if (SnbtNumbers.startsNumber(word.charAt(0))) {
      return SnbtNumbers.parse(word, start);
    }
    if ((word.equals("bool") || word.equals("uuid")) && cursor.startsWith("(")) {
      return readOperation(word);
    }
    return switch (word) {
      case "true" -> TRUE;
      case "false" -> FALSE;
      default -> new StringTag(word);
    };
  }

  private NbtTag readOperation(String name) throws NbtException {
    cursor.expect('(');
    cursor.skipBlanks();
    int argumentStart = cursor.position();
    NbtTag argument = readValue();
    cursor.skipBlanks();
    cursor.expect(')');
    if (name.equals("bool") && argument instanceof NumericTag number) {
      return number.doubleValue() != 0 ? TRUE : FALSE;
    }
    if (name.equals("uuid")
        && argument instanceof StringTag text
        && UUID_TEXT.matcher(text.value()).matches()) {
      UUID uuid = UUID.fromString(text.value());
      long most = uuid.getMostSignificantBits();
      long least = uuid.getLeastSignificantBits();
      // Most significant int first (R8).
      return IntArrayTag.of((int) (most >>> 32), (int) most, (int) (least >>> 32), (int) least);
    }
    throw cursor.error(
        Reason.SNBT_INVALID_OPERATION, argumentStart, "invalid argument of " + name + "()");
  }

  private CompoundTag readCompound() throws NbtException {
    cursor.expect('{');
    var entries = new LinkedHashMap<String, NbtTag>();
    cursor.skipBlanks();
    if (cursor.startsWith("}")) {
      cursor.next();
      return CompoundTag.empty();
    }
    do {
      String key = readKey();
      cursor.skipBlanks();
      cursor.expect(':');
      // A duplicate key keeps the last value (R42).
      entries.put(key, readValue());
    } while (!closeOrContinue('}'));
    return CompoundTag.of(entries);
  }

  private String readKey() throws NbtException {
    cursor.skipBlanks();
    if (!cursor.atEnd() && (cursor.peek() == '"' || cursor.peek() == '\'')) {
      return SnbtStrings.readQuoted(cursor);
    }
    String key = cursor.readUnquoted();
    if (key.isEmpty()) {
      throw cursor.syntax("expected a key");
    }
    return key;
  }

  /**
   * After an element: consumes {@code ,} or {@code close}, a trailing comma included (R49).
   *
   * @return true when the container is closed
   */
  private boolean closeOrContinue(char close) throws NbtException {
    cursor.skipBlanks();
    if (cursor.startsWith(",")) {
      cursor.next();
      cursor.skipBlanks();
      if (cursor.startsWith(String.valueOf(close))) {
        cursor.next();
        return true;
      }
      return false;
    }
    cursor.expect(close);
    return true;
  }

  private NbtTag readListOrArray() throws NbtException {
    cursor.expect('[');
    if (cursor.startsWith("B;") || cursor.startsWith("I;") || cursor.startsWith("L;")) {
      char kind = cursor.next();
      cursor.next();
      return readArray(kind);
    }
    cursor.skipBlanks();
    if (cursor.startsWith("]")) {
      cursor.next();
      return ListTag.empty();
    }
    var elements = new ArrayList<NbtTag>();
    do {
      elements.add(readValue());
    } while (!closeOrContinue(']'));
    return ListTag.of(elements);
  }

  /** Reads the elements of {@code [B;}, {@code [I;} or {@code [L;} after the semicolon (R48). */
  private NbtTag readArray(char kind) throws NbtException {
    var values = new ArrayList<Long>();
    cursor.skipBlanks();
    if (cursor.startsWith("]")) {
      cursor.next();
    } else {
      do {
        cursor.skipBlanks();
        int start = cursor.position();
        boolean number = !cursor.atEnd() && SnbtNumbers.startsNumber(cursor.peek());
        NbtTag element = readValue();
        if (!number || !accepts(kind, element)) {
          throw cursor.error(
              Reason.SNBT_INVALID_ARRAY_ELEMENT, start, "element not allowed in [" + kind + ";]");
        }
        values.add(((NumericTag) element).longValue());
      } while (!closeOrContinue(']'));
    }
    return switch (kind) {
      case 'B' -> ByteArrayTag.of(toBytes(values));
      case 'I' -> IntArrayTag.of(values.stream().mapToInt(Long::intValue).toArray());
      default -> LongArrayTag.of(values.stream().mapToLong(Long::longValue).toArray());
    };
  }

  /** B takes Byte; I takes Byte, Short, Int; L takes Byte, Short, Int, Long (R48). */
  private static boolean accepts(char kind, NbtTag element) {
    return switch (element) {
      case ByteTag _ -> true;
      case ShortTag _, IntTag _ -> kind != 'B';
      case LongTag _ -> kind == 'L';
      default -> false;
    };
  }

  private static byte[] toBytes(List<Long> values) {
    byte[] bytes = new byte[values.size()];
    for (int k = 0; k < bytes.length; k++) {
      bytes[k] = values.get(k).byteValue();
    }
    return bytes;
  }
}
