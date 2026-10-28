package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.chunk.ChunkFormatException;
import dev.shoko.minecraft.world.chunk.ChunkFormatException.Reason;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * Converts the roots of {@code entities/} and {@code poi/} chunks to and from NBT (AR-R22, AR-R23,
 * provisional). Unknown keys are ignored when reading; only the listed keys are written.
 */
public final class AuxiliaryChunkCodec {

  private static final String DATA_VERSION = "DataVersion";

  private AuxiliaryChunkCodec() {}

  /**
   * Reads the root of an {@code entities/} chunk.
   *
   * @param root the root Compound, after decompression
   * @return the entities chunk
   * @throws ChunkFormatException if {@code DataVersion}, {@code Position} (two ints) or {@code
   *     Entities} is missing or mistyped
   */
  public static EntitiesChunk decodeEntities(CompoundTag root) throws ChunkFormatException {
    int dataVersion = field(root, DATA_VERSION, IntTag.class, DATA_VERSION).value();
    int[] position = field(root, "Position", IntArrayTag.class, "Position").toArray();
    if (position.length != 2) {
      throw new ChunkFormatException(
          Reason.INVALID_VALUE,
          "Position",
          "Position has " + position.length + " ints, 2 expected");
    }
    ListTag list = field(root, "Entities", ListTag.class, "Entities");
    List<CompoundTag> entities = new ArrayList<>(list.elements().size());
    for (int index = 0; index < list.elements().size(); index++) {
      entities.add(
          element(list.elements().get(index), CompoundTag.class, "Entities[" + index + "]"));
    }
    return new EntitiesChunk(dataVersion, new ChunkPos(position[0], position[1]), entities);
  }

  /**
   * Writes the root of an {@code entities/} chunk.
   *
   * @param chunk the entities chunk
   * @return the root Compound
   */
  public static CompoundTag encodeEntities(EntitiesChunk chunk) {
    SequencedMap<String, NbtTag> root = new LinkedHashMap<>();
    root.put(DATA_VERSION, new IntTag(chunk.dataVersion()));
    root.put("Position", IntArrayTag.of(chunk.position().x(), chunk.position().z()));
    root.put("Entities", ListTag.of(chunk.entities()));
    return CompoundTag.of(root);
  }

  /**
   * Reads the root of a {@code poi/} chunk.
   *
   * @param root the root Compound, after decompression
   * @return the points of interest
   * @throws ChunkFormatException if {@code DataVersion} or {@code Sections} is missing or mistyped,
   *     a section key is not a decimal integer, or a record is malformed
   */
  public static PoiChunk decodePoi(CompoundTag root) throws ChunkFormatException {
    int dataVersion = field(root, DATA_VERSION, IntTag.class, DATA_VERSION).value();
    CompoundTag sections = field(root, "Sections", CompoundTag.class, "Sections");
    SequencedMap<Integer, PoiSection> result = new LinkedHashMap<>();
    for (var entry : sections.entries().entrySet()) {
      String path = "Sections." + entry.getKey();
      int y = sectionY(entry.getKey(), path);
      if (result.containsKey(y)) {
        throw new ChunkFormatException(Reason.INVALID_VALUE, path, path + " repeats section " + y);
      }
      CompoundTag section = element(entry.getValue(), CompoundTag.class, path);
      result.put(y, poiSection(section, path));
    }
    return new PoiChunk(dataVersion, result);
  }

  private static int sectionY(String key, String path) throws ChunkFormatException {
    try {
      return Integer.parseInt(key);
    } catch (NumberFormatException e) {
      throw new ChunkFormatException(
          Reason.INVALID_VALUE, path, "section key '" + key + "' is not a decimal integer", e);
    }
  }

  private static PoiSection poiSection(CompoundTag section, String path)
      throws ChunkFormatException {
    boolean valid = field(section, "Valid", ByteTag.class, path + ".Valid").value() != 0;
    ListTag records = field(section, "Records", ListTag.class, path + ".Records");
    List<PoiRecord> result = new ArrayList<>(records.elements().size());
    for (int index = 0; index < records.elements().size(); index++) {
      String recordPath = path + ".Records[" + index + "]";
      CompoundTag record = element(records.elements().get(index), CompoundTag.class, recordPath);
      int[] pos = field(record, "pos", IntArrayTag.class, recordPath + ".pos").toArray();
      if (pos.length != 3) {
        throw new ChunkFormatException(
            Reason.INVALID_VALUE,
            recordPath + ".pos",
            "pos has " + pos.length + " ints, 3 expected");
      }
      result.add(
          new PoiRecord(
              new BlockPos(pos[0], pos[1], pos[2]),
              field(record, "type", StringTag.class, recordPath + ".type").value(),
              field(record, "free_tickets", IntTag.class, recordPath + ".free_tickets").value()));
    }
    return new PoiSection(valid, result);
  }

  /**
   * Writes the root of a {@code poi/} chunk.
   *
   * @param chunk the points of interest
   * @return the root Compound
   */
  public static CompoundTag encodePoi(PoiChunk chunk) {
    SequencedMap<String, NbtTag> sections = new LinkedHashMap<>();
    chunk
        .sections()
        .forEach(
            (y, section) -> {
              List<CompoundTag> records = new ArrayList<>(section.records().size());
              section.records().forEach(record -> records.add(poiRecord(record)));
              SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
              entries.put("Valid", new ByteTag((byte) (section.valid() ? 1 : 0)));
              entries.put("Records", ListTag.of(records));
              sections.put(Integer.toString(y), CompoundTag.of(entries));
            });
    SequencedMap<String, NbtTag> root = new LinkedHashMap<>();
    root.put(DATA_VERSION, new IntTag(chunk.dataVersion()));
    root.put("Sections", CompoundTag.of(sections));
    return CompoundTag.of(root);
  }

  private static CompoundTag poiRecord(PoiRecord record) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    BlockPos pos = record.pos();
    entries.put("pos", IntArrayTag.of(pos.x(), pos.y(), pos.z()));
    entries.put("type", new StringTag(record.type()));
    entries.put("free_tickets", new IntTag(record.freeTickets()));
    return CompoundTag.of(entries);
  }

  private static <T extends NbtTag> T field(
      CompoundTag parent, String key, Class<T> type, String path) throws ChunkFormatException {
    NbtTag tag = parent.entries().get(key);
    if (tag == null) {
      throw new ChunkFormatException(Reason.MISSING_FIELD, path, path + " is missing");
    }
    return element(tag, type, path);
  }

  private static <T extends NbtTag> T element(NbtTag tag, Class<T> type, String path)
      throws ChunkFormatException {
    if (!type.isInstance(tag)) {
      throw new ChunkFormatException(
          Reason.WRONG_TYPE,
          path,
          path + " is a " + tag.type() + ", expected " + type.getSimpleName());
    }
    return type.cast(tag);
  }
}
