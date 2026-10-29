package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.Nbt.b;
import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.ints;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static dev.shoko.minecraft.world.support.Nbt.without;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.chunk.ChunkFormatException;
import dev.shoko.minecraft.world.support.Failures;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

/** AR-R22, AR-R23: the roots of entities/ and poi/ chunks (provisional schemas). */
class AuxiliaryChunkCodecTest {

  private static CompoundTag entitiesRoot() {
    return compound(
        "DataVersion",
        i(5023),
        "Position",
        ints(-1, 2),
        "Entities",
        list(compound("id", str("minecraft:pig")), compound("id", str("minecraft:armor_stand"))));
  }

  private static CompoundTag poiRoot() {
    return compound(
        "DataVersion",
        i(5023),
        "Sections",
        compound(
            "-4",
            compound(
                "Valid",
                b(1),
                "Records",
                list(
                    compound(
                        "pos",
                        ints(1, -60, 1),
                        "type",
                        str("minecraft:composter"),
                        "free_tickets",
                        i(0)),
                    compound(
                        "pos",
                        ints(-2, -63, 5),
                        "type",
                        str("minecraft:bed"),
                        "free_tickets",
                        i(1)))),
            "0",
            compound("Valid", b(0), "Records", list()),
            "5",
            compound("Valid", b(1), "Records", list())));
  }

  @Test
  void entitiesRootIsRead() throws ChunkFormatException {
    // spec: AR-R22 (DataVersion Int, Position Int_Array [cx, cz], Entities List of Compound)
    EntitiesChunk chunk = AuxiliaryChunkCodec.decodeEntities(entitiesRoot());

    assertThat(chunk.dataVersion()).isEqualTo(5023);
    assertThat(chunk.position()).isEqualTo(new ChunkPos(-1, 2));
    assertThat(chunk.entities())
        .containsExactly(
            compound("id", str("minecraft:pig")), compound("id", str("minecraft:armor_stand")));
  }

  @Test
  void entitiesRootIgnoresUnknownKeys() throws ChunkFormatException {
    // spec: AuxiliaryChunkCodec Javadoc (unknown keys are ignored when reading)
    EntitiesChunk chunk =
        AuxiliaryChunkCodec.decodeEntities(with(entitiesRoot(), "Extra", compound("a", i(1))));

    assertThat(chunk.entities()).hasSize(2);
  }

  @Test
  void entitiesRoundTripAndOnlyListedKeysAreWritten() throws ChunkFormatException {
    // spec: AR-R22 (only the listed keys are written)
    EntitiesChunk chunk = AuxiliaryChunkCodec.decodeEntities(entitiesRoot());

    CompoundTag written = AuxiliaryChunkCodec.encodeEntities(chunk);

    assertThat(written.entries().keySet())
        .containsExactlyInAnyOrder("DataVersion", "Position", "Entities");
    assertThat(written).isEqualTo(entitiesRoot());
    assertThat(AuxiliaryChunkCodec.decodeEntities(written)).isEqualTo(chunk);
  }

  @Test
  void entitiesAreWrittenWithTheTagTypesOfTheSchema() {
    // spec: AR-R22 (Position is an Int_Array)
    CompoundTag written =
        AuxiliaryChunkCodec.encodeEntities(
            new EntitiesChunk(5023, new ChunkPos(-30_000, 12), List.of()));

    assertThat(written.get("DataVersion")).contains(i(5023));
    assertThat(written.get("Position")).contains(ints(-30_000, 12));
    assertThat(written.getList("Entities").elements()).isEmpty();
  }

  @Test
  void entitiesFieldsMissingOrMistypedAreReportedWithTheirPath() {
    // spec: AuxiliaryChunkCodec.decodeEntities contract (ChunkFormatException)
    for (String key : new String[] {"DataVersion", "Position", "Entities"}) {
      ChunkFormatException missing =
          Failures.expect(
              ChunkFormatException.class,
              () -> AuxiliaryChunkCodec.decodeEntities(without(entitiesRoot(), key)));
      assertThat(missing.reason()).as(key).isEqualTo(ChunkFormatException.Reason.MISSING_FIELD);
      assertThat(missing.path()).as(key).isEqualTo(key);
    }
    NbtTag[] wrong = {str("5023"), list(i(1), i(2)), compound("a", i(1))};
    String[] keys = {"DataVersion", "Position", "Entities"};
    for (int k = 0; k < keys.length; k++) {
      CompoundTag root = with(entitiesRoot(), keys[k], wrong[k]);
      ChunkFormatException failure =
          Failures.expect(
              ChunkFormatException.class, () -> AuxiliaryChunkCodec.decodeEntities(root));
      assertThat(failure.reason()).as(keys[k]).isEqualTo(ChunkFormatException.Reason.WRONG_TYPE);
      assertThat(failure.path()).as(keys[k]).isEqualTo(keys[k]);
    }
  }

  @Test
  void positionOfAnotherLengthThanTwoIsRejected() {
    // spec: AR-R22 (Position is an Int_Array of 2 elements)
    for (int[] bad : new int[][] {{}, {1}, {1, 2, 3}}) {
      CompoundTag root = with(entitiesRoot(), "Position", ints(bad));

      ChunkFormatException failure =
          Failures.expect(
              ChunkFormatException.class, () -> AuxiliaryChunkCodec.decodeEntities(root));

      assertThat(failure.path()).isEqualTo("Position");
    }
  }

  @Test
  void poiRootIsReadWithSectionsKeyedByIntegers() throws ChunkFormatException {
    // spec: AR-R23 (Sections keyed by section y in decimal text, Valid Byte, Records)
    PoiChunk chunk = AuxiliaryChunkCodec.decodePoi(poiRoot());

    assertThat(chunk.dataVersion()).isEqualTo(5023);
    assertThat(List.copyOf(chunk.sections().keySet())).containsExactly(-4, 0, 5);
    PoiSection first = chunk.sections().get(-4);
    assertThat(first.valid()).isTrue();
    assertThat(first.records())
        .containsExactly(
            new PoiRecord(new BlockPos(1, -60, 1), "minecraft:composter", 0),
            new PoiRecord(new BlockPos(-2, -63, 5), "minecraft:bed", 1));
    assertThat(chunk.sections().get(0).valid()).isFalse();
    assertThat(chunk.sections().get(0).records()).isEmpty();
    assertThat(chunk.sections().get(5).valid()).isTrue();
  }

  @Test
  void poiSectionsKeepTheirFileOrder() throws ChunkFormatException {
    // spec: AuxiliaryChunkCodec Javadoc (PoiChunk sections are in file order)
    CompoundTag sections =
        compound(
            "3", compound("Valid", b(1), "Records", list()),
            "-4", compound("Valid", b(1), "Records", list()),
            "0", compound("Valid", b(1), "Records", list()));

    PoiChunk chunk =
        AuxiliaryChunkCodec.decodePoi(compound("DataVersion", i(5023), "Sections", sections));

    assertThat(List.copyOf(chunk.sections().keySet())).containsExactly(3, -4, 0);
  }

  @Test
  void poiRoundTripsWithTextKeysAndByteValid() throws ChunkFormatException {
    // spec: AR-R23 (keys "-4" and "0" in decimal, Valid written as a Byte)
    PoiChunk chunk = AuxiliaryChunkCodec.decodePoi(poiRoot());

    CompoundTag written = AuxiliaryChunkCodec.encodePoi(chunk);

    assertThat(written.entries().keySet()).containsExactlyInAnyOrder("DataVersion", "Sections");
    assertThat(List.copyOf(written.getCompound("Sections").entries().keySet()))
        .containsExactly("-4", "0", "5");
    assertThat(written.getCompound("Sections").getCompound("-4").get("Valid")).contains(b(1));
    assertThat(written.getCompound("Sections").getCompound("0").get("Valid")).contains(b(0));
    assertThat(written).isEqualTo(poiRoot());
    assertThat(AuxiliaryChunkCodec.decodePoi(written)).isEqualTo(chunk);
  }

  @Test
  void poiEncodeOfAnEmptyChunkHasAnEmptySectionsCompound() {
    // spec: AR-R23
    SequencedMap<Integer, PoiSection> none = new LinkedHashMap<>();

    CompoundTag written = AuxiliaryChunkCodec.encodePoi(new PoiChunk(5023, none));

    assertThat(written.getCompound("Sections").entries()).isEmpty();
    assertThat(written.getInt("DataVersion")).isEqualTo(5023);
  }

  @Test
  void poiFieldsMissingMistypedOrMalformedAreRejected() {
    // spec: AuxiliaryChunkCodec.decodePoi contract (DataVersion, Sections, key not decimal,
    // malformed record)
    CompoundTag badKey =
        compound(
            "DataVersion",
            i(5023),
            "Sections",
            compound("up", compound("Valid", b(1), "Records", list())));
    CompoundTag noRecords =
        compound("DataVersion", i(5023), "Sections", compound("-4", compound("Valid", b(1))));
    CompoundTag recordWithoutPos =
        compound(
            "DataVersion",
            i(5023),
            "Sections",
            compound(
                "-4",
                compound(
                    "Valid",
                    b(1),
                    "Records",
                    list(compound("type", str("x"), "free_tickets", i(0))))));
    CompoundTag[] roots = {
      without(poiRoot(), "DataVersion"),
      without(poiRoot(), "Sections"),
      with(poiRoot(), "DataVersion", str("5023")),
      with(poiRoot(), "Sections", list()),
      badKey,
      noRecords,
      recordWithoutPos
    };

    for (CompoundTag root : roots) {
      Failures.expect(ChunkFormatException.class, () -> AuxiliaryChunkCodec.decodePoi(root));
    }
  }

  @Test
  void poiFaultNamesTheFieldPath() {
    // spec: ChunkFormatException Javadoc (a path such as Sections.-4.Records)
    CompoundTag noRecords =
        compound("DataVersion", i(5023), "Sections", compound("-4", compound("Valid", b(1))));

    ChunkFormatException failure =
        Failures.expect(ChunkFormatException.class, () -> AuxiliaryChunkCodec.decodePoi(noRecords));

    assertThat(failure.path()).isEqualTo("Sections.-4.Records");
    assertThat(failure.reason()).isEqualTo(ChunkFormatException.Reason.MISSING_FIELD);
  }

  @Test
  void poiIgnoresUnknownKeys() throws ChunkFormatException {
    // spec: AuxiliaryChunkCodec Javadoc (unknown keys are ignored when reading)
    CompoundTag root = with(poiRoot(), "Extra", i(1));

    assertThat(AuxiliaryChunkCodec.decodePoi(root).sections()).hasSize(3);
  }
}
