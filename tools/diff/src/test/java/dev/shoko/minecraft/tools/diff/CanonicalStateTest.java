package dev.shoko.minecraft.tools.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CanonicalStateTest {

  /** SHA-256 of no bytes at all. */
  private static final String EMPTY_SHA256 =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  @Test
  void buildsKeysFromSegments() {
    String key =
        StateKey.of(
            StateKey.segment("chunk", 2, -1),
            StateKey.segment("section", 4),
            StateKey.segment("block", 34, 70, -12));

    assertThat(key).isEqualTo("chunk(2,-1)/section(4)/block(34,70,-12)");
    assertThat(StateKey.segment("biomes")).isEqualTo("biomes");
  }

  @Test
  void refusesMalformedAndDuplicateKeys() {
    CanonicalState.Builder builder = CanonicalState.builder().add("a/b", "1");

    assertThatThrownBy(() -> builder.add("a/b", "2")).hasMessageContaining("duplicate key");
    assertThatThrownBy(() -> builder.add("a//b", "1")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> builder.add("a/", "1")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> builder.add("", "1")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StateKey.segment("a/b")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void digestsDependOnOrderAndOnFieldBoundaries() {
    CanonicalState ab = CanonicalState.builder().add("a", "1").add("b", "2").build();
    CanonicalState ba = CanonicalState.builder().add("b", "2").add("a", "1").build();
    CanonicalState split1 = CanonicalState.builder().add("k", "ab").build();
    CanonicalState split2 = CanonicalState.builder().add("ka", "b").build();

    assertThat(CanonicalState.builder().build().digest()).isEqualTo(EMPTY_SHA256);
    assertThat(ab.digest()).hasSize(64).isNotEqualTo(ba.digest());
    assertThat(split1.digest()).isNotEqualTo(split2.digest());
    assertThat(ab.digest())
        .isEqualTo(CanonicalState.builder().add("a", "1").add("b", "2").build().digest());
  }

  @Test
  void digestsEachSubStateWithoutMixingSimilarPrefixes() {
    CanonicalState state =
        CanonicalState.builder()
            .add("chunk(2,-1)/section(0)/block(0,0,0)", "stone")
            .add("chunk(2,-10)/section(0)/block(0,0,0)", "stone")
            .add("chunk(2,-1)/section(1)/block(0,16,0)", "dirt")
            .build();

    assertThat(state.digestsAt(1)).containsOnlyKeys("chunk(2,-1)", "chunk(2,-10)");
    assertThat(state.digestsAt(1).keySet()).containsExactly("chunk(2,-1)", "chunk(2,-10)");
    assertThat(state.digestsAt(2))
        .containsOnlyKeys(
            "chunk(2,-1)/section(0)", "chunk(2,-10)/section(0)", "chunk(2,-1)/section(1)");
    CanonicalState onlyFirstChunk =
        CanonicalState.builder()
            .add("chunk(2,-1)/section(0)/block(0,0,0)", "stone")
            .add("chunk(2,-1)/section(1)/block(0,16,0)", "dirt")
            .build();
    assertThat(state.digestOf("chunk(2,-1)")).isEqualTo(onlyFirstChunk.digest());
    assertThat(state.digestsAt(1).get("chunk(2,-1)")).isEqualTo(onlyFirstChunk.digest());
    assertThat(state.digestOf("chunk(9,9)")).isEqualTo(EMPTY_SHA256);
  }

  @Test
  void separatesKeysThatDifferOnlyByTheSeparator() {
    CanonicalState state =
        CanonicalState.builder()
            .add("chunk(0,0)/heightmap", "64")
            .add("chunk(0,0)/heightmaps/x", "70")
            .build();
    CanonicalState heightmapOnly =
        CanonicalState.builder().add("chunk(0,0)/heightmap", "64").build();

    assertThat(state.digestOf("chunk(0,0)/heightmap")).isEqualTo(heightmapOnly.digest());
    assertThat(state.digestOf("chunk(0,0)/heightmaps"))
        .isEqualTo(CanonicalState.builder().add("chunk(0,0)/heightmaps/x", "70").build().digest());
    assertThat(state.digestsAt(2))
        .containsOnlyKeys("chunk(0,0)/heightmap", "chunk(0,0)/heightmaps");
  }

  @Test
  void listsSubStatesFoundOnlyInTheActualState() {
    CanonicalState expected = WorldFixture.reference();
    var actual = WorldFixture.entries();
    actual.add(WorldFixture.entry("chunk(9,-9)/section(0)/palette(0)", "minecraft:stone"));
    actual.add(2, WorldFixture.entry("chunk(-5,0)/biome", "minecraft:plains"));

    assertThat(StateComparator.divergentSubStates(expected, WorldFixture.state(actual), 1))
        .containsExactly("chunk(-5,0)", "chunk(9,-9)");
  }

  @Test
  void subStateDigestsSingleOutTheDivergentChunk() {
    CanonicalState expected = WorldFixture.reference();
    var actual = WorldFixture.entries();
    String key = "chunk(3,-1)/section(5)/palette(2)";
    actual.set(WorldFixture.indexOf(actual, key), WorldFixture.entry(key, "minecraft:glass"));

    assertThat(StateComparator.divergentSubStates(expected, WorldFixture.state(actual), 1))
        .containsExactly("chunk(3,-1)");
    assertThat(StateComparator.divergentSubStates(expected, WorldFixture.state(actual), 2))
        .containsExactly("chunk(3,-1)/section(5)");
    assertThat(StateComparator.divergentSubStates(expected, expected, 1)).isEmpty();
  }
}
