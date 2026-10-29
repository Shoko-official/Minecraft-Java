package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R24 to IC-R26 (adversarial): Items lists mutated from a fixed
// seed. The only failure mode is a Decoding result; a list that reads must write and read again to
// the same stacks, without findings.
class ContainerItemsAttackTest {

  private static final String[] IDS = {
    "minecraft:stone",
    "minecraft:dirt",
    "minecraft:potion",
    "minecraft:nope",
    "Minecraft:Stone",
    "",
    "::",
    "minecraft:",
    "stone"
  };
  private static final int[] COUNTS = {
    1, 2, 16, 64, 99, 100, 127, 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE
  };

  private static NbtTag randomSlot(Random random, int size) {
    return switch (random.nextInt(9)) {
      case 0 -> new ByteTag((byte) random.nextInt(Math.max(size, 1)));
      case 1 -> new ByteTag((byte) (random.nextInt(256) - 128));
      case 2 -> new IntTag(random.nextInt(size + 3) - 1);
      case 3 -> new ShortTag((short) random.nextInt(size + 1));
      case 4 -> new StringTag(Integer.toString(random.nextInt(size + 1)));
      case 5 -> new LongTag(random.nextLong());
      case 6 -> ListTag.empty();
      case 7 -> CompoundTag.empty();
      default -> new ByteTag((byte) 0);
    };
  }

  private static NbtTag randomId(Random random) {
    return switch (random.nextInt(10)) {
      case 0 -> new IntTag(7);
      case 1 -> ListTag.empty();
      default -> new StringTag(IDS[random.nextInt(IDS.length)]);
    };
  }

  private static NbtTag randomCount(Random random) {
    return switch (random.nextInt(8)) {
      case 0 -> new ByteTag((byte) random.nextInt(100));
      case 1 -> new StringTag("3");
      case 2 -> DoubleTag.of(2.5);
      default -> new IntTag(COUNTS[random.nextInt(COUNTS.length)]);
    };
  }

  private static CompoundTag single(String key, NbtTag value) {
    SequencedMap<String, NbtTag> map = new LinkedHashMap<>();
    map.put(key, value);
    return CompoundTag.of(map);
  }

  private static NbtTag randomComponents(Random random) {
    return switch (random.nextInt(7)) {
      case 0 -> single("minecraft:rarity", new StringTag("epic"));
      case 1 -> single("minecraft:nonexistent", new IntTag(1));
      case 2 -> single("!minecraft:rarity", CompoundTag.empty());
      case 3 -> single("minecraft:potion_contents", new IntTag(5));
      case 4 -> new IntTag(5);
      default -> CompoundTag.empty();
    };
  }

  private static NbtTag randomEntry(Random random, int size) {
    if (random.nextInt(12) == 0) {
      return random.nextBoolean() ? new IntTag(3) : new StringTag("stone");
    }
    SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
    if (random.nextInt(10) != 0) {
      entry.put("count", randomCount(random));
    }
    if (random.nextInt(8) != 0) {
      entry.put("Slot", randomSlot(random, size));
    }
    if (random.nextInt(10) != 0) {
      entry.put("id", randomId(random));
    }
    if (random.nextInt(3) == 0) {
      entry.put("components", randomComponents(random));
    }
    return CompoundTag.of(entry);
  }

  private static ListTag randomList(Random random, int size) {
    List<NbtTag> entries = new ArrayList<>();
    int length = random.nextInt(9);
    for (int i = 0; i < length; i++) {
      entries.add(randomEntry(random, size));
    }
    return ListTag.of(entries);
  }

  @Test
  @Timeout(60)
  void readNeverFailsOtherwiseThanByADecodingResult() {
    // spec: IC-R25, IC-R26
    ContainerItemsCodec codec = ContainerItemsCodec.of(TestItems.stackCodec());
    Random random = new Random(0x17E45L);
    for (int i = 0; i < 4000; i++) {
      int size = random.nextInt(31);
      Decoding<List<ItemStack>> decoding = codec.read(randomList(random, size), size);
      boolean hasError =
          decoding.findings().stream().anyMatch(f -> f.code().severity() == Severity.ERROR);
      if (decoding instanceof Decoding.Success<List<ItemStack>> success) {
        assertThat(hasError).as("success with an error finding at round %d", i).isFalse();
        assertThat(success.value()).as("round %d", i).hasSize(size);
      } else {
        assertThat(hasError).as("failure without an error finding at round %d", i).isTrue();
      }
      for (Finding finding : decoding.findings()) {
        assertThat(finding.path()).isNotNull();
        assertThat(finding.code()).isNotNull();
      }
    }
  }

  @Test
  @Timeout(60)
  void whatReadsWritesAndReadsAgainToTheSameStacksWithoutFinding() {
    // spec: IC-R24, IC-R25 (round trip of every accepted list)
    ContainerItemsCodec codec = ContainerItemsCodec.of(TestItems.stackCodec());
    Random random = new Random(0x5107L);
    int accepted = 0;
    for (int i = 0; i < 4000; i++) {
      int size = random.nextInt(31);
      Decoding<List<ItemStack>> decoding = codec.read(randomList(random, size), size);
      if (!(decoding instanceof Decoding.Success<List<ItemStack>> success)) {
        continue;
      }
      accepted++;
      ListTag written = codec.write(success.value());
      Decoding<List<ItemStack>> again = codec.read(written, size);
      assertThat(again.findings()).as("round %d", i).isEmpty();
      assertThat(again).isInstanceOf(Decoding.Success.class);
      assertThat(((Decoding.Success<List<ItemStack>>) again).value())
          .as("round %d", i)
          .isEqualTo(success.value());
    }
    assertThat(accepted).isPositive();
  }

  @Test
  @Timeout(60)
  void writtenListsHoldOnlyNonEmptyStacksWithDistinctByteSlots() {
    // spec: IC-R25 (sparse list, one entry per non-empty slot)
    ContainerItemsCodec codec = ContainerItemsCodec.of(TestItems.stackCodec());
    Random random = new Random(0xA11CEL);
    for (int i = 0; i < 500; i++) {
      List<ItemStack> slots = new ArrayList<>();
      int size = random.nextInt(40);
      int nonEmpty = 0;
      for (int slot = 0; slot < size; slot++) {
        boolean filled = random.nextInt(3) == 0;
        slots.add(filled ? TestItems.stack("stone", 1 + random.nextInt(64)) : ItemStack.empty());
        nonEmpty += filled ? 1 : 0;
      }
      ListTag written = codec.write(slots);
      assertThat(written.elements()).hasSize(nonEmpty);
      int previous = -1;
      for (NbtTag element : written.elements()) {
        int slot = ((CompoundTag) element).getByte("Slot");
        assertThat(slot).isGreaterThan(previous);
        assertThat(slots.get(slot).isEmpty()).isFalse();
        previous = slot;
      }
    }
  }
}
