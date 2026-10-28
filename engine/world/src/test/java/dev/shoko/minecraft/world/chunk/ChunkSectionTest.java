package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.palette.PalettedContainer;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R2, CF-R4, CF-R5, CF-R22: a 16 x 16 x 16 part of a chunk. */
class ChunkSectionTest {

  @ParameterizedTest
  @CsvSource({
    "0,-64,0,0",
    "15,-64,0,3",
    "0,-64,1,0",
    "0,-63,0,0",
    "-1,319,-1,63",
    "-16,0,5,4",
    "5,10,-3,45",
    "4,0,0,1",
    "0,4,0,16",
    "0,0,4,4",
    "3,3,3,0",
    "15,15,15,63",
    "0,-49,0,48",
    "-17,-1,-17,63"
  })
  void biomeCellIndexIsYzxOfTheQuartersOfTheLocalCoordinates(int x, int y, int z, int index) {
    // spec: CF-R5, CO1 (index (qy * 4 + qz) * 4 + qx with q = local >> 2)
    assertThat(ChunkSection.biomeIndex(x, y, z)).isEqualTo(index);
  }

  @Test
  void biomeCellsGroupFourByFourByFourBlocks() {
    // spec: CF-R5 (64 cells of 4 x 4 x 4 blocks)
    int base = ChunkSection.biomeIndex(8, 4, 12);

    for (int dx = 0; dx < 4; dx++) {
      for (int dy = 0; dy < 4; dy++) {
        for (int dz = 0; dz < 4; dz++) {
          assertThat(ChunkSection.biomeIndex(8 + dx, 4 + dy, 12 + dz)).isEqualTo(base);
        }
      }
    }
    assertThat(ChunkSection.biomeIndex(12, 4, 12)).isEqualTo(base + 1);
  }

  @Test
  void uniformSectionHoldsOneStateAndOneBiomeAndNoLight() {
    // spec: ChunkSection.uniform contract
    BlockStateRegistry registry = Blocks.registry();
    BlockState bedrock = registry.state(4);

    ChunkSection section = ChunkSection.uniform(-4, bedrock, Chunks.DESERT);

    assertThat(section.y()).isEqualTo(-4);
    assertThat(section.blockStates().size()).isEqualTo(4096);
    assertThat(section.biomes().size()).isEqualTo(64);
    assertThat(section.blockStates().get(0)).isEqualTo(bedrock);
    assertThat(section.blockStates().get(4095)).isEqualTo(bedrock);
    assertThat(section.biomes().get(63)).isEqualTo(Chunks.DESERT);
    assertThat(section.light().blockLight()).isEmpty();
    assertThat(section.light().skyLight()).isEmpty();
  }

  @Test
  void sectionKeepsTheContainersItIsGiven() {
    // spec: ChunkSection.of contract
    BlockStateRegistry registry = Blocks.registry();
    PalettedContainer<BlockState> states = Chunks.filledBlocks(registry.air());
    PalettedContainer<String> biomes = Chunks.filledBiomes(Chunks.PLAINS);
    SectionLight light = SectionLight.none();

    ChunkSection section = ChunkSection.of(7, states, biomes, light);
    section.blockStates().set(100, registry.state(4));
    section.biomes().set(5, Chunks.DESERT);
    section.light().setBlockLight(NibbleArray.filled(3));

    assertThat(section.y()).isEqualTo(7);
    assertThat(states.get(100)).isEqualTo(registry.state(4));
    assertThat(biomes.get(5)).isEqualTo(Chunks.DESERT);
    assertThat(light.blockLight()).isPresent();
  }

  @Test
  void containersOfTheWrongSizeAreRejected() {
    // spec: ChunkSection.of contract (IllegalArgumentException)
    BlockStateRegistry registry = Blocks.registry();
    PalettedContainer<BlockState> goodStates = Chunks.filledBlocks(registry.air());
    PalettedContainer<String> goodBiomes = Chunks.filledBiomes(Chunks.PLAINS);
    PalettedContainer<BlockState> badStates = PalettedContainer.filled(64, registry.air());
    PalettedContainer<String> badBiomes = PalettedContainer.filled(4096, Chunks.PLAINS);

    Failures.expect(
        IllegalArgumentException.class,
        () -> ChunkSection.of(0, badStates, goodBiomes, SectionLight.none()));
    Failures.expect(
        IllegalArgumentException.class,
        () -> ChunkSection.of(0, goodStates, badBiomes, SectionLight.none()));
  }
}
