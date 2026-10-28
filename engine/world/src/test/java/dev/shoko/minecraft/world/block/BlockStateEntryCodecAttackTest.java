package dev.shoko.minecraft.world.block;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.TreeMutator;
import dev.shoko.minecraft.world.support.Warnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on {@link BlockStateEntryCodec}: reading never fails (BS-R22 b). */
class BlockStateEntryCodecAttackTest {

  private static final List<WarningCode> ALLOWED =
      List.of(
          WarningCode.DEFAULTED_PROPERTY,
          WarningCode.UNKNOWN_BLOCK,
          WarningCode.UNKNOWN_PROPERTY,
          WarningCode.INVALID_PROPERTY_VALUE,
          WarningCode.MALFORMED_PALETTE_ENTRY);
  private static final String[] JUNK = {"", "true", "false", "w", "0", "99", "x", "north", "weird"};

  static Stream<Long> seeds() {
    return LongStream.range(0, 80).boxed();
  }

  private static CompoundTag randomEntry(BlockStateRegistry registry, SplittableRandom random) {
    BlockType block = registry.blocks().get(random.nextInt(registry.blockCount()));
    SequencedMap<String, NbtTag> properties = new LinkedHashMap<>();
    for (BlockProperty property : block.properties()) {
      if (random.nextInt(4) == 0) {
        continue;
      }
      String value =
          random.nextInt(4) == 0
              ? JUNK[random.nextInt(JUNK.length)]
              : property.values().get(random.nextInt(property.values().size()));
      properties.put(property.name(), str(value));
    }
    if (random.nextInt(5) == 0) {
      properties.put(JUNK[random.nextInt(JUNK.length)], str("v"));
    }
    String id = random.nextInt(10) == 0 ? "probe:" + random.nextInt(5) : block.id();
    return random.nextBoolean() || properties.isEmpty()
        ? compound("id", str(id))
        : compound("id", str(id), "properties", CompoundTag.of(properties));
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void everyEntryDecodesToAStateOfTheRegistryAndOnlyWarnsWithTheListedCodes(long seed) {
    // spec: BS-R19 to BS-R22 (no entry is a failure; every correction is a warning)
    BlockStateRegistry registry = Blocks.registry();
    SplittableRandom random = new SplittableRandom(seed);
    TreeMutator mutator = new TreeMutator(seed);
    for (int round = 0; round < 30; round++) {
      Warnings warnings = new Warnings();
      BlockStateEntryCodec codec = BlockStateEntryCodec.of(registry, warnings);
      CompoundTag entry = randomEntry(registry, random);
      NbtTag tag = round % 3 == 0 ? mutator.mutate(entry, 1 + random.nextInt(3)) : entry;

      BlockState state = codec.decode(tag);

      assertThat(state).isNotNull();
      assertThat(state.id()).isBetween(0, registry.stateCount() - 1);
      assertThat(registry.state(state.id())).isEqualTo(state);
      assertThat(warnings.codes()).isSubsetOf(ALLOWED);
      Warnings clean = new Warnings();
      BlockStateEntryCodec strict = BlockStateEntryCodec.of(registry, clean);
      assertThat(strict.decode(strict.encode(state))).isEqualTo(state);
      assertThat(clean.isEmpty()).isTrue();
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void randomTagsOfAnyKindAreTreatedAsUnknownBlocks(long seed) {
    // spec: BS-R22 a (an entry that is neither String nor Compound with a String id: air and a
    // warning)
    BlockStateRegistry registry = Blocks.registry();
    TreeMutator mutator = new TreeMutator(1_000 + seed);
    for (int round = 0; round < 20; round++) {
      Warnings warnings = new Warnings();
      NbtTag tag = mutator.randomTag(2);

      BlockState state = BlockStateEntryCodec.of(registry, warnings).decode(tag);

      assertThat(state).isNotNull();
      assertThat(warnings.codes()).isSubsetOf(ALLOWED);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void validEntriesNeverWarnAndKeepEveryPropertyValue(long seed) {
    // spec: BS-R16 to BS-R18 (a full entry in any key order)
    BlockStateRegistry registry = Blocks.registry();
    SplittableRandom random = new SplittableRandom(2_000 + seed);
    for (int round = 0; round < 30; round++) {
      BlockState expected = registry.state(random.nextInt(registry.stateCount()));
      List<Map.Entry<String, String>> shuffled =
          new java.util.ArrayList<>(expected.values().entrySet());
      java.util.Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
      SequencedMap<String, NbtTag> properties = new LinkedHashMap<>();
      shuffled.forEach(e -> properties.put(e.getKey(), str(e.getValue())));
      CompoundTag entry =
          properties.isEmpty()
              ? compound("id", str(expected.block().id()))
              : compound(
                  "id", str(expected.block().id()), "properties", CompoundTag.of(properties));
      Warnings warnings = new Warnings();

      BlockState state = BlockStateEntryCodec.of(registry, warnings).decode(entry);

      assertThat(state).isEqualTo(expected);
      assertThat(warnings.isEmpty()).isTrue();
    }
  }
}
