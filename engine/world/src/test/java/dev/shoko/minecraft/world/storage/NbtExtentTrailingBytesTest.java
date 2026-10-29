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

  // The shapes of a document that stress the measure of the root.
  static Stream<Arguments> documents() {
    Stream<Arguments> negativeFixedLists =
        IntStream.rangeClosed(TYPE_BYTE, 6)
            .boxed()
            .flatMap(
                type ->
                    Stream.of(
                        Arguments.of(
                            "list of type " + type + " of length -1",
                            tag(TYPE_LIST, "extra", listHeader(type, -1))),
                        Arguments.of(
                            "list of type " + type + " of length MIN_VALUE",
                            tag(TYPE_LIST, "extra", listHeader(type, Integer.MIN_VALUE)))));
    Stream<Arguments> others =
        Stream.of(
            Arguments.of(
                "list of Int of length 0", tag(TYPE_LIST, "extra", listHeader(TYPE_INT, 0))),
            Arguments.of(
                "list of Int of length 1",
                tag(
                    TYPE_LIST,
                    "extra",
                    out -> {
                      listHeader(TYPE_INT, 1).write(out);
                      out.writeInt(7);
                    })),
            Arguments.of(
                "list of Compound of length -1",
                tag(TYPE_LIST, "extra", listHeader(TYPE_COMPOUND, -1))),
            Arguments.of("list of End of length -1", tag(TYPE_LIST, "extra", listHeader(0, -1))),
            Arguments.of("string of 32767 bytes", tag(TYPE_STRING, "extra", stringOf(32_767))),
            Arguments.of("string of 32768 bytes", tag(TYPE_STRING, "extra", stringOf(32_768))),
            Arguments.of("string of 40000 bytes", tag(TYPE_STRING, "extra", stringOf(40_000))),
            Arguments.of("string of 65535 bytes", tag(TYPE_STRING, "extra", stringOf(65_535))),
            Arguments.of(
                "key of 40000 bytes", tag(TYPE_BYTE, "k".repeat(40_000), out -> out.writeByte(1))),
            Arguments.of(
                "key of 65535 bytes", tag(TYPE_BYTE, "k".repeat(65_535), out -> out.writeByte(1))),
            Arguments.of(
                "list of two lists of Int of length -1",
                tag(
                    TYPE_LIST,
                    "extra",
                    out -> {
                      listHeader(TYPE_LIST, 2).write(out);
                      listHeader(TYPE_INT, -1).write(out);
                      listHeader(TYPE_INT, -1).write(out);
                    })),
            Arguments.of(
                "list of list of list of Long of length -1",
                tag(
                    TYPE_LIST,
                    "extra",
                    out -> {
                      listHeader(TYPE_LIST, 1).write(out);
                      listHeader(TYPE_LIST, 1).write(out);
                      listHeader(4, -1).write(out);
                    })),
            Arguments.of(
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
    Stream<Arguments> sized =
        Stream.concat(
            IntStream.rangeClosed(TYPE_BYTE, 6)
                .mapToObj(
                    type ->
                        Arguments.of(
                            "list of type " + type + " of length 3",
                            tag(
                                TYPE_LIST,
                                "extra",
                                out -> {
                                  listHeader(type, 3).write(out);
                                  out.write(new byte[3 * FIXED_SIZE[type]]);
                                }))),
            Stream.of(
                Arguments.of(
                    "byte array of 3",
                    tag(
                        7,
                        "extra",
                        out -> {
                          out.writeInt(3);
                          out.write(new byte[3]);
                        })),
                Arguments.of(
                    "int array of 3",
                    tag(
                        11,
                        "extra",
                        out -> {
                          out.writeInt(3);
                          out.write(new byte[12]);
                        })),
                Arguments.of(
                    "long array of 3",
                    tag(
                        12,
                        "extra",
                        out -> {
                          out.writeInt(3);
                          out.write(new byte[24]);
                        }))));
    return Stream.of(negativeFixedLists, others, sized).flatMap(stream -> stream);
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
