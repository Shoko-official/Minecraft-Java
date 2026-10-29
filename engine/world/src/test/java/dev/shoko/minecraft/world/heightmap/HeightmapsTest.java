package dev.shoko.minecraft.world.heightmap;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.ints;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.longs;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RefPacking;
import dev.shoko.minecraft.world.support.Warnings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CF-R16, CF-R19, CF-R34 and vector HM4: valid or unknown, never invented. */
class HeightmapsTest {

  private static final WorldHeight HEIGHT = Chunks.OVERWORLD;
  private static final long[] ONES = RefPacking.pack(ChunkNbt.constant(256, 1), 9);

  private final Warnings warnings = new Warnings();

  private static CompoundTag fourTypes() {
    return compound(
        "MOTION_BLOCKING", longs(ONES),
        "MOTION_BLOCKING_NO_LEAVES", longs(ONES),
        "OCEAN_FLOOR", longs(ONES),
        "WORLD_SURFACE", longs(ONES));
  }

  private Heightmaps read(CompoundTag compound) {
    return Heightmaps.fromNbt(compound, HEIGHT, warnings);
  }

  @Test
  void emptyHolderHasNoValidType() {
    // spec: CF-R34 (each type is valid or unknown), Heightmaps.empty contract
    Heightmaps heightmaps = Heightmaps.empty();

    for (HeightmapType type : HeightmapType.values()) {
      assertThat(heightmaps.get(type)).as(type.name()).isEmpty();
    }
    assertThat(heightmaps.unknownTypes()).containsExactly(HeightmapType.values());
    assertThat(heightmaps.toNbt().entries()).isEmpty();
  }

  @Test
  void typeKeysAreTheConstantNames() {
    // spec: CF-R16 (the six types)
    assertThat(HeightmapType.values())
        .extracting(Enum::name)
        .containsExactly(
            "WORLD_SURFACE",
            "WORLD_SURFACE_WG",
            "OCEAN_FLOOR",
            "OCEAN_FLOOR_WG",
            "MOTION_BLOCKING",
            "MOTION_BLOCKING_NO_LEAVES");
  }

  @Test
  void validTypesAreReadAndAbsentOnesAreUnknownWithoutWarning() {
    // spec: CF-R19, CF-R34, HM4 (type absent: unknown, no warning)
    Heightmaps heightmaps = read(fourTypes());

    assertThat(heightmaps.get(HeightmapType.MOTION_BLOCKING)).isPresent();
    assertThat(heightmaps.get(HeightmapType.WORLD_SURFACE).orElseThrow().get(2, 3)).isEqualTo(1);
    assertThat(heightmaps.unknownTypes())
        .containsExactly(HeightmapType.WORLD_SURFACE_WG, HeightmapType.OCEAN_FLOOR_WG);
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void absentCompoundGivesSixUnknownTypesWithoutWarning() {
    // spec: HM4 (Heightmaps absent: unknown, no warning)
    Heightmaps heightmaps = read(CompoundTag.empty());

    assertThat(heightmaps.unknownTypes()).containsExactly(HeightmapType.values());
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void everyColumnAtTheHeightIsValid() {
    // spec: HM4 (37 longs, every column at 384: valid, no warning)
    long[] top = RefPacking.pack(ChunkNbt.constant(256, 384), 9);

    Heightmaps heightmaps = read(compound("OCEAN_FLOOR", longs(top)));

    assertThat(top[0]).isEqualTo(0x6030180c06030180L);
    assertThat(heightmaps.get(HeightmapType.OCEAN_FLOOR).orElseThrow().get(0, 0)).isEqualTo(384);
    assertThat(warnings.isEmpty()).isTrue();
  }

  @Test
  void provisionalOldContinuousLengthMakesTheTypeUnknownWithOneWarning() {
    // spec: CF-R34, HM4 (36 longs)
    CompoundTag compound = with(fourTypes(), "MOTION_BLOCKING", longs(Arrays.copyOf(ONES, 36)));

    Heightmaps heightmaps = read(compound);

    assertThat(heightmaps.get(HeightmapType.MOTION_BLOCKING)).isEmpty();
    assertThat(heightmaps.get(HeightmapType.WORLD_SURFACE)).isPresent();
    assertThat(heightmaps.unknownTypes()).contains(HeightmapType.MOTION_BLOCKING);
    assertThat(warnings.codes()).containsExactly(WarningCode.HEIGHTMAP_INVALID);
    assertThat(warnings.all().getFirst().message()).contains("MOTION_BLOCKING");
  }

  @Test
  void provisionalTooLongMakesTheTypeUnknownWithOneWarning() {
    // spec: CF-R34, HM4 (38 longs)
    CompoundTag compound = with(fourTypes(), "OCEAN_FLOOR", longs(Arrays.copyOf(ONES, 38)));

    Heightmaps heightmaps = read(compound);

    assertThat(heightmaps.get(HeightmapType.OCEAN_FLOOR)).isEmpty();
    assertThat(warnings.codes()).containsExactly(WarningCode.HEIGHTMAP_INVALID);
  }

  @Test
  void provisionalValueAboveTheHeightMakesTheTypeUnknownWithOneWarning() {
    // spec: CF-R34, HM4 (variant heightmap_overflow: column 0 at 400)
    int[] values = ChunkNbt.constant(256, 1);
    values[0] = 400;

    Heightmaps heightmaps =
        read(with(fourTypes(), "WORLD_SURFACE", longs(RefPacking.pack(values, 9))));

    assertThat(heightmaps.get(HeightmapType.WORLD_SURFACE)).isEmpty();
    assertThat(warnings.codes()).containsExactly(WarningCode.HEIGHTMAP_INVALID);
  }

  @Test
  void provisionalWrongTagTypesMakeTheTypeUnknownWithOneWarningEach() {
    // spec: CF-R34, HM4 (a List instead of a Long_Array), CF-R19 (type NBT invalid)
    NbtTag[] wrong = {list(i(1)), ints(new int[37]), i(3)};
    HeightmapType[] types = {
      HeightmapType.MOTION_BLOCKING, HeightmapType.OCEAN_FLOOR, HeightmapType.WORLD_SURFACE
    };
    CompoundTag compound = fourTypes();
    for (int k = 0; k < wrong.length; k++) {
      compound = with(compound, types[k].name(), wrong[k]);
    }

    Heightmaps heightmaps = read(compound);

    for (HeightmapType type : types) {
      assertThat(heightmaps.get(type)).as(type.name()).isEmpty();
    }
    assertThat(heightmaps.get(HeightmapType.MOTION_BLOCKING_NO_LEAVES)).isPresent();
    assertThat(warnings.codes()).hasSize(3).containsOnly(WarningCode.HEIGHTMAP_INVALID);
  }

  @Test
  void keyThatIsNotATypeIsIgnoredWithoutWarningAndNotWrittenBack() {
    // spec: CF-R34, HM4 (key FOO: ignored, not rewritten, no warning)
    Heightmaps heightmaps = read(with(fourTypes(), "FOO", longs(ONES)));

    assertThat(warnings.isEmpty()).isTrue();
    assertThat(heightmaps.toNbt().entries()).doesNotContainKey("FOO");
  }

  @Test
  void writtenCompoundHoldsTheValidTypesOnlyInDeclarationOrder() {
    // spec: Heightmaps.toNbt contract, CF-R34 (only VALID types are written)
    Heightmaps heightmaps = Heightmaps.empty();
    heightmaps.put(HeightmapType.MOTION_BLOCKING, Heightmap.filled(HEIGHT, 5));
    heightmaps.put(HeightmapType.WORLD_SURFACE, Heightmap.filled(HEIGHT, 1));

    CompoundTag written = heightmaps.toNbt();

    assertThat(List.copyOf(written.entries().keySet()))
        .containsExactly("WORLD_SURFACE", "MOTION_BLOCKING");
    assertThat(written.getLongArray("WORLD_SURFACE")).isEqualTo(ONES);
    assertThat(written.getLongArray("MOTION_BLOCKING"))
        .isEqualTo(Heightmap.filled(HEIGHT, 5).toLongs());
  }

  @Test
  void anInvalidTypeIsLostWhenWrittenBack() {
    // spec: CF-R34 (read with an unknown type then written without recompute: the type is lost)
    CompoundTag compound = with(fourTypes(), "MOTION_BLOCKING", longs(Arrays.copyOf(ONES, 36)));

    CompoundTag written = read(compound).toNbt();

    assertThat(written.entries()).doesNotContainKey("MOTION_BLOCKING");
    assertThat(written.entries()).containsKeys("WORLD_SURFACE", "OCEAN_FLOOR");
  }

  @Test
  void validCompoundReadsBackIdentically() {
    // spec: CF-R17, CF-R34 (round trip of valid types)
    CompoundTag written = read(fourTypes()).toNbt();

    assertThat(written).isEqualTo(fourTypes());
  }

  @Test
  void putMakesATypeValidAndRemoveMakesItUnknown() {
    // spec: Heightmaps.put and remove contracts, CF-R34 (the recompute of M5 makes a type valid)
    Heightmaps heightmaps = Heightmaps.empty();

    heightmaps.put(HeightmapType.OCEAN_FLOOR, Heightmap.filled(HEIGHT, 7));

    assertThat(heightmaps.get(HeightmapType.OCEAN_FLOOR).orElseThrow().get(0, 0)).isEqualTo(7);
    assertThat(heightmaps.unknownTypes()).doesNotContain(HeightmapType.OCEAN_FLOOR);
    heightmaps.remove(HeightmapType.OCEAN_FLOOR);
    assertThat(heightmaps.get(HeightmapType.OCEAN_FLOOR)).isEmpty();
    assertThat(heightmaps.unknownTypes()).contains(HeightmapType.OCEAN_FLOOR);
  }

  @Test
  void putReplacesThePreviousHeightmap() {
    Heightmaps heightmaps = read(fourTypes());

    heightmaps.put(HeightmapType.WORLD_SURFACE, Heightmap.filled(HEIGHT, 9));

    assertThat(heightmaps.get(HeightmapType.WORLD_SURFACE).orElseThrow().get(5, 5)).isEqualTo(9);
    assertThat(heightmaps.get(HeightmapType.OCEAN_FLOOR).orElseThrow().get(5, 5)).isEqualTo(1);
  }

  @Test
  void expectedLengthFollowsTheDimensionHeight() {
    // spec: CF-R17, D9 (the length is not the constant 37)
    WorldHeight low = new WorldHeight(0, 128);
    long[] data = RefPacking.pack(ChunkNbt.constant(256, 100), 8);

    Heightmaps readLow = Heightmaps.fromNbt(compound("OCEAN_FLOOR", longs(data)), low, warnings);
    Heightmaps readHigh = read(compound("OCEAN_FLOOR", longs(data)));

    assertThat(readLow.get(HeightmapType.OCEAN_FLOOR)).isPresent();
    assertThat(readHigh.get(HeightmapType.OCEAN_FLOOR)).isEmpty();
    assertThat(warnings.codes()).containsExactly(WarningCode.HEIGHTMAP_INVALID);
  }
}
