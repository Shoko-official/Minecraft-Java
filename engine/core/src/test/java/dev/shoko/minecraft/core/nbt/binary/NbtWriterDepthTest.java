package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFailsWithoutOffset;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.nestedLists;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Depth of the binary form on writing (R57), vectors W1 to W4: checked before any byte, wrappers of
 * heterogeneous lists (R63) included; beyond 512, {@code TOO_DEEP} without offset and nothing
 * written.
 */
class NbtWriterDepthTest {

  /** {@code levels} nested compounds under the key "a", the deepest one empty. */
  static CompoundTag nestedCompounds(int levels) {
    CompoundTag tree = CompoundTag.empty();
    for (int level = 1; level < levels; level++) {
      tree = compound("a", tree);
    }
    return tree;
  }

  static ListTag heterogeneous() {
    return list(byteTag(1), intTag(2));
  }

  static void assertTooDeepAndNothingWritten(NbtCompression compression, NbtTag tree) {
    var out = new ByteArrayOutputStream();
    assertFailsWithoutOffset(
        () -> NbtWriter.of(compression).writeNamed(root(tree), out), Reason.TOO_DEEP);
    assertThat(out.size()).isZero();
  }

  @Test
  void writes512NestedLists() throws NbtException {
    // spec: core-nbt R57, W1 (2563 bytes, identical to E09a)
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(nestedLists(512, ListTag.empty())));
    assertThat(Hex.format(written)).isEqualTo(Hex.format(BinaryVectors.nestedListsRoot(512)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(NbtCompression.class)
  void refuses513NestedListsWithoutWriting(NbtCompression compression) {
    // spec: core-nbt R57, W2: TOO_DEEP, no offset, nothing written, in every container
    assertTooDeepAndNothingWritten(compression, nestedLists(513, ListTag.empty()));
  }

  @Test
  void refusesHeterogeneousListAtLevel512() {
    // spec: core-nbt R57, R63, W3: 512 levels in memory, 513 in binary with the wrappers
    assertTooDeepAndNothingWritten(NbtCompression.NONE, nestedLists(512, heterogeneous()));
  }

  @Test
  void writesHeterogeneousListAtLevel511() throws NbtException {
    // spec: core-nbt R57, R63, W4 (2571 bytes, 512 levels in binary)
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(nestedLists(511, heterogeneous())));
    assertThat(Hex.format(written)).isEqualTo(Hex.format(BinaryVectors.w4Bytes()));
  }

  @Test
  void writtenDeepestTreesAreReadable() throws NbtException {
    // spec: core-nbt R57 (everything the writer produces is readable by the reader)
    for (NbtTag tree :
        List.of(nestedLists(512, ListTag.empty()), nestedLists(511, heterogeneous()))) {
      byte[] written = NbtWriter.of(NbtCompression.NONE).writeNamed(root(tree));
      assertThat(NbtReader.of(NbtCompression.NONE).readNamed(written)).isPresent();
    }
  }

  @Test
  void compoundsCountLikeLists() throws NbtException {
    // spec: core-nbt R31, R57: 512 nested compounds written, 513 refused
    byte[] written = NbtWriter.of(NbtCompression.NONE).writeNamed(root(nestedCompounds(512)));
    assertThat(NbtReader.of(NbtCompression.NONE).readNamed(written)).isPresent();
    assertTooDeepAndNothingWritten(NbtCompression.NONE, nestedCompounds(513));
  }

  @Test
  void deepTreeDeepInsideOtherEntriesWritesNothing() {
    // spec: core-nbt R57 (check before producing any byte), like R13: earlier entries are not
    // written either
    var tree =
        compound("first", intTag(1), "deep", nestedLists(512, ListTag.empty()), "last", intTag(2));
    assertTooDeepAndNothingWritten(NbtCompression.GZIP, tree);
  }

  @Test
  void unnamedRootIsCheckedToo() {
    // spec: core-nbt R20, R57
    var out = new ByteArrayOutputStream();
    assertFailsWithoutOffset(
        () ->
            NbtWriter.of(NbtCompression.NONE).writeUnnamed(nestedLists(513, ListTag.empty()), out),
        Reason.TOO_DEEP);
    assertThat(out.size()).isZero();
  }

  @Test
  void veryDeepTreeIsRefusedWithoutStackOverflow() {
    // spec: core-nbt R57 (beyond 512: TOO_DEEP; the unchecked writer exhausted the stack near
    // 1424 levels)
    assertTooDeepAndNothingWritten(NbtCompression.NONE, nestedLists(100_000, ListTag.empty()));
  }
}
