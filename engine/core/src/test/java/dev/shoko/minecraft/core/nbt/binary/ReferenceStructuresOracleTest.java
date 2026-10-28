package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.TagType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Criterion M1 of the spec on the 1511 structure files of the reference game (R14, R16, R30):
 * decode the gzip file, rewrite it, compare with the decompressed bytes. The files are read from
 * the local cache ({@code MINECRAFT_CACHE_DIR}, else {@code .cache} at the repository root) and
 * never copied; every test is skipped when the cache is absent.
 */
@Tag("oracle")
class ReferenceStructuresOracleTest {

  private static final String GZIP_HEADER = "1f 8b 08 00 00 00 00 00 00 ff";

  static Path structures() {
    String configured = System.getenv("MINECRAFT_CACHE_DIR");
    Path cache;
    if (configured != null && !configured.isBlank()) {
      cache = Path.of(configured);
    } else {
      Path dir = Path.of("").toAbsolutePath();
      while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
        dir = dir.getParent();
      }
      cache = (dir == null ? Path.of("") : dir).resolve(".cache");
    }
    return cache.resolve("reference/26.3/resources/data/minecraft/structure");
  }

  static Path requireStructures() {
    Path dir = structures();
    assumeTrue(Files.isDirectory(dir), () -> "reference cache absent: " + dir);
    return dir;
  }

  static List<Path> structureFiles() throws IOException {
    try (Stream<Path> files = Files.walk(requireStructures())) {
      return files.filter(p -> p.toString().endsWith(".nbt")).sorted().toList();
    }
  }

  @Test
  void corpusHas1511Files() throws IOException {
    // spec: core-nbt R14, R16 (1511 files)
    assertThat(structureFiles()).hasSize(1511);
  }

  @TestFactory
  Stream<DynamicTest> everyStructureFileRewritesToSameBytes() throws IOException {
    // spec: core-nbt R14, R16, R30 (criterion M1: identical decompressed bytes)
    Path root = requireStructures();
    return structureFiles().stream()
        .map(
            file ->
                DynamicTest.dynamicTest(
                    root.relativize(file).toString().replace('\\', '/'),
                    () -> assertRewritesIdentically(Files.readAllBytes(file))));
  }

  private static void assertRewritesIdentically(byte[] file) throws NbtException {
    assertThat(Hex.format(Arrays.copyOf(file, 10))).isEqualTo(GZIP_HEADER);
    byte[] decompressed = Containers.gunzip(file);
    NamedTag fromGzip = NbtReader.of(NbtCompression.GZIP).readNamed(file).orElseThrow();
    assertThat(fromGzip.name()).isEmpty();
    assertThat(fromGzip.tag().type()).isEqualTo(TagType.COMPOUND);
    assertThat(((CompoundTag) fromGzip.tag()).getInt("DataVersion")).isEqualTo(5023);
    byte[] rewritten = NbtWriter.of(NbtCompression.NONE).writeNamed(fromGzip);
    assertThat(rewritten).as("rewritten bytes").isEqualTo(decompressed);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "empty.nbt, 124, 148, ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f",
    "igloo/top.nbt, 914, 6298, 2ec42e950b2dbfd2db6cc4ab440b29046c5fbebfd2952d8a5e600c20917b959a",
    "village/plains/villagers/baby.nbt, 712, 1134,"
        + " 1de0d5f67910dfae108852eb3bf91ece28ec115136700613893513989679d251",
    "bastion/hoglin_stable/air_base.nbt, 80375, 1078387,"
        + " 2e28eafc83c37d958453de50ad9e3b76cd78ed91c1f5509ca3827e5ed846132d"
  })
  void referenceFileMatchesTableFDigests(
      String name, int compressed, int decompressed, String sha256) throws IOException {
    // spec: core-nbt-vectors table F (F1 to F4): sizes and SHA-256 of the decompressed bytes
    byte[] file = Files.readAllBytes(requireStructures().resolve(name));
    assertThat(file).hasSize(compressed);
    byte[] raw = Containers.gunzip(file);
    assertThat(raw).hasSize(decompressed);
    assertThat(Hex.sha256(raw)).isEqualTo(sha256);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "empty.nbt, 0, 1, 1",
    "igloo/top.nbt, 0, 152, 11",
    "village/plains/villagers/baby.nbt, 1, 2, 2",
    "bastion/hoglin_stable/air_base.nbt, 0, 29938, 3"
  })
  void referenceFileMatchesTableFContents(String name, int entities, int blocks, int palette)
      throws IOException, NbtException {
    // spec: core-nbt-vectors table F (F1 to F4): root keys in order and list element counts
    byte[] file = Files.readAllBytes(requireStructures().resolve(name));
    var tag = (CompoundTag) NbtReader.of(NbtCompression.GZIP).readNamed(file).orElseThrow().tag();
    assertThat(tag.entries().sequencedKeySet())
        .containsExactly("size", "entities", "blocks", "palette", "DataVersion");
    assertThat(tag.getList("size").elementType()).isEqualTo(TagType.INT);
    assertThat(tag.getList("entities").elements()).hasSize(entities);
    assertThat(tag.getList("blocks").elements()).hasSize(blocks);
    assertThat(tag.getList("palette").elements()).hasSize(palette);
  }

  @Test
  void emptyStructureIsVectorF1() throws IOException {
    // spec: core-nbt-vectors F1 (bytes printed in the spec)
    byte[] file = Files.readAllBytes(requireStructures().resolve("empty.nbt"));
    assertThat(Hex.format(Containers.gunzip(file))).isEqualTo(Hex.format(ProbeFiles.f1()));
  }

  @Test
  void babyVillagerHasDocumentedValues() throws IOException, NbtException {
    // spec: core-nbt-vectors "Details de F3", R8 (UUID), R35 (-0.0784000015258789)
    Path file = requireStructures().resolve("village/plains/villagers/baby.nbt");
    var tag =
        (CompoundTag)
            NbtReader.of(NbtCompression.GZIP)
                .readNamed(Files.readAllBytes(file))
                .orElseThrow()
                .tag();
    assertThat(tag.getList("size")).isEqualTo(list(intTag(1), intTag(2), intTag(1)));
    var nbt = ((CompoundTag) tag.getList("entities").elements().get(0)).getCompound("nbt");
    assertThat(nbt.entries()).hasSize(32);
    assertThat(nbt.get("UUID"))
        .contains(IntArrayTag.of(895037032, -995736946, -1319125422, -1224629455));
    assertThat(nbt.get("Health")).contains(FloatTag.of(20.0f));
    assertThat(nbt.getInt("Age")).isEqualTo(-21359);
    assertThat(nbt.getShort("Fire")).isEqualTo((short) -1);
    assertThat(nbt.getShort("Air")).isEqualTo((short) 300);
    assertThat(nbt.getLong("LastRestock")).isZero();
    assertThat(nbt.getByte("OnGround")).isEqualTo((byte) 1);
    assertThat(nbt.getList("Motion").elementType()).isEqualTo(TagType.DOUBLE);
    assertThat(nbt.getList("Motion").elements().get(1)).isEqualTo(doubleTag(-0.0784000015258789));
    assertThat(nbt.getList("Rotation").elementType()).isEqualTo(TagType.FLOAT);
    assertThat(nbt.getList("Gossips")).isEqualTo(ListTag.empty());
    assertThat(nbt.getList("HandItems").elements()).hasSize(2);
    assertThat(nbt.getList("ArmorItems").elements()).hasSize(4);
    assertThat(nbt.getCompound("VillagerData").entries()).hasSize(3);
  }
}
