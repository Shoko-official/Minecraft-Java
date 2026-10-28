package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.nestedLists;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Depth of the binary form on writing (R57) for heterogeneous lists holding containers. The list is
 * at binary level L, the wrappers of R63 at L + 1, and a container element inside a wrapper at L +
 * 2 (a scalar element adds no level of its own). Everything written is readable (R57), with the
 * wrappers kept on reading (R63).
 */
class NbtWriterHeterogeneousDepthTest {

  /** {@code [[1], 2]}: a List of Int and an Int, so a heterogeneous list holding a container. */
  static ListTag listAndInt() {
    return list(list(intTag(1)), intTag(2));
  }

  /** What {@link #listAndInt()} becomes after a binary round trip (R63). */
  static ListTag listAndIntWrapped() {
    return list(compound("", list(intTag(1))), compound("", intTag(2)));
  }

  /** {@code [{a: 1}, 2]}: a Compound and an Int, wrapped like any other element (R47). */
  static ListTag compoundAndInt() {
    return list(compound("a", intTag(1)), intTag(2));
  }

  static ListTag compoundAndIntWrapped() {
    return list(compound("", compound("a", intTag(1))), compound("", intTag(2)));
  }

  /** {@code levels} heterogeneous lists, each holding the next one and an Int, the last [1b, 2]. */
  static ListTag heterogeneousChain(int levels) {
    ListTag tree = list(byteTag(1), intTag(2));
    for (int level = 1; level < levels; level++) {
      tree = list(tree, intTag(2));
    }
    return tree;
  }

  /** The same chain once wrapped, as the reader gives it back. */
  static ListTag wrappedChain(int levels) {
    ListTag tree = list(compound("", byteTag(1)), compound("", intTag(2)));
    for (int level = 1; level < levels; level++) {
      tree = list(compound("", tree), compound("", intTag(2)));
    }
    return tree;
  }

  static byte[] write(NbtTag tree) throws NbtException {
    return NbtWriter.of(NbtCompression.NONE).writeNamed(root(tree));
  }

  /** Writes, reads back, checks the wrapped tree, and checks that writing it again is stable. */
  static void assertWritesAndRereadsWrapped(NbtTag tree, NbtTag wrapped) throws NbtException {
    byte[] written = write(tree);
    var reread = NbtReader.of(NbtCompression.NONE).readNamed(written);
    assertThat(reread).contains(root(wrapped));
    assertThat(Hex.format(write(wrapped))).isEqualTo(Hex.format(written));
  }

  @ParameterizedTest(name = "level {0}")
  @ValueSource(ints = {511, 512})
  void refusesHeterogeneousListHoldingAListAtLevel(int level) {
    // spec: core-nbt R57, R63: [[1], 2] at level L has its wrapper at L + 1 and [1] at L + 2,
    // so 513 at least from L = 511: TOO_DEEP without offset, nothing written
    NbtWriterDepthTest.assertTooDeepAndNothingWritten(
        NbtCompression.NONE, nestedLists(level, listAndInt()));
  }

  @ParameterizedTest(name = "level {0}")
  @ValueSource(ints = {511, 512})
  void refusesHeterogeneousListHoldingACompoundAtLevel(int level) {
    // spec: core-nbt R57, R63, R47: the compound of [{a: 1}, 2] is wrapped, then nested in the
    // wrapper: one level below the wrapper
    NbtWriterDepthTest.assertTooDeepAndNothingWritten(
        NbtCompression.NONE, nestedLists(level, compoundAndInt()));
  }

  @ParameterizedTest(name = "level {0}")
  @ValueSource(ints = {1, 100, 509, 510})
  void writesHeterogeneousListHoldingAListAtLevel(int level) throws NbtException {
    // spec: core-nbt R57, R63: [[1], 2] at level 510 reaches 512 (the wrapper at 511, [1] at 512)
    assertWritesAndRereadsWrapped(
        nestedLists(level, listAndInt()), nestedLists(level, listAndIntWrapped()));
  }

  @ParameterizedTest(name = "level {0}")
  @ValueSource(ints = {1, 100, 509, 510})
  void writesHeterogeneousListHoldingACompoundAtLevel(int level) throws NbtException {
    // spec: core-nbt R57, R63: [{a: 1}, 2] at level 510 reaches 512 (the compound at 512)
    assertWritesAndRereadsWrapped(
        nestedLists(level, compoundAndInt()), nestedLists(level, compoundAndIntWrapped()));
  }

  @Test
  void writesAs512BinaryLevels256NestedHeterogeneousLists() throws NbtException {
    // spec: core-nbt R57, R63: each heterogeneous list costs 2 levels (its own, its wrappers);
    // the deepest of 256 is at level 511 and its wrappers at 512
    assertWritesAndRereadsWrapped(heterogeneousChain(256), wrappedChain(256));
  }

  @Test
  void refuses257NestedHeterogeneousListsWithoutWriting() {
    // spec: core-nbt R57, R63: the 257th list would be at binary level 513
    NbtWriterDepthTest.assertTooDeepAndNothingWritten(NbtCompression.NONE, heterogeneousChain(257));
  }

  @Test
  void refusesHeterogeneousChainInEveryContainer() {
    // spec: core-nbt R57: the check does not depend on the compression
    for (NbtCompression compression : NbtCompression.values()) {
      NbtWriterDepthTest.assertTooDeepAndNothingWritten(compression, heterogeneousChain(257));
    }
  }

  @Test
  void homogeneousListsOfListsCostOneLevelEach() throws NbtException {
    // spec: core-nbt R57: contrast with the chain above, a homogeneous list adds no wrapper
    byte[] written = write(nestedLists(512, list(intTag(1))));
    assertThat(NbtReader.of(NbtCompression.NONE).readNamed(written)).isPresent();
  }
}
