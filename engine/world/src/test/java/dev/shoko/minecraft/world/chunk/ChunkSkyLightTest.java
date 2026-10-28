package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

/** CF-R30, CF-R31 and vector LI2: the effective sky light is a view computed on demand. */
class ChunkSkyLightTest {

  private final BlockStateRegistry registry = Blocks.registry();

  private Chunk overworld() {
    return Chunks.empty(registry, new ChunkPos(-1, -1), Chunks.OVERWORLD);
  }

  /** Bottom layer (indices 0 to 255) is i & 15, the 15 other layers hold 3: section 19 of LI2. */
  private static NibbleArray chainArray() {
    return Chunks.nibbles(i -> i < 256 ? i & 15 : 3);
  }

  private static void assertBottomLayerRepeated(NibbleArray actual, NibbleArray source) {
    byte[] bytes = actual.toBytes();
    byte[] bottom = source.toBytes();
    assertThat(bytes).hasSize(2048);
    for (int index = 0; index < 2048; index++) {
      assertThat(bytes[index]).as("byte " + index).isEqualTo(bottom[index % 128]);
    }
  }

  @Test
  void withoutAnyStoredArrayEverySectionIsFullyLit() {
    // spec: LI2, CF-R30 (the top section has nothing above: 15 everywhere, and so on downwards)
    Chunk chunk = overworld();

    for (int y = -4; y <= 19; y++) {
      assertThat(chunk.effectiveSkyLight(y).toBytes())
          .as("section " + y)
          .hasSize(2048)
          .containsOnly((byte) 0xff);
    }
  }

  @Test
  void derivingNeverStoresAnything() {
    // spec: CF-R30, LI2 (no side effect: absent stays absent, neither read nor write fills it)
    Chunk chunk = overworld();

    chunk.effectiveSkyLight(-4);
    chunk.effectiveSkyLight(19);

    for (ChunkSection section : chunk.sections()) {
      assertThat(section.light().skyLight()).as("section " + section.y()).isEmpty();
      assertThat(section.light().blockLight()).as("section " + section.y()).isEmpty();
    }
  }

  @Test
  void storedArrayIsTheEffectiveOneOfItsSection() {
    // spec: CF-R30 (if SkyLight is present, that array), LI2 section 19
    Chunk chunk = overworld();
    chunk.section(19).light().setSkyLight(chainArray());

    byte[] bytes = chunk.effectiveSkyLight(19).toBytes();

    assertThat(bytes[0]).isEqualTo((byte) 0x10);
    assertThat(bytes[127]).isEqualTo((byte) 0xfe);
    assertThat(bytes[128]).isEqualTo((byte) 0x33);
    assertThat(bytes[2047]).isEqualTo((byte) 0x33);
  }

  @Test
  void sectionBelowRepeatsTheBottomLayerOfTheSectionAbove() {
    // spec: CF-R30, LI2 (octet 128 is 10, not 33: the bottom layer, not the whole array)
    Chunk chunk = overworld();
    chunk.section(19).light().setSkyLight(chainArray());

    NibbleArray below = chunk.effectiveSkyLight(18);

    assertThat(below.toBytes()[0]).isEqualTo((byte) 0x10);
    assertThat(below.toBytes()[127]).isEqualTo((byte) 0xfe);
    assertThat(below.toBytes()[128]).isEqualTo((byte) 0x10);
    assertThat(below.toBytes()[2047]).isEqualTo((byte) 0xfe);
    assertBottomLayerRepeated(below, chainArray());
  }

  @Test
  void derivationIsRecursiveUpwardsAllTheWayDown() {
    // spec: CF-R30, LI2 (sections 17 down to -4 equal section 18)
    Chunk chunk = overworld();
    chunk.section(19).light().setSkyLight(chainArray());
    byte[] expected = chunk.effectiveSkyLight(18).toBytes();

    for (int y = 17; y >= -4; y--) {
      assertThat(chunk.effectiveSkyLight(y).toBytes()).as("section " + y).isEqualTo(expected);
    }
  }

  @Test
  void derivationStartsAtTheNearestStoredArrayAbove() {
    // spec: CF-R30 (section above if it exists, recursive; here a stored array in the middle)
    Chunk chunk = overworld();
    chunk.section(10).light().setSkyLight(NibbleArray.filled(5));

    assertThat(chunk.effectiveSkyLight(10).toBytes()).containsOnly((byte) 0x55);
    assertThat(chunk.effectiveSkyLight(9).toBytes()).containsOnly((byte) 0x55);
    assertThat(chunk.effectiveSkyLight(-4).toBytes()).containsOnly((byte) 0x55);
    assertThat(chunk.effectiveSkyLight(11).toBytes()).containsOnly((byte) 0xff);
    assertThat(chunk.effectiveSkyLight(19).toBytes()).containsOnly((byte) 0xff);
  }

  @Test
  void storedArrayOfZerosIsNotAbsent() {
    // spec: CF-R30 (absence is distinct from present and all zero)
    Chunk chunk = overworld();
    chunk.section(12).light().setSkyLight(NibbleArray.zeros());

    assertThat(chunk.effectiveSkyLight(12).toBytes()).containsOnly((byte) 0);
    assertThat(chunk.effectiveSkyLight(11).toBytes()).containsOnly((byte) 0);
    assertThat(chunk.effectiveSkyLight(13).toBytes()).containsOnly((byte) 0xff);
  }

  @Test
  void viewFollowsLaterChangesOfTheSectionsAbove() {
    // spec: Chunk.effectiveSkyLight contract (reflects later changes of the sections above)
    Chunk chunk = overworld();
    assertThat(chunk.effectiveSkyLight(5).toBytes()).containsOnly((byte) 0xff);

    chunk.section(8).light().setSkyLight(NibbleArray.filled(2));
    assertThat(chunk.effectiveSkyLight(5).toBytes()).containsOnly((byte) 0x22);

    chunk.section(8).light().clearSkyLight();
    assertThat(chunk.effectiveSkyLight(5).toBytes()).containsOnly((byte) 0xff);
  }

  @Test
  void returnedArrayIsANewOneThatDoesNotWriteThrough() {
    // spec: Chunk.effectiveSkyLight contract (a new array)
    Chunk chunk = overworld();
    chunk.section(19).light().setSkyLight(chainArray());

    NibbleArray view = chunk.effectiveSkyLight(19);
    view.set(0, 0);
    NibbleArray derived = chunk.effectiveSkyLight(3);
    derived.set(7, 0);

    assertThat(chunk.section(19).light().skyLight().orElseThrow().get(0)).isZero();
    assertThat(chunk.effectiveSkyLight(19).get(0)).isZero();
    assertThat(chunk.effectiveSkyLight(3).get(7)).isEqualTo(7);
    assertThat(chunk.section(3).light().skyLight()).isEmpty();
  }

  @Test
  void lightSectionAboveTheWorldCountsAsTheSectionAbove() {
    // spec: CF-R30, CF-R31 (the light section 20 is the section above section 19)
    SequencedMap<Integer, LightOnlySection> lightOnly = new LinkedHashMap<>();
    SectionLight topLight = SectionLight.none();
    topLight.setSkyLight(chainArray());
    lightOnly.put(20, new LightOnlySection(topLight, CompoundTag.empty()));
    Chunk chunk =
        Chunks.withAttachments(
            registry,
            new ChunkPos(0, 0),
            Chunks.OVERWORLD,
            Chunks.attachmentsWithLight(24, lightOnly));

    assertBottomLayerRepeated(chunk.effectiveSkyLight(19), chainArray());
    assertBottomLayerRepeated(chunk.effectiveSkyLight(0), chainArray());
  }

  @Test
  void lightSectionWithoutSkyLightAboveTheWorldIsFullyLit() {
    // spec: CF-R30, CF-R31 (a section without SkyLight derives from the one above, none: 15)
    SequencedMap<Integer, LightOnlySection> lightOnly = new LinkedHashMap<>();
    lightOnly.put(20, new LightOnlySection(SectionLight.none(), CompoundTag.empty()));
    Chunk chunk =
        Chunks.withAttachments(
            registry,
            new ChunkPos(0, 0),
            Chunks.OVERWORLD,
            Chunks.attachmentsWithLight(24, lightOnly));

    assertThat(chunk.effectiveSkyLight(19).toBytes()).containsOnly((byte) 0xff);
  }

  @Test
  void netherTopSectionIsFullyLit() {
    // spec: CF-R30 (no section above: 15 everywhere), CF-R1 (sections 0 to 15)
    Chunk chunk = Chunks.empty(registry, new ChunkPos(0, 0), Chunks.NETHER);

    assertThat(chunk.effectiveSkyLight(15).toBytes()).containsOnly((byte) 0xff);
    assertThat(chunk.effectiveSkyLight(0).toBytes()).containsOnly((byte) 0xff);
  }

  @Test
  void sectionsOutsideTheWorldHeightAreRejected() {
    // spec: Chunk.effectiveSkyLight contract (IllegalArgumentException), CF-R31
    Chunk chunk = overworld();

    Failures.expect(IllegalArgumentException.class, () -> chunk.effectiveSkyLight(20));
    Failures.expect(IllegalArgumentException.class, () -> chunk.effectiveSkyLight(-5));
    Failures.expect(IllegalArgumentException.class, () -> chunk.effectiveSkyLight(100));
  }
}
