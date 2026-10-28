package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * File F1 of {@code core-nbt-vectors.md} (decompressed {@code structure/empty.nbt}, 148 bytes, as
 * printed in the spec) and the probe files p00 to p19 of PB1, rebuilt with the same byte patches as
 * {@code nbt_ref.py probes}. Offsets are those of the spec: element type of {@code entities} at 38,
 * End byte of the root at 147.
 */
final class ProbeFiles {

  static final String F1_HEX =
      "0a000009000473697a650300000003000000010000000100000001090008656e746974696573000000000009"
          + "0006626c6f636b730a00000001090003706f730300000003000000000000000000000000030005737461"
          + "7465000000000009000770616c657474650a000000010800026964000d6d696e6563726166743a616972"
          + "0003000b4461746156657273696f6e0000139f00";

  /** Offset of the element type byte of the {@code entities} list in F1. */
  static final int ENTITIES_ELEMENT_TYPE = 38;

  /** Offset of the End byte that closes the root compound of F1. */
  static final int ROOT_END = 147;

  /** Byte count of the Byte_Array of p19: 64 MiB. */
  static final int P19_ARRAY_LENGTH = 64 << 20;

  /** Element count of the List of Byte of p20: 20 000 000. */
  static final int P20_LIST_LENGTH = 20_000_000;

  private ProbeFiles() {}

  static byte[] f1() {
    return Hex.bytes(F1_HEX);
  }

  /** Decoded tree of F1, keys in file order (spec "Arbre attendu de F1"). */
  static CompoundTag f1Tree() {
    return compound(
        "size", list(intTag(1), intTag(1), intTag(1)),
        "entities", ListTag.empty(),
        "blocks", list(compound("pos", list(intTag(0), intTag(0), intTag(0)), "state", intTag(0))),
        "palette", list(compound("id", stringTag("minecraft:air"))),
        "DataVersion", intTag(5023));
  }

  /** Probe file p00 to p18 (decompressed bytes). p19 is only available as a stream. */
  static byte[] probe(String id) {
    return switch (id) {
      case "p00_base" -> f1();
      case "p01_root_name_x" -> Hex.concat(Hex.bytes("0a 00 01 78"), slice(3, 148));
      case "p02_entities_end_len_minus1" -> replaceEntitiesHeader("00 ff ff ff ff");
      case "p03_entities_byte_len_minus1" -> replaceEntitiesHeader("01 ff ff ff ff");
      case "p04_entities_end_len_1" -> replaceEntitiesHeader("00 00 00 00 01");
      case "p05_entities_byte_len_0" -> replaceEntitiesHeader("01 00 00 00 00");
      case "p06_trailing_byte" -> Hex.concat(f1(), new byte[1]);
      case "p07_truncated" -> slice(0, 147);
      case "p08_extra_unknown_type_13" -> insertInRoot(Hex.bytes("0d 00 01 71"));
      case "p09_extra_string_raw_nul" -> insertInRoot(Hex.bytes("08 00 01 71 00 01 00"));
      case "p10_extra_string_utf8_4byte" -> insertInRoot(Hex.bytes("08 00 01 71 00 04 f09f9880"));
      case "p11_extra_string_mutf8_pair" ->
          insertInRoot(Hex.bytes("08 00 01 71 00 06 eda0bd edb880"));
      case "p12_extra_string_lone_surrogate" -> insertInRoot(Hex.bytes("08 00 01 71 00 03 eda0bd"));
      case "p13_extra_bytearray_len_minus1" -> insertInRoot(Hex.bytes("07 00 01 71 ff ff ff ff"));
      case "p14_duplicate_palette_id_stone" -> duplicatePaletteId();
      case "p15_extra_deep_510" -> insertInRoot(deepListEntry(510));
      case "p16_extra_deep_511" -> insertInRoot(deepListEntry(511));
      case "p17_extra_deep_512" -> insertInRoot(deepListEntry(512));
      case "p18_extra_deep_513" -> insertInRoot(deepListEntry(513));
      default -> throw new IllegalArgumentException("unknown probe " + id);
    };
  }

  /** Probe p19: F1 with a Byte_Array {@code q} of 64 MiB zeros, streamed to save memory. */
  static InputStream p19Stream() {
    byte[] head = Hex.concat(slice(0, ROOT_END), Hex.bytes("07 00 01 71 04 00 00 00"));
    List<InputStream> parts =
        List.of(
            new ByteArrayInputStream(head),
            new ZeroInputStream(P19_ARRAY_LENGTH),
            new ByteArrayInputStream(new byte[1]));
    return new SequenceInputStream(Collections.enumeration(parts));
  }

  /**
   * Probe p20: F1 with a List of Byte {@code q} of 20 000 000 zeros (20 000 157 bytes), streamed:
   * "09 00 01 71", element type 01, length 01 31 2d 00.
   */
  static InputStream p20Stream() {
    byte[] head = Hex.concat(slice(0, ROOT_END), Hex.bytes("09 00 01 71 01 01 31 2d 00"));
    return concat(head, zeros(P20_LIST_LENGTH), new byte[1]);
  }

  /**
   * Container vector C09: C08 (F1 in a stored gzip member) whose low byte of {@code DataVersion}
   * goes from 9f to a0, with the original CRC-32 (R61).
   */
  static byte[] c09() {
    byte[] gzip = Containers.gzipStored(f1());
    byte[] name = "DataVersion".getBytes(StandardCharsets.US_ASCII);
    int lowByte = indexOf(f1(), name) + name.length + 3;
    gzip[15 + lowByte]++;
    return gzip;
  }

  /** A stream of {@code head}, then the given stream, then {@code tail}. */
  static InputStream concat(byte[] head, InputStream middle, byte[] tail) {
    List<InputStream> parts =
        List.of(new ByteArrayInputStream(head), middle, new ByteArrayInputStream(tail));
    return new SequenceInputStream(Collections.enumeration(parts));
  }

  /** A stream of {@code length} zero bytes, without allocating them. */
  static InputStream zeros(long length) {
    return new ZeroInputStream(length);
  }

  /** Offset of the first payload byte of the list {@code q} inserted at the end of the root. */
  static int insertedPayloadOffset() {
    return ROOT_END + 4;
  }

  private static byte[] slice(int from, int to) {
    return Arrays.copyOfRange(f1(), from, to);
  }

  private static byte[] replaceEntitiesHeader(String fiveBytes) {
    return Hex.concat(
        slice(0, ENTITIES_ELEMENT_TYPE),
        Hex.bytes(fiveBytes),
        slice(ENTITIES_ELEMENT_TYPE + 5, 148));
  }

  private static byte[] insertInRoot(byte[] entry) {
    return Hex.concat(slice(0, ROOT_END), entry, slice(ROOT_END, 148));
  }

  /** List entry {@code q} holding {@code n} nested lists, so n + 1 containers with the root. */
  private static byte[] deepListEntry(int n) {
    byte[] nested = BinaryVectors.nestedListsRoot(n);
    return Hex.concat(Hex.bytes("09 00 01 71"), Arrays.copyOfRange(nested, 3, nested.length));
  }

  private static byte[] duplicatePaletteId() {
    byte[] air = "minecraft:air".getBytes(StandardCharsets.US_ASCII);
    int paletteEntryEnd = indexOf(f1(), air) + air.length;
    byte[] stone = "minecraft:stone".getBytes(StandardCharsets.US_ASCII);
    byte[] entry =
        Hex.concat(Hex.bytes("08 00 02 69 64 00"), new byte[] {(byte) stone.length}, stone);
    return Hex.concat(slice(0, paletteEntryEnd), entry, slice(paletteEntryEnd, 148));
  }

  private static int indexOf(byte[] data, byte[] pattern) {
    outer:
    for (int i = 0; i + pattern.length <= data.length; i++) {
      for (int j = 0; j < pattern.length; j++) {
        if (data[i + j] != pattern[j]) {
          continue outer;
        }
      }
      return i;
    }
    throw new IllegalArgumentException("pattern not found");
  }

  /** Yields a fixed number of zero bytes. */
  private static final class ZeroInputStream extends InputStream {

    private long remaining;

    ZeroInputStream(long length) {
      this.remaining = length;
    }

    @Override
    public int read() {
      if (remaining == 0) {
        return -1;
      }
      remaining--;
      return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
      if (length == 0) {
        return 0;
      }
      if (remaining == 0) {
        return -1;
      }
      int count = (int) Math.min(length, remaining);
      Arrays.fill(buffer, offset, offset + count, (byte) 0);
      remaining -= count;
      return count;
    }
  }
}
