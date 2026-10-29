package dev.shoko.minecraft.world.palette;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.ints;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.longs;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Nbt;
import dev.shoko.minecraft.world.support.RefPacking;
import dev.shoko.minecraft.world.support.TestEntries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CF-R6 to CF-R10: reading the disk form of a container. */
class PalettedContainerCodecReadTest {

  private static PalettedContainerCodec<Integer> blocks() {
    return PalettedContainerCodec.of(ContainerLayout.BLOCKS, TestEntries.ints());
  }

  private static PalettedContainerCodec<String> biomes() {
    return PalettedContainerCodec.of(ContainerLayout.BIOMES, TestEntries.strings());
  }

  private static ListTag intPalette(int size) {
    List<NbtTag> entries = new ArrayList<>();
    for (int k = 0; k < size; k++) {
      entries.add(i(k));
    }
    return Nbt.listOf(entries);
  }

  private static CompoundTag blockContainer(int paletteSize, long[] data) {
    return data == null
        ? compound("palette", intPalette(paletteSize))
        : compound("palette", intPalette(paletteSize), "data", longs(data));
  }

  private static void assertCycle(PalettedContainer<Integer> container, int modulus) {
    assertThat(container.size()).isEqualTo(4096);
    for (int index = 0; index < 4096; index++) {
      assertThat(container.get(index)).as("position " + index).isEqualTo(index % modulus);
    }
  }

  @Test
  void singleEntryPaletteFillsTheContainer() throws PaletteFormatException {
    // spec: CF-R6 (one entry: no data, the entry fills the container)
    PalettedContainer<Integer> container = blocks().decode(blockContainer(1, null));

    assertThat(container.size()).isEqualTo(4096);
    assertThat(container.isUniform()).isTrue();
    assertThat(container.get(0)).isZero();
    assertThat(container.get(4095)).isZero();
  }

  @Test
  void provisionalSingleEntryPaletteIgnoresDataThatIsPresent() throws PaletteFormatException {
    // spec: "Cas limites" of chunk-format (palette of one entry and data present: data ignored)
    PalettedContainer<Integer> container = blocks().decode(blockContainer(1, new long[] {5, 6, 7}));

    assertThat(container.isUniform()).isTrue();
    assertThat(container.get(100)).isZero();
  }

  @Test
  void seventeenEntriesUseFiveBitsWithoutOverlap() throws PaletteFormatException {
    // spec: CF-R7, CF-R8, PK3 (value i mod 17, 342 longs)
    long[] data = RefPacking.pack(RefPacking.cycle(4096, 17), 5);

    assertThat(data).hasSize(342);
    assertCycle(blocks().decode(blockContainer(17, data)), 17);
  }

  @Test
  void twoEntriesUseFourBitsAndTheBottomLayerIsTheSecondEntry() throws PaletteFormatException {
    // spec: CF-R7 (minimum 4 bits for blocks), PK4, RG1 chunk A (bedrock layer)
    int[] indices = new int[4096];
    Arrays.fill(indices, 0, 256, 1);

    PalettedContainer<Integer> container =
        blocks().decode(blockContainer(2, RefPacking.pack(indices, 4)));

    for (int index = 0; index < 4096; index++) {
      assertThat(container.get(index)).isEqualTo(index < 256 ? 1 : 0);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 15, 16, 17, 31, 32, 33, 64, 65, 128, 129, 256, 257, 1025, 4096})
  void everyBlockPaletteSizeIsReadWithItsBitsPerEntry(int paletteSize)
      throws PaletteFormatException {
    // spec: CF-R7, CF-R9 (bits are deduced from the palette size, not stored)
    int bits = RefPacking.blockBits(paletteSize);
    long[] data = RefPacking.pack(RefPacking.cycle(4096, paletteSize), bits);

    assertCycle(blocks().decode(blockContainer(paletteSize, data)), paletteSize);
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5, 8, 9, 17, 33, 64})
  void everyBiomePaletteSizeIsReadWithItsBitsPerEntry(int paletteSize)
      throws PaletteFormatException {
    // spec: CF-R7 (biomes have no minimum), PK1
    int bits = RefPacking.biomeBits(paletteSize);
    long[] data = RefPacking.pack(RefPacking.cycle(64, paletteSize), bits);
    List<NbtTag> entries = new ArrayList<>();
    for (int k = 0; k < paletteSize; k++) {
      entries.add(str("biome_" + k));
    }

    PalettedContainer<String> container =
        biomes().decode(compound("palette", Nbt.listOf(entries), "data", longs(data)));

    assertThat(container.size()).isEqualTo(64);
    for (int index = 0; index < 64; index++) {
      assertThat(container.get(index)).isEqualTo("biome_" + index % paletteSize);
    }
  }

  @Test
  void biomeCellsOfVectorPk5cAreRead() throws PaletteFormatException {
    // spec: PK5c (1 bit, 0x3333333333333333: cells with qx < 2 are the second entry)
    PalettedContainer<String> container =
        biomes()
            .decode(
                compound(
                    "palette",
                    list(str("plains"), str("desert")),
                    "data",
                    longs(0x3333333333333333L)));

    for (int index = 0; index < 64; index++) {
      assertThat(container.get(index)).isEqualTo((index & 3) < 2 ? "desert" : "plains");
    }
  }

  @Test
  void fullBlockPaletteOf4096EntriesUsesTwelveBits() throws PaletteFormatException {
    // spec: CF-R7 (n <= 4096, 12 bits), CF-R9 (820 longs)
    int[] indices = new int[4096];
    for (int k = 0; k < 4096; k++) {
      indices[k] = (k * 7 + 3) % 4096;
    }
    long[] data = RefPacking.pack(indices, 12);

    PalettedContainer<Integer> container = blocks().decode(blockContainer(4096, data));

    assertThat(data).hasSize(820);
    for (int k = 0; k < 4096; k++) {
      assertThat(container.get(k)).isEqualTo(indices[k]);
    }
  }

  @Test
  void provisionalUnusedHighBitsOfEachLongAreIgnored() throws PaletteFormatException {
    // spec: CF-R8 (the unused high bits are ignored at reading, provisional)
    long[] clean = RefPacking.pack(RefPacking.cycle(4096, 17), 5);
    long[] dirty = Arrays.stream(clean).map(value -> value | 0xF000000000000000L).toArray();

    assertCycle(blocks().decode(blockContainer(17, dirty)), 17);
  }

  @Test
  void emptyPaletteIsRejected() {
    // spec: CF-R10, RG3 empty_palette
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(compound("palette", ListTag.empty())));
    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(compound("palette", ListTag.empty(), "data", longs(new long[256]))));
  }

  @Test
  void paletteLargerThanTheLayoutIsRejected() {
    // spec: CF-R7 (n <= 4096 for blocks, n <= 64 for biomes), CF-R10
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(blockContainer(4097, new long[820])));
    List<NbtTag> sixtyFive = new ArrayList<>();
    for (int k = 0; k < 65; k++) {
      sixtyFive.add(str("biome_" + k));
    }
    Failures.expect(
        PaletteFormatException.class,
        () ->
            biomes()
                .decode(compound("palette", Nbt.listOf(sixtyFive), "data", longs(new long[7]))));
  }

  @Test
  void missingOrMistypedFieldsAreRejected() {
    // spec: CF-R6, CF-R10 (palette is a List, data a Long_Array)
    Failures.expect(PaletteFormatException.class, () -> blocks().decode(CompoundTag.empty()));
    Failures.expect(PaletteFormatException.class, () -> blocks().decode(compound("palette", i(1))));
    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(compound("palette", compound("a", i(1)))));
    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(compound("palette", intPalette(2), "data", ints(new int[256]))));
    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(compound("palette", intPalette(2), "data", list(i(0)))));
  }

  @Test
  void dataIsRequiredForSeveralEntries() {
    // spec: CF-R6, CF-R10
    Failures.expect(PaletteFormatException.class, () -> blocks().decode(blockContainer(2, null)));
  }

  @Test
  void dataOfAnotherLengthIsRejected() {
    // spec: CF-R9, CF-R10, RG3 bad_length (255 longs instead of 256)
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(blockContainer(2, new long[255])));
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(blockContainer(2, new long[257])));
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(blockContainer(2, new long[0])));
  }

  @Test
  void continuousPackingLengthIsRejected() {
    // spec: CF-R8, PK3 (320 longs is the continuous packing of before 1.16, 342 are expected)
    Failures.expect(
        PaletteFormatException.class, () -> blocks().decode(blockContainer(17, new long[320])));
  }

  @Test
  void indexEqualToThePaletteSizeIsRejected() {
    // spec: CF-R10, RG3 index_overflow
    int[] indices = new int[4096];
    indices[4095] = 2;

    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(blockContainer(2, RefPacking.pack(indices, 4))));
  }

  @Test
  void indexBeyondThePaletteInTheMiddleOfTheDataIsRejected() {
    // spec: CF-R10 (any position, here an index that fits the bits but not the palette)
    int[] indices = new int[4096];
    indices[1234] = 15;

    Failures.expect(
        PaletteFormatException.class,
        () -> blocks().decode(blockContainer(3, RefPacking.pack(indices, 4))));
  }

  @Test
  void entryThatCannotBeDecodedFailsTheContainer() {
    // spec: PaletteEntryCodec.decode contract (PaletteFormatException)
    CompoundTag mixed = compound("palette", Nbt.list(i(1), str("x")), "data", longs(new long[256]));

    Failures.expect(PaletteFormatException.class, () -> blocks().decode(mixed));
  }
}
