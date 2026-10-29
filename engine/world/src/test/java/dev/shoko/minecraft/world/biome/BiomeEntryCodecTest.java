package dev.shoko.minecraft.world.biome;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.palette.PaletteFormatException;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Warnings;
import org.junit.jupiter.api.Test;

/** CF-R11: palette entries of biomes. */
class BiomeEntryCodecTest {

  private final Warnings warnings = new Warnings();

  private BiomeEntryCodec codec() {
    return BiomeEntryCodec.of(Chunks.biomes(), warnings);
  }

  @Test
  void fallbackBiomeIsPlains() {
    // spec: CF-R11, CF-R22
    assertThat(BiomeEntryCodec.FALLBACK_BIOME).isEqualTo("minecraft:plains");
  }

  @Test
  void knownBiomeInAStringIsReadAsIs() throws PaletteFormatException {
    // spec: CF-R11
    assertThat(codec().decode(str("minecraft:desert"))).isEqualTo("minecraft:desert");
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void compoundWithANameIsAlsoAccepted() throws PaletteFormatException {
    // spec: CF-R11 (the reader also accepts a Compound with Name)
    assertThat(codec().decode(compound("Name", str("minecraft:ocean"))))
        .isEqualTo("minecraft:ocean");
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void provisionalUnknownBiomeBecomesPlainsWithAWarning() throws PaletteFormatException {
    // spec: CF-R11 (unknown biome: minecraft:plains with a warning)
    assertThat(codec().decode(str("probe:unknown_biome"))).isEqualTo("minecraft:plains");
    assertThat(warnings.codes()).containsExactly(WarningCode.UNKNOWN_BIOME);
    assertThat(warnings.all().getFirst().message()).contains("probe:unknown_biome");
  }

  @Test
  void provisionalUnknownBiomeInACompoundBecomesPlains() throws PaletteFormatException {
    // spec: CF-R11
    assertThat(codec().decode(compound("Name", str("probe:other")))).isEqualTo("minecraft:plains");
    assertThat(warnings.codes()).containsExactly(WarningCode.UNKNOWN_BIOME);
  }

  @Test
  void provisionalOtherTagsFailTheContainer() {
    // spec: CF-R11, CF-R10 (the Javadoc of the codec: the spec does not classify this case)
    NbtTag[] bad = {i(3), list(str("minecraft:plains")), compound("Id", str("minecraft:plains"))};
    BiomeEntryCodec codec = codec();

    for (NbtTag tag : bad) {
      Failures.expect(PaletteFormatException.class, () -> codec.decode(tag));
    }
  }

  @Test
  void writtenEntryIsAString() {
    // spec: CF-R11
    assertThat(codec().encode("minecraft:desert")).isEqualTo(str("minecraft:desert"));
  }
}
