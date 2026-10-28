package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Nbt;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CF-R31, CF-R32, CF-R33: reconciliation of the section list, light sections and yPos. */
class ChunkCodecSectionsTest {

  private static ReadResult.Present<Chunk> read(String variant) {
    return Chunks.present(Chunks.decode(ChunkNbt.chunk(-1, -1, true, variant)));
  }

  private static List<Integer> writtenYs(CompoundTag written) {
    return written.getList("sections").elements().stream()
        .map(tag -> (int) ((ByteTag) ((CompoundTag) tag).get("Y").orElseThrow()).value())
        .toList();
  }

  @Test
  void unsortedSectionsAreReadLikeSortedOnes() {
    // spec: CF-R32 d, RG4 unsorted_sections (the order of the entries is irrelevant)
    Chunk sorted = Chunks.decodeClean(ChunkNbt.chunk(-1, -1, true, "bedrock"));
    ReadResult.Present<Chunk> unsorted = read("unsorted_sections");

    assertThat(unsorted.warnings()).isEmpty();
    Chunks.assertSameChunk(sorted, unsorted.value());
    assertThat(unsorted.value().sections().getFirst().y()).isEqualTo(-4);
  }

  @Test
  void unsortedSectionsAreWrittenFromBottomToTop() {
    // spec: CF-R32 d (the writer sorts from bottom to top)
    Chunk chunk = read("unsorted_sections").value();

    List<Integer> ys = writtenYs(Chunks.codec().encode(chunk));

    assertThat(ys).hasSize(24).isSorted();
    assertThat(ys.getFirst()).isEqualTo(-4);
    assertThat(ys.getLast()).isEqualTo(19);
  }

  @Test
  void missingSectionIsSynthesizedEmptyWithOneWarning() {
    // spec: CF-R32 f, RG4 missing_section (section 0 absent: air, plains, no light, a warning)
    ReadResult.Present<Chunk> result = read("missing_section");

    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsExactly(WarningCode.SECTION_MISSING);
    Chunk chunk = result.value();
    assertThat(chunk.sections()).hasSize(24);
    assertThat(chunk.section(0).y()).isZero();
    assertThat(chunk.section(0).blockStates().isUniform()).isTrue();
    assertThat(chunk.section(0).blockStates().get(0).id()).isZero();
    assertThat(chunk.section(0).biomes().get(0)).isEqualTo(Chunks.PLAINS);
    assertThat(chunk.section(0).light().blockLight()).isEmpty();
    assertThat(chunk.section(0).light().skyLight()).isEmpty();
    assertThat(chunk.section(-4).blockStates().get(0).id()).isEqualTo(4);
  }

  @Test
  void rewriteAfterAMissingSectionHasTheCompleteList() {
    // spec: CF-R32 f (the list written at the next save is complete)
    Chunk chunk = read("missing_section").value();

    List<Integer> ys = writtenYs(Chunks.codec().encode(chunk));

    assertThat(ys).hasSize(24).contains(0);
  }

  @Test
  void provisionalSeveralMissingSectionsEachGetOneWarning() {
    // spec: CF-R32 f (one warning for each synthesized section)
    CompoundTag root = ChunkNbt.chunk(-1, -1, true, "bedrock");
    List<NbtTag> kept = new ArrayList<>(root.getList("sections").elements().subList(0, 3));

    ReadResult.Present<Chunk> result =
        Chunks.present(Chunks.decode(with(root, "sections", Nbt.listOf(kept))));

    assertThat(result.value().sections()).hasSize(24);
    assertThat(result.warnings()).hasSize(21);
    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsOnly(WarningCode.SECTION_MISSING);
    assertThat(result.value().section(-4).blockStates().get(0).id()).isEqualTo(4);
  }

  @Test
  void emptySectionListGivesOneWarningAndEverySectionEmpty() {
    // spec: CF-R32 g (an empty List: all sections synthesized, a single warning)
    ReadResult.Present<Chunk> result =
        Chunks.present(
            Chunks.decode(
                with(ChunkNbt.chunk(-1, -1, true, "bedrock"), "sections", ListTag.empty())));

    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsExactly(WarningCode.SECTIONS_EMPTY);
    assertThat(result.value().sections()).hasSize(24);
    for (ChunkSection section : result.value().sections()) {
      assertThat(section.blockStates().isUniform()).isTrue();
      assertThat(section.blockStates().get(0).id()).isZero();
      assertThat(section.biomes().get(5)).isEqualTo(Chunks.PLAINS);
    }
  }

  @Test
  void farSectionsAreIgnoredWithAWarningEach() {
    // spec: CF-R32 e, RG4 far_sections (Y -6 and 21: ignored, not rewritten)
    ReadResult.Present<Chunk> result = read("far_sections");

    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsExactly(WarningCode.SECTION_IGNORED, WarningCode.SECTION_IGNORED);
    assertThat(result.value().sections()).hasSize(24);
    assertThat(result.value().attachments().lightOnlySections()).isEmpty();
    assertThat(writtenYs(Chunks.codec().encode(result.value()))).hasSize(24).doesNotContain(-6, 21);
  }

  @Test
  void lightSectionsAreAcceptedWithoutWarningAndKeptAside() {
    // spec: CF-R31, RG4 light_sections (Y -5 and 20 hold only light)
    ReadResult.Present<Chunk> result = read("light_sections");

    assertThat(result.warnings()).isEmpty();
    Chunk chunk = result.value();
    assertThat(chunk.sections()).hasSize(24);
    assertThat(List.copyOf(chunk.attachments().lightOnlySections().keySet()))
        .containsExactly(-5, 20);
    LightOnlySection bottom = chunk.attachments().lightOnlySections().get(-5);
    assertThat(bottom.light().blockLight().orElseThrow().toBytes()[0]).isEqualTo((byte) 7);
    assertThat(bottom.light().skyLight().orElseThrow().toBytes()).containsOnly((byte) 0xff);
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(-5));
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(20));
  }

  @Test
  void lightSectionsAreWrittenBackAsTheyWereRead() {
    // spec: CF-R31 (rewritten as read: same Y, same Compound, at their place bottom to top)
    CompoundTag original = ChunkNbt.chunk(-1, -1, true, "light_sections");
    Chunk chunk = Chunks.decodeClean(original);

    CompoundTag written = Chunks.codec().encode(chunk);

    assertThat(writtenYs(written)).hasSize(26);
    assertThat(writtenYs(written).getFirst()).isEqualTo(-5);
    assertThat(writtenYs(written).getLast()).isEqualTo(20);
    List<NbtTag> out = written.getList("sections").elements();
    List<NbtTag> in = original.getList("sections").elements();
    assertThat(out.getFirst()).isEqualTo(in.getFirst());
    assertThat(out.getLast()).isEqualTo(in.getLast());
  }

  @Test
  void blocksAndBiomesOfALightSectionAreIgnoredButKeptVerbatim() {
    // spec: CF-R31 (block_states and biomes of a light section are ignored, written back unchanged)
    CompoundTag original = ChunkNbt.chunk(-1, -1, true, "light_sections");
    List<NbtTag> sections = new ArrayList<>(original.getList("sections").elements());
    CompoundTag brokenContainer = Nbt.compound("palette", ListTag.empty());
    CompoundTag top = (CompoundTag) sections.getLast();
    top = with(top, "block_states", brokenContainer);
    top = with(top, "biomes", Nbt.compound("palette", Nbt.list(i(1))));
    sections.set(sections.size() - 1, top);

    ReadResult.Present<Chunk> result =
        Chunks.present(Chunks.decode(with(original, "sections", Nbt.listOf(sections))));

    assertThat(result.warnings()).isEmpty();
    CompoundTag written = Chunks.codec().encode(result.value());
    assertThat(written.getList("sections").elements().getLast()).isEqualTo(top);
  }

  @Test
  void lightSectionWithoutLightArraysIsAccepted() {
    // spec: CF-R31 (with or without BlockLight and SkyLight)
    CompoundTag original = ChunkNbt.chunk(-1, -1, true, "bedrock");
    List<NbtTag> sections = new ArrayList<>(original.getList("sections").elements());
    sections.add(Nbt.compound("Y", Nbt.b(20)));

    ReadResult.Present<Chunk> result =
        Chunks.present(Chunks.decode(with(original, "sections", Nbt.listOf(sections))));

    assertThat(result.warnings()).isEmpty();
    LightOnlySection light = result.value().attachments().lightOnlySections().get(20);
    assertThat(light.light().blockLight()).isEmpty();
    assertThat(light.light().skyLight()).isEmpty();
  }

  @Test
  void skyLightAbsentStaysAbsentThroughReadAndWrite() {
    // spec: CF-R30, RG4 skylight_chain (only section 19 stores SkyLight)
    ReadResult.Present<Chunk> result = read("skylight_chain");

    assertThat(result.warnings()).isEmpty();
    Chunk chunk = result.value();
    assertThat(chunk.section(19).light().skyLight()).isPresent();
    for (int y = -4; y <= 18; y++) {
      assertThat(chunk.section(y).light().skyLight()).as("section " + y).isEmpty();
    }
    CompoundTag written = Chunks.codec().encode(chunk);
    for (NbtTag tag : written.getList("sections").elements()) {
      CompoundTag section = (CompoundTag) tag;
      boolean isTop = section.getByte("Y") == 19;
      assertThat(section.entries().containsKey("SkyLight"))
          .as("Y " + section.getByte("Y"))
          .isEqualTo(isTop);
      assertThat(section.entries().containsKey("BlockLight")).isFalse();
    }
  }

  @Test
  void skyLightChainGivesTheEffectiveLightOfVectorLi2() {
    // spec: LI2 (section 18: byte 128 is 0x10, bottom layer repeated)
    Chunk chunk = read("skylight_chain").value();

    byte[] top = chunk.effectiveSkyLight(19).toBytes();
    byte[] below = chunk.effectiveSkyLight(18).toBytes();

    assertThat(top[128]).isEqualTo((byte) 0x33);
    assertThat(below[128]).isEqualTo((byte) 0x10);
    assertThat(below[2047]).isEqualTo((byte) 0xfe);
  }

  @Test
  void storedLightArraysAreWrittenByteForByte() {
    // spec: CF-R14, CF-R30 (a stored array is written as stored)
    CompoundTag original = ChunkNbt.chunk(-1, -1, true, "skylight_chain");
    Chunk chunk = Chunks.decodeClean(original);

    CompoundTag written = Chunks.codec().encode(chunk);

    CompoundTag top = (CompoundTag) written.getList("sections").elements().getLast();
    ByteArrayTag expected = ChunkNbt.lightArray(3, k -> k & 15);
    assertThat(top.get("SkyLight")).contains(expected);
  }

  @Test
  void wrongYPosWarnsAndIsRewrittenFromTheDimension() {
    // spec: CF-R33, RG4 ypos_wrong (yPos 0 is ignored with a warning; the writer gives -4)
    ReadResult.Present<Chunk> result = read("ypos_wrong");

    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsExactly(WarningCode.YPOS_MISMATCH);
    assertThat(result.value().sections().getFirst().y()).isEqualTo(-4);
    assertThat(Chunks.codec().encode(result.value()).getInt("yPos")).isEqualTo(-4);
  }

  @Test
  void absentYPosIsAcceptedWithoutWarning() {
    // spec: CF-R33, RG4 ypos_absent
    ReadResult.Present<Chunk> result = read("ypos_absent");

    assertThat(result.warnings()).isEmpty();
    assertThat(Chunks.codec().encode(result.value()).getInt("yPos")).isEqualTo(-4);
  }

  @Test
  void yPosOfAnotherTypeIsIgnoredWithAWarning() {
    // spec: CF-R33 (present and not an Int: ignored with a warning)
    for (NbtTag wrong : new NbtTag[] {str("-4"), Nbt.l(-4), Nbt.b(-4), Nbt.s(-4)}) {
      ReadResult.Present<Chunk> result =
          Chunks.present(
              Chunks.decode(with(ChunkNbt.chunk(-1, -1, true, "bedrock"), "yPos", wrong)));

      assertThat(result.warnings())
          .extracting(w -> w.code())
          .containsExactly(WarningCode.YPOS_WRONG_TYPE);
    }
  }

  @Test
  void yPosIsMinSectionOfTheDimensionForTheNether() {
    // spec: CF-R33 (written: min_y >> 4 of the dimension type; 0 for the Nether)
    Chunk chunk =
        Chunks.present(Chunks.codec().decode(ChunkNbt.netherChunk(0, 0), Chunks.NETHER)).value();

    assertThat(Chunks.codec().encode(chunk).getInt("yPos")).isZero();
  }

  @Test
  void netherLightSectionsAreAtMinusOneAndSixteen() {
    // spec: CF-R31 (Nether and End: Y -1 and 16)
    CompoundTag nether = ChunkNbt.netherChunk(0, 0);
    List<NbtTag> sections = new ArrayList<>(nether.getList("sections").elements());
    CompoundTag low = ChunkNbt.lightOnlySection(-1);
    CompoundTag high = ChunkNbt.lightOnlySection(16);
    sections.addFirst(low);
    sections.add(high);
    sections.add(ChunkNbt.lightOnlySection(17));

    ReadResult.Present<Chunk> result =
        Chunks.present(
            Chunks.codec().decode(with(nether, "sections", Nbt.listOf(sections)), Chunks.NETHER));

    assertThat(List.copyOf(result.value().attachments().lightOnlySections().keySet()))
        .containsExactly(-1, 16);
    assertThat(result.warnings())
        .extracting(w -> w.code())
        .containsExactly(WarningCode.SECTION_IGNORED);
  }
}
