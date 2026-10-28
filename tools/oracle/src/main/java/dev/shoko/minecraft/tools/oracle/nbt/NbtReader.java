package dev.shoko.minecraft.tools.oracle.nbt;

import java.io.BufferedInputStream;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Minimal read-only NBT decoder. It shares no code with the engine on purpose: a reading bug shared
 * by the engine and the oracle would make the comparison blind (docs/VERIFICATION.md).
 *
 * <p>Tags map to plain Java values: byte {@link Byte}, short {@link Short}, int {@link Integer},
 * long {@link Long}, float {@link Float}, double {@link Double}, string {@link String}, byte, int
 * and long arrays {@code byte[]}, {@code int[]}, {@code long[]}, list an unmodifiable {@link List},
 * compound an unmodifiable {@link Map} in file order. The reader is strict: duplicate compound
 * keys, negative lengths, unknown tag ids, non-empty lists of the end tag and trailing bytes after
 * the root are rejected; compressed input is read to its end so that its checksum is verified.
 */
public final class NbtReader {

  /** Nesting limit of the reference format for compounds and lists. */
  public static final int DEFAULT_MAX_DEPTH = 512;

  /** Limit on the decoded size, which also bounds decompression bombs. */
  public static final long DEFAULT_MAX_BYTES = 64L * 1024 * 1024;

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

  /** Reads an uncompressed named root compound. */
  public NamedCompound read(InputStream uncompressed) throws IOException {
    return new Session(uncompressed).readRoot();
  }

  public NamedCompound readGzip(InputStream compressed) throws IOException {
    return read(new GZIPInputStream(compressed));
  }

  public NamedCompound readZlib(InputStream compressed) throws IOException {
    return read(new InflaterInputStream(compressed));
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

    NamedCompound readRoot() throws IOException {
      try {
        int type = in.readUnsignedByte();
        if (type != NbtTag.COMPOUND) {
          throw malformed("root tag must be a compound, found tag id " + type);
        }
        String name = in.readUTF();
        NamedCompound root = new NamedCompound(name, readCompound(1));
        // Reading to the end also makes gzip and zlib verify their checksums.
        if (in.read() != -1) {
          throw malformed("trailing data after the root compound");
        }
        return root;
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
        case NbtTag.STRING -> in.readUTF();
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
        String name = in.readUTF();
        if (entries.containsKey(name)) {
          throw malformed("duplicate key '" + name + "'");
        }
        path.addLast(name);
        entries.put(name, readPayload(type, depth));
        path.removeLast();
      }
    }

    private List<Object> readList(int depth) throws IOException {
      checkDepth(depth);
      int type = in.readUnsignedByte();
      int length = readLength();
      if (type == NbtTag.END && length > 0) {
        throw malformed("list of end tags with " + length + " elements");
      }
      if (type != NbtTag.END) {
        reserve(length, NbtTag.minimumPayloadSize(type), "list");
      }
      List<Object> elements = new ArrayList<>(Math.min(length, 1024));
      for (int i = 0; i < length; i++) {
        path.addLast("[" + i + "]");
        elements.add(readPayload(type, depth));
        path.removeLast();
      }
      return Collections.unmodifiableList(elements);
    }

    private byte[] readByteArray() throws IOException {
      int length = readLength();
      reserve(length, Byte.BYTES, "byte array");
      byte[] values = new byte[length];
      in.readFully(values);
      return values;
    }

    private int[] readIntArray() throws IOException {
      int length = readLength();
      reserve(length, Integer.BYTES, "int array");
      int[] values = new int[length];
      for (int i = 0; i < length; i++) {
        values[i] = in.readInt();
      }
      return values;
    }

    private long[] readLongArray() throws IOException {
      int length = readLength();
      reserve(length, Long.BYTES, "long array");
      long[] values = new long[length];
      for (int i = 0; i < length; i++) {
        values[i] = in.readLong();
      }
      return values;
    }

    private int readLength() throws IOException {
      int length = in.readInt();
      if (length < 0) {
        throw malformed("negative length " + length);
      }
      return length;
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

    private IOException malformed(String problem) {
      return new IOException("Malformed NBT at " + describePath() + ": " + problem);
    }

    private String describePath() {
      if (path.isEmpty()) {
        return "<root>";
      }
      StringBuilder text = new StringBuilder();
      for (Iterator<String> it = path.iterator(); it.hasNext(); ) {
        String part = it.next();
        if (!text.isEmpty() && !part.startsWith("[")) {
          text.append('.');
        }
        text.append(part);
      }
      return text.toString();
    }
  }
}
