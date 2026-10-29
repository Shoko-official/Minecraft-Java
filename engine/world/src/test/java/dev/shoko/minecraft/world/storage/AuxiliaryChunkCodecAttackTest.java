package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.Nbt.b;
import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.ints;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.chunk.ChunkFormatException;
import dev.shoko.minecraft.world.support.TreeMutator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on {@link AuxiliaryChunkCodec}: a fault of the tree is a ChunkFormatException. */
class AuxiliaryChunkCodecAttackTest {

  static Stream<Long> seeds() {
    return LongStream.range(0, 80).boxed();
  }

  private static CompoundTag entities() {
    return compound(
        "DataVersion",
        i(5023),
        "Position",
        ints(3, -4),
        "Entities",
        list(compound("id", str("minecraft:pig")), compound("id", str("minecraft:cow"))));
  }

  private static CompoundTag poi() {
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
                        str("minecraft:bed"),
                        "free_tickets",
                        i(1)))),
            "2",
            compound("Valid", b(0), "Records", list())));
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedEntitiesRootsAreReadOrReportedAndWhatIsReadWritesBack(long seed) {
    // spec: AR-R22, AuxiliaryChunkCodec.decodeEntities contract (ChunkFormatException only)
    TreeMutator mutator = new TreeMutator(seed);
    for (int round = 0; round < 15; round++) {
      CompoundTag damaged = mutator.mutate(entities(), 1 + round % 3);
      try {
        EntitiesChunk chunk = AuxiliaryChunkCodec.decodeEntities(damaged);
        assertThat(AuxiliaryChunkCodec.decodeEntities(AuxiliaryChunkCodec.encodeEntities(chunk)))
            .isEqualTo(chunk);
      } catch (ChunkFormatException expected) {
        assertThat(expected.path()).isNotNull();
        assertThat(expected.reason()).isNotNull();
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedPoiRootsAreReadOrReportedAndWhatIsReadWritesBack(long seed) {
    // spec: AR-R23, AuxiliaryChunkCodec.decodePoi contract (ChunkFormatException only)
    TreeMutator mutator = new TreeMutator(1_000 + seed);
    for (int round = 0; round < 15; round++) {
      CompoundTag damaged = mutator.mutate(poi(), 1 + round % 3);
      try {
        PoiChunk chunk = AuxiliaryChunkCodec.decodePoi(damaged);
        assertThat(AuxiliaryChunkCodec.decodePoi(AuxiliaryChunkCodec.encodePoi(chunk)))
            .isEqualTo(chunk);
      } catch (ChunkFormatException expected) {
        assertThat(expected.path()).isNotNull();
        assertThat(expected.reason()).isNotNull();
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void randomRootsAreReportedAsFormatFaults(long seed) {
    // spec: AR-R22, AR-R23 (a root without the required fields)
    TreeMutator mutator = new TreeMutator(2_000 + seed);
    CompoundTag random =
        mutator.randomTag(2) instanceof CompoundTag compound ? compound : CompoundTag.empty();

    try {
      AuxiliaryChunkCodec.decodeEntities(random);
    } catch (ChunkFormatException expected) {
      assertThat(expected.getMessage()).isNotNull();
    }
    try {
      AuxiliaryChunkCodec.decodePoi(random);
    } catch (ChunkFormatException expected) {
      assertThat(expected.getMessage()).isNotNull();
    }
  }
}
