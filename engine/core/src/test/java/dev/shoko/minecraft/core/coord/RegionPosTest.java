package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: core-math R33. The conversion from chunks is tested in ChunkPosTest; file names, sectors
// and header offsets belong to the Anvil storage spec and have no API here.
class RegionPosTest {

  // spec: core-math R33, V8: published example, chunk (30, -3) is in region (0, -1)
  @Test
  void publishedExampleChunkIsInRegionZeroMinusOne() {
    assertThat(new ChunkPos(30, -3).region()).isEqualTo(new RegionPos(0, -1));
  }

  // records: value semantics, negative coordinates kept as given
  @Test
  void isAValueObject() {
    RegionPos a = new RegionPos(-58594, 58593);
    RegionPos b = new RegionPos(-58594, 58593);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new RegionPos(58593, -58594));
    assertThat(a.x()).isEqualTo(-58594);
    assertThat(a.z()).isEqualTo(58593);
  }
}
