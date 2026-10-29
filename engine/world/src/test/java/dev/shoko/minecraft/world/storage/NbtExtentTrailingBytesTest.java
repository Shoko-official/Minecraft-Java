package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Hex;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * AR-R30 d: the bytes after the root are ignored with the warning TRAILING_BYTES. The store finds
 * them by measuring the root; the measure must follow the NBT reader on the odd shapes it accepts:
 * a negative list length is an empty list (core-nbt R25), a string length is unsigned.
 */
class NbtExtentTrailingBytesTest {

  private static final byte[] TRAILING = {0, 1, 2, 3, 4};
  private static final int TYPE_BYTE = 1;
  private static final int TYPE_INT = 3;
  private static final int TYPE_STRING = 8;
  private static final int TYPE_LIST = 9;
  private static final int TYPE_COMPOUND = 10;
  // Payload size of the types 1 to 6, from the NBT format: byte, short, int, long, float, double.
  private static final int[] FIXED_SIZE = {0, 1, 2, 4, 8, 4, 8};

  @TempDir Path folder;

  /** The chunk A root with one more entry, the bytes of its tag given whole (type included). */
  private static byte[] chunkWith(byte[] extraTag) {
    byte[] plain = ChunkNbt.chunkA("bedrock");
    return Hex.concat(Arrays.copyOf(plain, plain.length - 1), extraTag, new byte[] {0});
  }

  private interface Body {
    void write(DataOutputStream out) throws IOException;
  }

  private static byte[] tag(int type, String key, Body payload) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bytes);
      out.writeByte(type);
      byte[] name = key.getBytes(StandardCharsets.US_ASCII);
      out.writeShort(name.length);
      out.write(name);
      payload.write(out);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Body listHeader(int elementType, int count) {
    return out -> {
      out.writeByte(elementType);
      out.writeInt(count);
    };
  }

  private static Body stringOf(int length) {
    return out -> {
      out.writeShort(length);
      out.write("a".repeat(length).getBytes(StandardCharsets.US_ASCII));
    };
  }

  private static Arguments shape(String name, byte[] tag) {
    return Arguments.of(name, tag);
  }

  private static Arguments listShape(String name, int elementType, int count) {
    return shape(name, tag(TYPE_LIST, "extra", listHeader(elementType, count)));
  }

  // The shapes of a document that stress the measure of the root.
  static Stream<Arguments> documents() {
    return Stream.of(negativeLists(), emptyAndOddLists(), strings(), nested(), sized())
        .flatMap(stream -> stream);
  }

  private static Stream<Arguments> negativeLists() {
    return IntStream.rangeClosed(TYPE_BYTE, 6)
        .boxed()
        .flatMap(
            type ->
                Stream.of(
                    listShape("list of type " + type + " of length -1", type, -1),
                    listShape(
                        "list of type " + type + " of length MIN_VALUE", type, Integer.MIN_VALUE)));
  }

  private static Stream<Arguments> emptyAndOddLists() {
    return Stream.of(
        listShape("list of Int of length 0", TYPE_INT, 0),
        listShape("list of Compound of length -1", TYPE_COMPOUND, -1),
        listShape("list of End of length -1", 0, -1),
        shape(
            "list of Int of length 1",
            tag(
                TYPE_LIST,
                "extra",
                out -> {
                  listHeader(TYPE_INT, 1).write(out);
                  out.writeInt(7);
                })));
  }

  private static Stream<Arguments> strings() {
    return Stream.of(
        shape("string of 32767 bytes", tag(TYPE_STRING, "extra", stringOf(32_767))),
        shape("string of 32768 bytes", tag(TYPE_STRING, "extra", stringOf(32_768))),
        shape("string of 40000 bytes", tag(TYPE_STRING, "extra", stringOf(40_000))),
        shape("string of 65535 bytes", tag(TYPE_STRING, "extra", stringOf(65_535))),
        shape("key of 40000 bytes", tag(TYPE_BYTE, "k".repeat(40_000), out -> out.writeByte(1))),
        shape("key of 65535 bytes", tag(TYPE_BYTE, "k".repeat(65_535), out -> out.writeByte(1))));
  }

  private static Stream<Arguments> nested() {
    return Stream.of(
        shape(
            "list of two lists of Int of length -1",
            tag(
                TYPE_LIST,
                "extra",
                out -> {
                  listHeader(TYPE_LIST, 2).write(out);
                  listHeader(TYPE_INT, -1).write(out);
                  listHeader(TYPE_INT, -1).write(out);
                })),
        shape(
            "list of list of list of Long of length -1",
            tag(
                TYPE_LIST,
                "extra",
                out -> {
                  listHeader(TYPE_LIST, 1).write(out);
                  listHeader(TYPE_LIST, 1).write(out);
                  listHeader(4, -1).write(out);
                })),
        shape(
            "list of Compound holding a negative list and a long string",
            tag(
                TYPE_LIST,
                "extra",
                out -> {
                  listHeader(TYPE_COMPOUND, 2).write(out);
                  for (int element = 0; element < 2; element++) {
                    out.write(tag(TYPE_LIST, "inner", listHeader(TYPE_INT, -1)));
                    out.write(tag(TYPE_STRING, "text", stringOf(33_000)));
                    out.writeByte(0);
                  }
                })));
  }

  private static Stream<Arguments> sized() {
    return Stream.concat(
        IntStream.rangeClosed(TYPE_BYTE, 6)
            .mapToObj(
                type ->
                    shape(
                        "list of type " + type + " of length 3",
                        tag(
                            TYPE_LIST,
                            "extra",
                            out -> {
                              listHeader(type, 3).write(out);
                              out.write(new byte[3 * FIXED_SIZE[type]]);
                            }))),
        Stream.of(
            arrayShape("byte array of 3", 7, 3),
            arrayShape("int array of 3", 11, 12),
            arrayShape("long array of 3", 12, 24)));
  }

  // An array tag of 3 elements whose payload takes this many bytes.
  private static Arguments arrayShape(String name, int type, int payloadBytes) {
    return shape(
        name,
        tag(
            type,
            "extra",
            out -> {
              out.writeInt(3);
              out.write(new byte[payloadBytes]);
            }));
  }

  private Chunk loadClean(byte[] nbt, boolean expectTrailingWarning) throws Exception {
    try (RegionFile file = RegionFile.open(folder, CHUNK_A.region())) {
      file.write(CHUNK_A, nbt, RegionCompression.ZLIB, 1L);
    }
    try (AnvilChunkStore store =
        AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      var present = Chunks.present(store.load(CHUNK_A));
      assertThat(present.warnings())
          .extracting(ReadWarning::code)
          .isEqualTo(expectTrailingWarning ? List.of(WarningCode.TRAILING_BYTES) : List.of());
      return present.value();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("documents")
  void bytesAfterARootWithThisShapeGiveTheTrailingBytesWarning(String shape, byte[] extraTag)
      throws Exception {
    // spec: AR-R30 d (bytes after the root are ignored with a warning), core-nbt R25 and R34
    byte[] document = chunkWith(extraTag);

    assertThat(NbtExtent.rootLength(document)).as(shape).isEqualTo(document.length);
    byte[] withTrailing = Hex.concat(document, TRAILING);
    assertThat(NbtExtent.rootLength(withTrailing)).as(shape).isEqualTo(document.length);

    assertThat(loadClean(withTrailing, true).pos()).isEqualTo(CHUNK_A);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("documents")
  void aRootWithThisShapeAndNothingAfterItGivesNoWarning(String shape, byte[] extraTag)
      throws Exception {
    // spec: AR-R30 d (the control of the test above: the measure is not short of the root)
    byte[] document = chunkWith(extraTag);

    assertThat(loadClean(document, false).pos()).isEqualTo(CHUNK_A);
  }

  @Test
  void aRootNameLongerThan32767BytesIsMeasuredAsUnsigned() throws Exception {
    // spec: AR-R30 c and d, core-nbt R15: the length of a name is an unsigned 16 bit value
    byte[] plain = ChunkNbt.chunkA("bedrock");
    byte[] name = "n".repeat(40_000).getBytes(StandardCharsets.US_ASCII);
    byte[] document =
        Hex.concat(
            new byte[] {10, (byte) (40_000 >> 8), (byte) 40_000},
            name,
            Arrays.copyOfRange(plain, 3, plain.length));
    byte[] withTrailing = Hex.concat(document, TRAILING);

    assertThat(NbtExtent.rootLength(withTrailing)).isEqualTo(document.length);
    try (RegionFile file = RegionFile.open(folder, CHUNK_A.region())) {
      file.write(CHUNK_A, withTrailing, RegionCompression.ZLIB, 1L);
    }
    try (AnvilChunkStore store =
        AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      assertThat(Chunks.present(store.load(CHUNK_A)).warnings())
          .extracting(ReadWarning::code)
          .containsExactlyInAnyOrder(WarningCode.ROOT_NAME_IGNORED, WarningCode.TRAILING_BYTES);
    }
  }
}
