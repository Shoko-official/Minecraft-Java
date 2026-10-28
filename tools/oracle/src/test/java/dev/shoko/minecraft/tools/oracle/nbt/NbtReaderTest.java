package dev.shoko.minecraft.tools.oracle.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class NbtReaderTest {

  private static NamedCompound read(byte[] bytes) throws IOException {
    return new NbtReader().read(new ByteArrayInputStream(bytes));
  }

  private static Map<String, Object> compound(Object value) {
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) value;
    return map;
  }

  @Test
  void readsTheHelloWorldVectorOfThePublicSpecification() throws IOException {
    byte[] bytes =
        HexFormat.of()
            .parseHex(
                "0a000b68656c6c6f20776f726c64" + "0800046e616d65000942616e616e72616d61" + "00");

    NamedCompound root = read(bytes);

    assertThat(root.name()).isEqualTo("hello world");
    assertThat(root.value()).containsExactly(Map.entry("name", "Bananrama"));
  }

  @Test
  void readsEveryTagTypeAndKeepsFileOrder() throws IOException {
    byte[] bytes =
        new NbtBytes()
            .named(10, "")
            .named(1, "b")
            .u8(0xff)
            .named(2, "s")
            .i16(-2)
            .named(3, "i")
            .i32(-3)
            .named(4, "l")
            .i64(Long.MIN_VALUE)
            .named(5, "f")
            .f32(1.5f)
            .named(6, "d")
            .f64(-0.25)
            .named(7, "ba")
            .i32(2)
            .raw(1, 0x80)
            .named(8, "str")
            .utf("stone")
            .named(9, "list")
            .u8(10)
            .i32(2)
            .named(3, "x")
            .i32(1)
            .end()
            .end()
            .named(11, "ia")
            .i32(2)
            .i32(-1)
            .i32(7)
            .named(12, "la")
            .i32(1)
            .i64(-5)
            .named(9, "empty")
            .u8(0)
            .i32(0)
            .end()
            .toByteArray();

    Map<String, Object> root = read(bytes).value();

    assertThat(root.keySet())
        .containsExactly("b", "s", "i", "l", "f", "d", "ba", "str", "list", "ia", "la", "empty");
    assertThat(root.get("b")).isEqualTo((byte) -1);
    assertThat(root.get("s")).isEqualTo((short) -2);
    assertThat(root.get("i")).isEqualTo(-3);
    assertThat(root.get("l")).isEqualTo(Long.MIN_VALUE);
    assertThat(root.get("f")).isEqualTo(1.5f);
    assertThat(root.get("d")).isEqualTo(-0.25);
    assertThat((byte[]) root.get("ba")).containsExactly(1, -128);
    assertThat(root.get("str")).isEqualTo("stone");
    assertThat((List<?>) root.get("list")).hasSize(2);
    assertThat(compound(((List<?>) root.get("list")).get(0))).containsExactly(Map.entry("x", 1));
    assertThat(compound(((List<?>) root.get("list")).get(1))).isEmpty();
    assertThat((int[]) root.get("ia")).containsExactly(-1, 7);
    assertThat((long[]) root.get("la")).containsExactly(-5L);
    assertThat((List<?>) root.get("empty")).isEmpty();
  }

  @Test
  void decodesModifiedUtf8NullAndSupplementaryCharacters() throws IOException {
    // Modified UTF-8: U+0000 is C0 80, U+1F600 is the surrogate pair D83D DE00 encoded separately.
    byte[] bytes =
        new NbtBytes()
            .named(10, "")
            .named(8, "t")
            .i16(8)
            .raw(0xc0, 0x80, 0xed, 0xa0, 0xbd, 0xed, 0xb8, 0x80)
            .end()
            .toByteArray();

    assertThat(read(bytes).value().get("t"))
        .isEqualTo("\u0000" + new String(Character.toChars(0x1F600)));
  }

  @Test
  void detectsGzipZlibAndUncompressedFiles(@TempDir Path dir) throws IOException {
    byte[] plain = NbtBytes.rootWithInt("v", 42);
    Files.write(dir.resolve("plain.nbt"), plain);
    Files.write(dir.resolve("gzip.nbt"), NbtBytes.gzip(plain));
    Files.write(dir.resolve("zlib.nbt"), NbtBytes.zlib(plain));

    for (String name : List.of("plain.nbt", "gzip.nbt", "zlib.nbt")) {
      assertThat(new NbtReader().readFile(dir.resolve(name)).value()).containsEntry("v", 42);
    }
  }

  @Test
  void rejectsTruncatedDataAndNamesWhereItStopped() {
    byte[] whole = NbtBytes.rootWithInt("v", 42);
    byte[] truncated = Arrays.copyOf(whole, whole.length - 3);

    assertThatThrownBy(() -> read(truncated))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Malformed NBT at v (byte ")
        .hasMessageContaining("truncated");
  }

  @Test
  void ignoresBytesAfterTheRoot() throws IOException {
    byte[] bytes =
        new NbtBytes().named(10, "").named(1, "a").u8(1).end().raw(0, 7, 9).toByteArray();

    assertThat(read(bytes).value()).containsExactly(Map.entry("a", (byte) 1));
  }

  @Test
  void showsTheRootWhenThePathIsEmpty() {
    byte[] bytes = new NbtBytes().named(13, "").toByteArray();

    assertThatThrownBy(() -> new NbtReader().readAnyRoot(new ByteArrayInputStream(bytes), true))
        .hasMessageStartingWith("Malformed NBT at <root> (byte ")
        .hasMessageContaining("unknown tag id 13");
  }

  @Test
  void rejectsARootThatIsNotACompound() {
    byte[] bytes = new NbtBytes().named(3, "x").i32(1).toByteArray();

    assertThatThrownBy(() -> read(bytes)).hasMessageContaining("root tag must be a compound");
  }

  @Test
  void locatesAnUnknownTagIdByItsPath() {
    byte[] bytes =
        new NbtBytes()
            .named(10, "")
            .named(9, "sections")
            .u8(10)
            .i32(2)
            .end()
            .named(13, "bad")
            .end()
            .end()
            .toByteArray();

    assertThatThrownBy(() -> read(bytes))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("sections[1].bad")
        .hasMessageContaining("unknown tag id 13");
  }

  @Test
  void keepsTheLastValueOfADuplicateKeyAtItsFirstPosition() throws IOException {
    byte[] bytes =
        new NbtBytes()
            .named(10, "")
            .named(3, "a")
            .i32(1)
            .named(3, "b")
            .i32(5)
            .named(3, "a")
            .i32(2)
            .end()
            .toByteArray();

    assertThat(read(bytes).value()).containsExactly(Map.entry("a", 2), Map.entry("b", 5));
  }

  @Test
  void rejectsNegativeArrayLengths() {
    for (int type : new int[] {7, 11, 12}) {
      byte[] bytes = new NbtBytes().named(10, "").named(type, "arr").i32(-1).end().toByteArray();

      assertThatThrownBy(() -> read(bytes))
          .as("array type %d", type)
          .hasMessageContaining("negative array length -1");
    }
  }

  @Test
  void readsANegativeListLengthAsAnEmptyList() throws IOException {
    byte[] bytes = new NbtBytes().named(10, "").named(9, "l").u8(0).i32(-5).end().toByteArray();

    assertThat(read(bytes).value().get("l")).isEqualTo(List.of());
  }

  @Test
  void rejectsAnUnknownListElementTypeEvenWhenEmpty() {
    byte[] bytes = new NbtBytes().named(10, "").named(9, "l").u8(99).i32(0).end().toByteArray();

    assertThatThrownBy(() -> read(bytes))
        .hasMessageContaining("at l (byte ")
        .hasMessageContaining("unknown list element type 99");
  }

  @Test
  void rejectsANonEmptyListOfEndTags() {
    byte[] bytes = new NbtBytes().named(10, "").named(9, "l").u8(0).i32(3).end().toByteArray();

    assertThatThrownBy(() -> read(bytes)).hasMessageContaining("list of end tags");
  }

  @Test
  void rejectsInvalidStringBytesWithTheirPath() {
    byte[] rawNul = new NbtBytes().named(10, "").named(8, "s").i16(1).raw(0).end().toByteArray();
    byte[] fourBytes =
        new NbtBytes()
            .named(10, "")
            .named(8, "s")
            .i16(4)
            .raw(0xf0, 0x9f, 0x98, 0x80)
            .end()
            .toByteArray();
    byte[] cut =
        new NbtBytes().named(10, "").named(8, "s").i16(2).raw(0xe2, 0x82).end().toByteArray();

    assertThatThrownBy(() -> read(rawNul))
        .hasMessageContaining("Malformed NBT at s")
        .hasMessageContaining("raw 00 byte, byte 0x00 at offset 0");
    assertThatThrownBy(() -> read(fourBytes))
        .hasMessageContaining("Malformed NBT at s")
        .hasMessageContaining("byte 0xf0 at offset 0");
    assertThatThrownBy(() -> read(cut)).hasMessageContaining("truncated group");
  }

  @Test
  void keepsALoneSurrogate() throws IOException {
    byte[] bytes =
        new NbtBytes().named(10, "").named(8, "s").i16(3).raw(0xed, 0xa0, 0xbd).end().toByteArray();

    assertThat(read(bytes).value().get("s")).isEqualTo(String.valueOf((char) 0xd83d));
  }

  @Test
  void acceptsExactly512NestedContainersRootIncluded() throws IOException {
    assertThat(read(nestedCompounds(512)).value()).containsOnlyKeys("c");
    assertThatThrownBy(() -> read(nestedCompounds(513)))
        .hasMessageContaining("nesting deeper than 512");
  }

  /** A root compound holding nested compounds named c, {@code containers} in all. */
  private static byte[] nestedCompounds(int containers) {
    NbtBytes bytes = new NbtBytes().named(10, "");
    for (int i = 1; i < containers; i++) {
      bytes.named(10, "c");
    }
    for (int i = 0; i < containers; i++) {
      bytes.end();
    }
    return bytes.toByteArray();
  }

  @Test
  void refusesAHugeArrayLengthBeforeAllocating() {
    byte[] bytes =
        new NbtBytes().named(10, "").named(12, "la").i32(Integer.MAX_VALUE).end().toByteArray();

    assertThatThrownBy(() -> read(bytes)).hasMessageContaining("exceeds the limit");
  }

  @Test
  @Timeout(10)
  void boundsTheDecodedSizeOfManySmallTags() {
    NbtBytes bytes = new NbtBytes().named(10, "");
    for (int i = 0; i < 150_000; i++) {
      bytes.named(1, "k" + i).u8(0);
    }
    byte[] plain = bytes.end().toByteArray();
    NbtReader reader = new NbtReader(NbtReader.DEFAULT_MAX_DEPTH, 1_000_000);

    assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(plain)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("exceeds the limit of 1000000 bytes");
  }

  @Test
  @Timeout(10)
  void boundsTheSizeOfADecompressionBomb() {
    byte[] bomb =
        new NbtBytes()
            .named(10, "")
            .named(7, "zeros")
            .i32(4_000_000)
            .raw(new int[4_000_000])
            .end()
            .toByteArray();
    byte[] gzip = NbtBytes.gzip(bomb);
    byte[] zlib = NbtBytes.zlib(bomb);
    NbtReader reader = new NbtReader(NbtReader.DEFAULT_MAX_DEPTH, 1_000_000);

    assertThat(gzip.length).isLessThan(100_000);
    assertThatThrownBy(() -> reader.readGzip(new ByteArrayInputStream(gzip)))
        .hasMessageContaining("decompressed data exceeds 1000000 bytes");
    assertThatThrownBy(() -> reader.readZlib(new ByteArrayInputStream(zlib)))
        .hasMessageContaining("decompressed data exceeds 1000000 bytes");
  }
}
