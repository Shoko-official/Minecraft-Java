package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtReader;
import dev.shoko.minecraft.core.nbt.binary.NbtWriter;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkCodec;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and saves the chunks of one dimension in its {@code region/} folder: region file, then NBT,
 * then {@link ChunkCodec} (AR-R19 to AR-R21, AR-R24, AR-R26 to AR-R30). Not thread safe; the thread
 * that ticks the world owns it.
 *
 * <p>The NBT written is one named root Compound with an empty name and nothing after it (AR-R24).
 * The NBT read is more tolerant (AR-R30): see {@link #load}.
 *
 * <p>The store remembers, until it is closed, the positions whose load was refused (the mark,
 * AR-R28). A marked position is never read again from the file: it keeps answering the same
 * refusal, and a save there fails. A position that was absent is never marked. A save at a position
 * that was never loaded is not checked: the caller loads before it saves.
 */
public final class AnvilChunkStore implements Closeable {

  private final Path regionFolder;
  private final WorldHeight height;
  private final ChunkCodec codec;
  private final RegionCompression compression;
  private final long decompressionLimit;
  private final Map<RegionPos, RegionFile> regions = new LinkedHashMap<>();
  private final Map<ChunkPos, ReadResult.Refused<Chunk>> marks = new HashMap<>();

  private AnvilChunkStore(
      Path regionFolder,
      WorldHeight height,
      ChunkCodec codec,
      RegionCompression compression,
      long decompressionLimit) {
    this.regionFolder = regionFolder;
    this.height = height;
    this.codec = codec;
    this.compression = compression;
    this.decompressionLimit = decompressionLimit;
  }

  /**
   * Opens the store of a dimension with {@link RegionCompression#DEFAULT_DECOMPRESSION_LIMIT}.
   *
   * @param regionFolder the {@code region/} folder, which may not exist yet
   * @param height vertical extent of the dimension
   * @param codec the chunk NBT codec
   * @param compression type used for writing
   * @return the store
   */
  public static AnvilChunkStore open(
      Path regionFolder, WorldHeight height, ChunkCodec codec, RegionCompression compression) {
    return open(
        regionFolder, height, codec, compression, RegionCompression.DEFAULT_DECOMPRESSION_LIMIT);
  }

  /**
   * Opens the store of a dimension.
   *
   * @param regionFolder the {@code region/} folder, which may not exist yet
   * @param height vertical extent of the dimension
   * @param codec the chunk NBT codec
   * @param compression type used for writing
   * @param decompressionLimit the most bytes the decompression of one record may produce (AR-R29)
   * @return the store
   * @throws IllegalArgumentException if the limit is negative
   */
  public static AnvilChunkStore open(
      Path regionFolder,
      WorldHeight height,
      ChunkCodec codec,
      RegionCompression compression,
      long decompressionLimit) {
    if (decompressionLimit < 0) {
      throw new IllegalArgumentException("negative decompression limit " + decompressionLimit);
    }
    return new AnvilChunkStore(regionFolder, height, codec, compression, decompressionLimit);
  }

  /**
   * Loads a chunk. Exactly one of the three issues of AR-R26; the causes and warnings are those of
   * {@link RegionFile#read(ChunkPos, long)}, of {@link ChunkCodec#decode}, and of these rules:
   *
   * <ul>
   *   <li>Absent, UNREADABLE_RECORD, with a warning: the NBT is malformed (unknown type, premature
   *       end, unterminated Compound, negative array length) or the root is not a Compound (AR-R30
   *       a, b).
   *   <li>Absent, POSITION_MISMATCH, with a warning: {@code xPos} or {@code zPos} differ from
   *       {@code pos} or are missing (AR-R21, CF-R35).
   *   <li>Refused, TOO_LARGE or TOO_DEEP: the NBT quota or depth is exceeded (AR-R30 e).
   *   <li>Present with a warning: a non-empty root name, ignored, ROOT_NAME_IGNORED; bytes after
   *       the root, ignored, TRAILING_BYTES (AR-R30 c, d).
   * </ul>
   *
   * A refusal marks the position (AR-R28); later loads of it answer the same refusal without
   * reading the file. Nothing is generated and the bytes are never touched. The message of a
   * refusal names the file, the index in the region, and the faulty field or section.
   *
   * @param pos absolute chunk position
   * @return the issue of the read
   * @throws IOException on a failure of the file system, which is none of the issues: nothing is
   *     generated, no mark is set (AR-R27)
   */
  public ReadResult<Chunk> load(ChunkPos pos) throws IOException {
    ReadResult.Refused<Chunk> marked = marks.get(pos);
    if (marked != null) {
      return marked;
    }
    ReadResult<Chunk> result =
        switch (region(pos).read(pos, decompressionLimit)) {
          case ReadResult.Absent<byte[]> absent ->
              new ReadResult.Absent<>(absent.cause(), absent.warnings());
          case ReadResult.Refused<byte[]> refused ->
              new ReadResult.Refused<>(refused.cause(), refused.message());
          case ReadResult.Present<byte[]> present -> decode(pos, present.value());
        };
    if (result instanceof ReadResult.Refused<Chunk> refused) {
      marks.put(pos, refused);
    }
    return result;
  }

  private ReadResult<Chunk> decode(ChunkPos pos, byte[] nbt) {
    List<ReadWarning> warnings = new ArrayList<>();
    CompoundTag root;
    try {
      Optional<NamedTag> named = NbtReader.of(NbtCompression.NONE).readNamed(nbt);
      if (named.isEmpty() || !(named.get().tag() instanceof CompoundTag compound)) {
        return unreadable(pos, "the NBT root is not a Compound");
      }
      root = compound;
      if (!named.get().name().isEmpty()) {
        warnings.add(
            new ReadWarning(
                WarningCode.ROOT_NAME_IGNORED,
                where(pos) + ": root name '" + named.get().name() + "' is ignored"));
      }
    } catch (NbtException e) {
      return switch (e.reason()) {
        case TOO_LARGE -> new ReadResult.Refused<>(RefusedCause.TOO_LARGE, where(pos) + ": " + e);
        case TOO_DEEP -> new ReadResult.Refused<>(RefusedCause.TOO_DEEP, where(pos) + ": " + e);
        default -> unreadable(pos, e.getMessage());
      };
    }
    if (NbtExtent.rootLength(nbt) < nbt.length) {
      warnings.add(
          new ReadWarning(
              WarningCode.TRAILING_BYTES, where(pos) + ": bytes after the NBT root are ignored"));
    }
    return decodeChunk(pos, root, warnings);
  }

  private ReadResult<Chunk> decodeChunk(
      ChunkPos pos, CompoundTag root, List<ReadWarning> warnings) {
    return switch (codec.decode(root, height)) {
      case ReadResult.Present<Chunk> present when !present.value().pos().equals(pos) -> {
        warnings.add(
            new ReadWarning(
                WarningCode.POSITION_MISMATCH,
                where(pos) + ": the NBT holds chunk " + present.value().pos()));
        yield new ReadResult.Absent<>(AbsentCause.POSITION_MISMATCH, List.copyOf(warnings));
      }
      case ReadResult.Present<Chunk> present -> {
        warnings.addAll(present.warnings());
        yield new ReadResult.Present<>(present.value(), List.copyOf(warnings));
      }
      case ReadResult.Absent<Chunk> absent -> {
        warnings.addAll(absent.warnings());
        yield new ReadResult.Absent<>(absent.cause(), List.copyOf(warnings));
      }
      case ReadResult.Refused<Chunk> refused ->
          new ReadResult.Refused<>(refused.cause(), where(pos) + ": " + refused.message());
    };
  }

  private ReadResult<Chunk> unreadable(ChunkPos pos, String detail) {
    return new ReadResult.Absent<>(
        AbsentCause.UNREADABLE_RECORD,
        List.of(new ReadWarning(WarningCode.UNREADABLE_RECORD, where(pos) + ": " + detail)));
  }

  private String where(ChunkPos pos) {
    return WorldLayout.regionFileName(pos.region()) + " index " + pos.indexInRegion();
  }

  private RegionFile region(ChunkPos pos) throws IOException {
    RegionPos position = pos.region();
    RegionFile region = regions.get(position);
    if (region == null) {
      region = RegionFile.open(regionFolder, position);
      regions.put(position, region);
    }
    return region;
  }

  /**
   * Saves a chunk, replacing what is stored at its position. Writes nothing for a marked position:
   * no record, no timestamp, no {@code .mcc} written or deleted, and the region file keeps its size
   * and content (AR-R28).
   *
   * @param chunk the chunk
   * @param timestampSeconds Unix seconds written in the region header, from the caller
   * @throws WriteOnRefusedPositionException if the position is marked: a load of it was refused
   * @throws IOException on a write failure
   */
  public void save(Chunk chunk, long timestampSeconds) throws IOException {
    ChunkPos pos = chunk.pos();
    ReadResult.Refused<Chunk> marked = marks.get(pos);
    if (marked != null) {
      throw new WriteOnRefusedPositionException(
          where(pos) + " was refused (" + marked.cause() + "), it cannot be written");
    }
    byte[] nbt;
    try {
      nbt = NbtWriter.of(NbtCompression.NONE).writeNamed(new NamedTag("", codec.encode(chunk)));
    } catch (NbtException e) {
      throw new IllegalStateException("chunk " + pos + " cannot be written as NBT", e);
    }
    region(pos).write(pos, nbt, compression, timestampSeconds);
  }

  /** Closes the region files and clears the marks: an external repair is seen on reopening. */
  @Override
  public void close() throws IOException {
    IOException failure = null;
    for (RegionFile region : regions.values()) {
      try {
        region.close();
      } catch (IOException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    regions.clear();
    marks.clear();
    if (failure != null) {
      throw failure;
    }
  }
}
