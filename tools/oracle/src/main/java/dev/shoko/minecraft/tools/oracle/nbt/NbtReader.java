package dev.shoko.minecraft.tools.oracle.nbt;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal read-only NBT decoder. It shares no code with the engine on purpose: a reading bug shared
 * by the engine and the oracle would make the comparison blind (docs/VERIFICATION.md). Its behavior
 * follows docs/specs/core-nbt.md, including the provisional rules.
 *
 * <p>Tags map to plain Java values: byte {@link Byte}, short {@link Short}, int {@link Integer},
 * long {@link Long}, float {@link Float}, double {@link Double}, string {@link String}, byte, int
 * and long arrays {@code byte[]}, {@code int[]}, {@code long[]}, list an unmodifiable {@link List},
 * compound an unmodifiable {@link Map} in file order.
 *
 * <p>Accepted as the spec says: an empty list of any known element type (R23), a list with a
 * negative length read as empty (R25), a duplicate compound key whose last value wins at the
 * position of the first (R27), a lone surrogate in a string (R12), bytes after the root (R34).
 * Refused: an unknown tag id, also as a list element type (R33), a non-empty list of end tags
 * (R24), a negative array length (R25), a raw 00 byte, a 4-byte sequence or a truncated sequence in
 * a string (R12), truncated data (R33), more than 512 nested lists and compounds, root included
 * (R31). Gzip and zlib input is decompressed to its end and its trailer verified. Errors name the
 * path of the tag and the offset of the decoded byte where reading stopped.
 */
public final class NbtReader {

  /** Nesting limit of R31, root included. */
  public static final int DEFAULT_MAX_DEPTH = 512;

  /**
   * Limit on the decoded size, which bounds decompression bombs. R32 sets no limit for files; this
   * instrument keeps one, above the largest reference file and the 64 MiB probe of the spec.
   */
  public static final long DEFAULT_MAX_BYTES = 256L * 1024 * 1024;

  private static final int GZIP_MAGIC_FIRST = 0x1f;
  private static final int GZIP_MAGIC_SECOND = 0x8b;
  private static final int ZLIB_DEFLATE_METHOD = 0x78;

  private final int maxDepth;
  private final long maxBytes;

  public NbtReader() {
    this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_BYTES);
  }

  public NbtReader(int maxDepth, long maxBytes) {
    if (maxDepth < 1 || maxBytes < 1) {
      throw new IllegalArgumentException("NBT limits must be positive");
    }
    this.maxDepth = maxDepth;
    this.maxBytes = maxBytes;
  }

  /** Root tag of any type: its id, its name (empty for a network root) and its value. */
  record NamedTag(int type, String name, Object value) {}

  /** Reads an uncompressed named root, which must be a compound. */
  public NamedCompound read(InputStream uncompressed) throws IOException {
    Session session = new Session(uncompressed);
    NamedTag root =
        session.readRoot(true).orElseThrow(() -> session.malformed("no root tag (end tag)"));
    if (root.type() != NbtTag.COMPOUND) {
      throw session.malformed("root tag must be a compound, found tag id " + root.type());
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) root.value();
    return new NamedCompound(root.name(), value);
  }

  public NamedCompound readGzip(InputStream compressed) throws IOException {
    return read(new ByteArrayInputStream(Containers.gunzip(compressed, maxBytes)));
  }

  public NamedCompound readZlib(InputStream compressed) throws IOException {
    return read(new ByteArrayInputStream(Containers.inflateZlib(compressed, maxBytes)));
  }

  /**
   * Reads a root of any type, named (files) or unnamed (network, R20). Empty when the root is a
   * lone end tag.
   */
  Optional<NamedTag> readAnyRoot(InputStream uncompressed, boolean named) throws IOException {
    return new Session(uncompressed).readRoot(named);
  }

  /** Reads a file, detecting gzip, zlib or no compression from its first bytes. */
  public NamedCompound readFile(Path file) throws IOException {
    try (InputStream raw = new BufferedInputStream(Files.newInputStream(file))) {
      raw.mark(2);
      int first = raw.read();
      int second = raw.read();
      raw.reset();
      if (first == GZIP_MAGIC_FIRST && second == GZIP_MAGIC_SECOND) {
        return readGzip(raw);
      }
      if (first == ZLIB_DEFLATE_METHOD) {
        return readZlib(raw);
      }
      return read(raw);
    } catch (IOException e) {
      throw new IOException(file + ": " + e.getMessage(), e);
    }
  }

  /** State of one decoding: byte budget and the path of the tag being read, for messages. */
  private final class Session {

    private final BudgetInputStream budget;
    private final DataInputStream in;
    private final Deque<String> path = new ArrayDeque<>();

    Session(InputStream source) {
      this.budget = new BudgetInputStream(source, maxBytes);
      this.in = new DataInputStream(budget);
    }

    Optional<NamedTag> readRoot(boolean named) throws IOException {
      try {
        int type = in.readUnsignedByte();
        if (type == NbtTag.END) {
          return Optional.empty();
        }
        String name = named ? readString() : "";
        // Bytes after the root are ignored (R34).
        return Optional.of(new NamedTag(type, name, readPayload(type, 0)));
      } catch (EOFException e) {
        throw malformed("truncated data");
      }
    }

    private Object readPayload(int type, int depth) throws IOException {
      return switch (type) {
        case NbtTag.BYTE -> in.readByte();
        case NbtTag.SHORT -> in.readShort();
        case NbtTag.INT -> in.readInt();
        case NbtTag.LONG -> in.readLong();
        case NbtTag.FLOAT -> in.readFloat();
        case NbtTag.DOUBLE -> in.readDouble();
        case NbtTag.BYTE_ARRAY -> readByteArray();
        case NbtTag.STRING -> readString();
        case NbtTag.LIST -> readList(depth + 1);
        case NbtTag.COMPOUND -> readCompound(depth + 1);
        case NbtTag.INT_ARRAY -> readIntArray();
        case NbtTag.LONG_ARRAY -> readLongArray();
        default -> throw malformed("unknown tag id " + type);
      };
    }

    private Map<String, Object> readCompound(int depth) throws IOException {
      checkDepth(depth);
      Map<String, Object> entries = new LinkedHashMap<>();
      while (true) {
        int type = in.readUnsignedByte();
        if (type == NbtTag.END) {
          return Collections.unmodifiableMap(entries);
        }
        String name = readString();
        path.addLast(name);
        // A duplicate key keeps its first position and takes the last value (R27).
        entries.put(name, readPayload(type, depth));
        path.removeLast();
      }
    }

    private List<Object> readList(int depth) throws IOException {
      checkDepth(depth);
      int type = in.readUnsignedByte();
      int length = in.readInt();
      if (type > NbtTag.LONG_ARRAY) {
        throw malformed("unknown list element type " + type);
      }
      if (length <= 0) {
        return List.of();
      }
      if (type == NbtTag.END) {
        throw malformed("list of end tags with " + length + " elements");
      }
      reserve(length, NbtTag.minimumPayloadSize(type), "list");
      List<Object> elements = new ArrayList<>(Math.min(length, 1024));
      for (int i = 0; i < length; i++) {
        path.addLast("[" + i + "]");
        elements.add(readPayload(type, depth));
        path.removeLast();
      }
      return Collections.unmodifiableList(elements);
    }

    private byte[] readByteArray() throws IOException {
      int length = readArrayLength();
      reserve(length, Byte.BYTES, "byte array");
      byte[] values = new byte[length];
      in.readFully(values);
      return values;
    }

    private int[] readIntArray() throws IOException {
      int length = readArrayLength();
      reserve(length, Integer.BYTES, "int array");
      int[] values = new int[length];
      for (int i = 0; i < length; i++) {
        values[i] = in.readInt();
      }
      return values;
    }

    private long[] readLongArray() throws IOException {
      int length = readArrayLength();
      reserve(length, Long.BYTES, "long array");
      long[] values = new long[length];
      for (int i = 0; i < length; i++) {
        values[i] = in.readLong();
      }
      return values;
    }

    private int readArrayLength() throws IOException {
      int length = in.readInt();
      if (length < 0) {
        throw malformed("negative array length " + length);
      }
      return length;
    }

    private String readString() throws IOException {
      byte[] bytes = new byte[in.readUnsignedShort()];
      in.readFully(bytes);
      try {
        return ModifiedUtf8.decode(bytes);
      } catch (IllegalArgumentException e) {
        throw malformed("invalid string: " + e.getMessage());
      }
    }

    /** Refuses a length whose smallest possible encoding already exceeds the byte budget. */
    private void reserve(int count, int elementSize, String what) throws IOException {
      long needed = (long) count * elementSize;
      if (needed > budget.remaining()) {
        throw malformed(
            what + " of " + count + " elements exceeds the limit of " + maxBytes + " bytes");
      }
    }

    private void checkDepth(int depth) throws IOException {
      if (depth > maxDepth) {
        throw malformed("nesting deeper than " + maxDepth);
      }
    }

    IOException malformed(String problem) {
      return new IOException(
          "Malformed NBT at " + describePath() + " (byte " + budget.consumed() + "): " + problem);
    }

    private String describePath() {
      if (path.isEmpty()) {
        return "<root>";
      }
      StringBuilder text = new StringBuilder();
      for (String part : path) {
        if (part.startsWith("[")) {
          text.append(part);
        } else {
          text.append(text.isEmpty() ? "" : ".").append(part.isEmpty() ? "\"\"" : part);
        }
      }
      return text.toString();
    }
  }
}
