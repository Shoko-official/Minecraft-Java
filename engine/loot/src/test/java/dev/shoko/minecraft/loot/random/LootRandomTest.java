package dev.shoko.minecraft.loot.random;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.core.random.XoroshiroRandom;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R63, LT-R65, LT-R68 (priority of the random source of a top-level draw)
class LootRandomTest {

  private static final Identifier SEQUENCE = Identifier.parse("minecraft:blocks/stone");
  private static final SequenceRef NAMED = new SequenceRef.Named(SEQUENCE);
  private static final SequenceRef UNNAMED = new SequenceRef.Unnamed();

  /** A store that hands out one generator per identifier and remembers the requests. */
  private static final class RecordingStore implements RandomSequenceStore {
    private final Map<Identifier, RandomSource> sources = new HashMap<>();
    private final List<Identifier> requested = new ArrayList<>();

    @Override
    public RandomSource sequence(Identifier id) {
      requested.add(id);
      return sources.computeIfAbsent(id, key -> XoroshiroRandom.ofSeed(key.hashCode()));
    }
  }

  private static List<Integer> draws(RandomSource source, int count) {
    List<Integer> values = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      values.add(source.nextInt(1_000_000));
    }
    return values;
  }

  @Test
  void provisionalExplicitSeedGivesAFresh48BitGenerator() {
    // spec: LT-R63 (1)
    RecordingStore store = new RecordingStore();
    RandomSource level = Lcg48Random.ofSeed(1);
    ChosenRandom chosen = LootRandom.choose(42L, NAMED, store, level);
    assertThat(chosen.origin()).isEqualTo(RandomOrigin.EXPLICIT_SEED);
    assertThat(draws(chosen.source(), 8)).isEqualTo(draws(Lcg48Random.ofSeed(42L), 8));
    assertThat(chosen.source()).isNotSameAs(level);
  }

  @Test
  void provisionalExplicitSeedBeatsTheNamedSequence() {
    // spec: LT-R63 (priority 1 over 2): the store is not even asked
    RecordingStore store = new RecordingStore();
    LootRandom.choose(42L, NAMED, store, Lcg48Random.ofSeed(1));
    assertThat(store.requested).isEmpty();
  }

  @Test
  void provisionalNegativeSeedIsAnExplicitSeed() {
    // spec: LT-R63 (only 0 means absent)
    ChosenRandom chosen =
        LootRandom.choose(-7L, NAMED, new RecordingStore(), Lcg48Random.ofSeed(1));
    assertThat(chosen.origin()).isEqualTo(RandomOrigin.EXPLICIT_SEED);
    assertThat(draws(chosen.source(), 4)).isEqualTo(draws(Lcg48Random.ofSeed(-7L), 4));
  }

  @Test
  void provisionalSeedZeroWithANamedSequenceUsesTheSequenceOfTheStore() {
    // spec: LT-R63 (2), LT-R65
    RecordingStore store = new RecordingStore();
    ChosenRandom chosen = LootRandom.choose(0L, NAMED, store, Lcg48Random.ofSeed(1));
    assertThat(chosen.origin()).isEqualTo(RandomOrigin.NAMED_SEQUENCE);
    assertThat(chosen.source()).isSameAs(store.sequence(SEQUENCE));
  }

  @Test
  void provisionalSeedZeroWithoutSequenceUsesTheSourceOfTheLevel() {
    // spec: LT-R63 (3)
    RecordingStore store = new RecordingStore();
    RandomSource level = Lcg48Random.ofSeed(1);
    ChosenRandom chosen = LootRandom.choose(0L, UNNAMED, store, level);
    assertThat(chosen.origin()).isEqualTo(RandomOrigin.LEVEL);
    assertThat(chosen.source()).isSameAs(level);
    assertThat(store.requested).isEmpty();
  }

  @Test
  void provisionalChoosingConsumesNoStep() {
    // spec: LT-R63 (the choice itself draws nothing)
    RecordingStore store = new RecordingStore();
    RandomSource level = Lcg48Random.ofSeed(1);
    RandomSource sequence = store.sequence(SEQUENCE);
    long before = sequence.steps();
    LootRandom.choose(0L, NAMED, store, level);
    ChosenRandom explicit = LootRandom.choose(9L, NAMED, store, level);
    assertThat(level.steps()).isZero();
    assertThat(sequence.steps()).isEqualTo(before);
    assertThat(explicit.source().steps()).isZero();
  }

  @Test
  void provisionalTwoTablesSharingASequenceShareItsState() {
    // spec: LT-R65 (the content depends on the order of the calls)
    RecordingStore store = new RecordingStore();
    ChosenRandom first = LootRandom.choose(0L, NAMED, store, Lcg48Random.ofSeed(1));
    ChosenRandom second = LootRandom.choose(0L, NAMED, store, Lcg48Random.ofSeed(2));
    assertThat(second.source()).isSameAs(first.source());
    long before = second.source().steps();
    first.source().nextInt(10);
    assertThat(second.source().steps()).isGreaterThan(before);
  }

  @Test
  void provisionalTwoDistinctSequencesAreIndependent() {
    // spec: LT-R65
    RecordingStore store = new RecordingStore();
    SequenceRef other = new SequenceRef.Named(Identifier.parse("minecraft:blocks/dirt"));
    ChosenRandom first = LootRandom.choose(0L, NAMED, store, Lcg48Random.ofSeed(1));
    ChosenRandom second = LootRandom.choose(0L, other, store, Lcg48Random.ofSeed(1));
    assertThat(second.source()).isNotSameAs(first.source());
  }

  @Test
  void provisionalTheSameExplicitSeedTwiceGivesTwoEqualIndependentStreams() {
    // spec: LT-R63, LT-R68 (same seed, same content)
    RecordingStore store = new RecordingStore();
    ChosenRandom first = LootRandom.choose(123456789L, NAMED, store, Lcg48Random.ofSeed(1));
    ChosenRandom second = LootRandom.choose(123456789L, NAMED, store, Lcg48Random.ofSeed(2));
    assertThat(second.source()).isNotSameAs(first.source());
    assertThat(draws(first.source(), 16)).isEqualTo(draws(second.source(), 16));
  }

  @Test
  void provisionalDifferentExplicitSeedsGiveDifferentStreams() {
    // spec: LT-R63
    RecordingStore store = new RecordingStore();
    ChosenRandom first = LootRandom.choose(1L, UNNAMED, store, Lcg48Random.ofSeed(1));
    ChosenRandom second = LootRandom.choose(2L, UNNAMED, store, Lcg48Random.ofSeed(1));
    assertThat(draws(first.source(), 16)).isNotEqualTo(draws(second.source(), 16));
  }
}
