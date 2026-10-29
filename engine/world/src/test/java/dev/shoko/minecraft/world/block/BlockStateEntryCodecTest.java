package dev.shoko.minecraft.world.block;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Warnings;
import org.junit.jupiter.api.Test;

/** BS-R16 to BS-R22: reading is lenient and never fails, writing follows R16 and R17. */
class BlockStateEntryCodecTest {

  private final Warnings warnings = new Warnings();

  private BlockStateEntryCodec codec() {
    return BlockStateEntryCodec.of(Blocks.registry(), warnings);
  }

  private int decodeId(NbtTag entry) {
    return codec().decode(entry).id();
  }

  @Test
  void fullCompoundEntryIsDecodedWithoutWarning() {
    // spec: BS-R16, BS-R19
    CompoundTag entry =
        compound(
            "id",
            str(Blocks.CHEST),
            "properties",
            compound("facing", str("north"), "type", str("left"), "waterlogged", str("true")));

    assertThat(decodeId(entry)).isEqualTo(26);
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void orderOfThePropertyKeysIsIrrelevant() {
    // spec: BS-R18
    CompoundTag one =
        compound(
            "id",
            str(Blocks.CHEST),
            "properties",
            compound("waterlogged", str("false"), "facing", str("west"), "type", str("right")));
    CompoundTag other =
        compound(
            "id",
            str(Blocks.CHEST),
            "properties",
            compound("type", str("right"), "waterlogged", str("false"), "facing", str("west")));

    assertThat(decodeId(one)).isEqualTo(decodeId(other));
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void blockWithoutPropertiesNeedsOnlyItsIdentifier() {
    // spec: BS-R15, BS-R16
    assertThat(decodeId(compound("id", str(Blocks.STONE)))).isEqualTo(1);
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void stringEntryIsTheDefaultStateOfTheBlock() {
    // spec: BS-R19 (a palette may be a List of String of default states)
    assertThat(decodeId(str(Blocks.OAK_LOG))).isEqualTo(22);
    assertThat(decodeId(str(Blocks.CHEST))).isEqualTo(25);
    assertThat(decodeId(str(Blocks.STONE))).isEqualTo(1);
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void provisionalMissingPropertiesTakeTheDefaultValue() {
    // spec: BS-R20, BS-R22 c (one warning per corrected property, naming it)
    int id = decodeId(compound("id", str(Blocks.OAK_LOG)));

    assertThat(id).isEqualTo(22);
    assertThat(warnings.codes()).containsExactly(WarningCode.DEFAULTED_PROPERTY);
    assertThat(warnings.all().getFirst().message()).contains("axis");
  }

  @Test
  void provisionalPartialPropertiesKeepTheGivenOnesAndDefaultTheRest() {
    // spec: BS-R20 (facing=south given; type=single and waterlogged=false are the defaults)
    int id =
        decodeId(compound("id", str(Blocks.CHEST), "properties", compound("facing", str("south"))));

    assertThat(id).isEqualTo(24 + 1 * 6 + 0 * 2 + 1);
    assertThat(warnings.codes())
        .containsExactlyInAnyOrder(WarningCode.DEFAULTED_PROPERTY, WarningCode.DEFAULTED_PROPERTY);
    assertThat(warnings.all()).extracting(ReadWarning::message).anyMatch(m -> m.contains("type"));
    assertThat(warnings.all())
        .extracting(ReadWarning::message)
        .anyMatch(m -> m.contains("waterlogged"));
  }

  @Test
  void provisionalUnknownBlockBecomesAirWithAWarning() {
    // spec: BS-R21, BS-R22 c
    int id = decodeId(compound("id", str("probe:unknown")));

    assertThat(id).isZero();
    assertThat(warnings.codes()).containsExactly(WarningCode.UNKNOWN_BLOCK);
    assertThat(warnings.all().getFirst().message()).contains("probe:unknown");
  }

  @Test
  void provisionalUnknownBlockInAStringEntryBecomesAir() {
    // spec: BS-R21 (a String entry is a block identifier too)
    assertThat(decodeId(str("probe:unknown"))).isZero();
    assertThat(warnings.codes()).containsExactly(WarningCode.UNKNOWN_BLOCK);
  }

  @Test
  void provisionalUnknownPropertyIsIgnored() {
    // spec: BS-R21, BS-R22 c
    int id =
        decodeId(
            compound(
                "id",
                str(Blocks.OAK_LOG),
                "properties",
                compound("axis", str("x"), "color", str("red"))));

    assertThat(id).isEqualTo(21);
    assertThat(warnings.codes()).containsExactly(WarningCode.UNKNOWN_PROPERTY);
    assertThat(warnings.all().getFirst().message()).contains("color");
  }

  @Test
  void provisionalValueOutsideTheListTakesTheDefaultsValue() {
    // spec: BS-R21 (oak_log[axis=w] gives axis=y), BS-R22 c
    int id =
        decodeId(compound("id", str(Blocks.OAK_LOG), "properties", compound("axis", str("w"))));

    assertThat(id).isEqualTo(22);
    assertThat(warnings.codes()).containsExactly(WarningCode.INVALID_PROPERTY_VALUE);
    assertThat(warnings.all().getFirst().message()).contains("axis");
  }

  @Test
  void provisionalOneBadValueDoesNotSpoilTheOtherProperties() {
    // spec: BS-R21 (only the faulty property takes the default's value)
    int id =
        decodeId(
            compound(
                "id",
                str(Blocks.CHEST),
                "properties",
                compound("facing", str("east"), "type", str("weird"), "waterlogged", str("true"))));

    assertThat(id).isEqualTo(24 + 3 * 6 + 0 * 2 + 0);
    assertThat(warnings.codes()).containsExactly(WarningCode.INVALID_PROPERTY_VALUE);
  }

  @Test
  void provisionalEntriesThatAreNeitherStringNorCompoundBecomeAir() {
    // spec: BS-R22 a (treated like an unknown block), BS-R22 b (never refuses the chunk)
    assertThat(decodeId(i(5))).isZero();
    assertThat(decodeId(list(str(Blocks.STONE)))).isZero();
    assertThat(warnings.codes())
        .containsExactly(WarningCode.MALFORMED_PALETTE_ENTRY, WarningCode.MALFORMED_PALETTE_ENTRY);
  }

  @Test
  void provisionalCompoundWithoutAStringIdBecomesAir() {
    // spec: BS-R22 a
    assertThat(decodeId(compound("properties", compound("axis", str("x"))))).isZero();
    assertThat(decodeId(compound("id", i(1)))).isZero();
    assertThat(warnings.codes())
        .containsExactly(WarningCode.MALFORMED_PALETTE_ENTRY, WarningCode.MALFORMED_PALETTE_ENTRY);
  }

  @Test
  void writtenEntryHasStringIdAndAllPropertiesAsStrings() {
    // spec: BS-R16
    BlockState chest = Blocks.registry().state(26);

    NbtTag written = codec().encode(chest);

    assertThat(written)
        .isEqualTo(
            compound(
                "id",
                str(Blocks.CHEST),
                "properties",
                compound("facing", str("north"), "type", str("left"), "waterlogged", str("true"))));
  }

  @Test
  void writtenEntryOfABlockWithoutPropertiesHasNoPropertiesKey() {
    // spec: BS-R16 (properties present only if the block has properties)
    NbtTag written = codec().encode(Blocks.registry().state(1));

    assertThat(written).isEqualTo(compound("id", str(Blocks.STONE)));
  }

  @Test
  void writtenEntryDoesNotElideTheDefaultState() {
    // spec: BS-R17 (grass_block[snowy=false] is the default and keeps its properties)
    NbtTag written = codec().encode(Blocks.registry().state(3));

    assertThat(written)
        .isEqualTo(
            compound("id", str(Blocks.GRASS_BLOCK), "properties", compound("snowy", str("false"))));
  }

  @Test
  void everyStateRoundTripsWithoutWarning() {
    // spec: BS-R16, BS-R19 (the reader accepts what the writer produces)
    BlockStateRegistry registry = Blocks.registry();
    BlockStateEntryCodec codec = BlockStateEntryCodec.of(registry, warnings);

    for (int id = 0; id < registry.stateCount(); id++) {
      BlockState state = registry.state(id);
      assertThat(codec.decode(codec.encode(state))).isEqualTo(state);
    }
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void writingTwiceGivesEqualTrees() {
    // spec: determinism
    BlockState state = Blocks.registry().state(41);

    assertThat(codec().encode(state)).isEqualTo(codec().encode(state));
  }
}
