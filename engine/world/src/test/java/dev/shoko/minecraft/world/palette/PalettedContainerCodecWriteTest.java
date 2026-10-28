package dev.shoko.minecraft.world.palette;

import static dev.shoko.minecraft.world.support.Nbt.i;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RefPacking;
import dev.shoko.minecraft.world.support.TestEntries;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R6 to CF-R9: writing the disk form of a container, and round trips. */
class PalettedContainerCodecWriteTest {

  private static PalettedContainerCodec<Integer> blocks() {
    return PalettedContainerCodec.of(ContainerLayout.BLOCKS, TestEntries.ints());
  }

  private static PalettedContainerCodec<String> biomes() {
    return PalettedContainerCodec.of(ContainerLayout.BIOMES, TestEntries.strings());
  }

  private static PalettedContainer<Integer> withDistinct(int distinct) {
    PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
    for (int index = 0; index < 4096; index++) {
      container.set(index, index % distinct);
    }
    return container;
  }

  /** Reads back a written container using the reference packing, whatever the palette order. */
  private static int[] indicesOf(CompoundTag written, int bits, int count) {
    long[] data = written.getLongArray("data");
    return RefPacking.unpack(data, bits, count);
  }

  private static int[] expandedValues(CompoundTag written, int bits, int count) {
    ListTag palette = written.getList("palette");
    int[] indices = indicesOf(written, bits, count);
    int[] values = new int[count];
    for (int k = 0; k < count; k++) {
      NbtTag entry = palette.elements().get(indices[k]);
      values[k] = ((dev.shoko.minecraft.core.nbt.IntTag) entry).value();
    }
    return values;
  }

  @Test
  void uniformContainerIsAPaletteOfOneEntryWithoutData() {
    // spec: CF-R6
    CompoundTag written = blocks().encode(PalettedContainer.filled(4096, 7));

    assertThat(written.entries().keySet()).containsExactly("palette");
    assertThat(written.getList("palette").elements()).containsExactly(i(7));
  }

  @ParameterizedTest
  @CsvSource({
    "2,4,256",
    "3,4,256",
    "16,4,256",
    "17,5,342",
    "32,5,342",
    "33,6,410",
    "129,8,512",
    "256,8,512",
    "257,9,586",
    "1025,11,820",
    "2049,12,820",
    "4096,12,820"
  })
  void blockDataHasTheBitsAndLengthOfThePaletteSize(int distinct, int bits, int longs) {
    // spec: CF-R7, CF-R8, CF-R9 (thresholds 16/17, 32/33, 256/257)
    PalettedContainer<Integer> container = withDistinct(distinct);

    CompoundTag written = blocks().encode(container);

    assertThat(written.getList("palette").elements()).hasSize(distinct);
    assertThat(written.get("data")).containsInstanceOf(LongArrayTag.class);
    assertThat(written.getLongArray("data")).hasSize(longs);
    assertThat(expandedValues(written, bits, 4096)).isEqualTo(RefPacking.cycle(4096, distinct));
  }

  @ParameterizedTest
  @CsvSource({"2,1,1", "3,2,2", "4,2,2", "5,3,4", "8,3,4", "9,4,4", "33,6,7", "64,6,7"})
  void biomeDataHasTheBitsAndLengthOfThePaletteSize(int distinct, int bits, int longs) {
    // spec: CF-R7 (biomes have no minimum), CF-R9
    PalettedContainer<String> container = PalettedContainer.filled(64, "biome_0");
    for (int index = 0; index < 64; index++) {
      container.set(index, "biome_" + index % distinct);
    }

    CompoundTag written = biomes().encode(container);

    assertThat(written.getList("palette").elements()).hasSize(distinct);
    assertThat(written.getLongArray("data")).hasSize(longs);
    ListTag palette = written.getList("palette");
    int[] indices = indicesOf(written, bits, 64);
    for (int index = 0; index < 64; index++) {
      NbtTag entry = palette.elements().get(indices[index]);
      assertThat(entry)
          .isEqualTo(new dev.shoko.minecraft.core.nbt.StringTag("biome_" + index % distinct));
    }
  }

  @Test
  void paletteIsCompactedAndHasNoUnusedEntry() {
    // spec: CF-R7 ("palettes compactees"), PalettedContainer.distinctValues contract
    PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
    container.set(10, 5);
    container.set(11, 6);
    container.set(11, 0);

    CompoundTag written = blocks().encode(container);

    assertThat(written.getList("palette").elements()).hasSize(2);
    assertThat(written.getList("palette").elements()).containsExactlyInAnyOrder(i(0), i(5));
    assertThat(written.getLongArray("data")).hasSize(256);
  }

  @Test
  void paletteEntriesAreAllDistinct() {
    // spec: CF-R6 (a palette lists distinct values)
    CompoundTag written = blocks().encode(withDistinct(40));

    assertThat(written.getList("palette").elements()).doesNotHaveDuplicates();
  }

  @Test
  void bottomLayerOfBedrockHasThePackedValueOfVectorPk4() {
    // spec: PK4, RG1 chunk A (the first layer of 256 positions holds the second entry)
    PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
    for (int index = 0; index < 256; index++) {
      container.set(index, 1);
    }

    CompoundTag written = blocks().encode(container);

    List<NbtTag> palette = written.getList("palette").elements();
    int[] indices = indicesOf(written, 4, 4096);
    for (int index = 0; index < 4096; index++) {
      assertThat(palette.get(indices[index])).isEqualTo(i(index < 256 ? 1 : 0));
    }
  }

  @Test
  void wrongSizeIsRejected() {
    // spec: PalettedContainerCodec.encode contract (IllegalArgumentException)
    PalettedContainer<Integer> small = PalettedContainer.filled(64, 0);
    PalettedContainer<String> large = PalettedContainer.filled(4096, "a");

    Failures.expect(IllegalArgumentException.class, () -> blocks().encode(small));
    Failures.expect(IllegalArgumentException.class, () -> biomes().encode(large));
  }

  @Test
  void writtenContainerReadsBackWithTheSameValues() throws PaletteFormatException {
    // spec: CF-R6 to CF-R9 (the reader accepts what the writer produces, any palette order)
    SplittableRandom random = new SplittableRandom(0xC0FFEE);
    int[] distinctCounts = {1, 2, 5, 16, 17, 100, 256, 257, 900, 4096};
    for (int distinct : distinctCounts) {
      PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
      int[] expected = new int[4096];
      for (int index = 0; index < 4096; index++) {
        expected[index] = distinct == 4096 ? index : random.nextInt(distinct);
        container.set(index, expected[index]);
      }

      PalettedContainer<Integer> back = blocks().decode(blocks().encode(container));

      for (int index = 0; index < 4096; index++) {
        assertThat(back.get(index))
            .as(distinct + " distinct, position " + index)
            .isEqualTo(expected[index]);
      }
    }
  }

  @Test
  void biomeContainerReadsBackWithTheSameValues() throws PaletteFormatException {
    // spec: CF-R5, CF-R6 (64 cells)
    PalettedContainer<String> container = PalettedContainer.filled(64, "minecraft:plains");
    for (int index = 0; index < 64; index += 3) {
      container.set(index, "minecraft:desert");
    }
    container.set(63, "minecraft:ocean");

    PalettedContainer<String> back = biomes().decode(biomes().encode(container));

    for (int index = 0; index < 64; index++) {
      assertThat(back.get(index)).isEqualTo(container.get(index));
    }
  }

  @Test
  void sameContainerAlwaysGivesTheSameOutput() {
    // spec: determinism (same input, same output)
    PalettedContainer<Integer> container = withDistinct(77);

    CompoundTag first = blocks().encode(container);
    CompoundTag second = blocks().encode(container.copy());

    assertThat(second).isEqualTo(first);
    assertThat(second.getList("palette")).isEqualTo(first.getList("palette"));
    assertThat(second.getLongArray("data")).isEqualTo(first.getLongArray("data"));
  }
}
