package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedMap;

/**
 * The sparse {@code Items} list of block entities and containers (IC-R24, IC-R25): only non-empty
 * stacks are written, each with its {@code Slot} Byte, and an empty list is written as is.
 */
public final class ContainerItemsCodec {

  private static final String SLOT = "Slot";

  private final StackCodec stacks;

  private ContainerItemsCodec(StackCodec stacks) {
    this.stacks = stacks;
  }

  /**
   * Creates a codec.
   *
   * @param stacks the codec of one stack
   * @return the codec
   */
  public static ContainerItemsCodec of(StackCodec stacks) {
    return new ContainerItemsCodec(stacks);
  }

  /**
   * Reads an {@code Items} list.
   *
   * @param items the list of compounds
   * @param size the number of slots of the container
   * @return {@code size} stacks, empty where nothing is written. An entry whose {@code Slot} is
   *     missing, mistyped, outside {@code 0..size-1} or already filled is skipped with an {@code
   *     INVALID_SLOT} warning; the findings of a stack (IC-R26) are kept. The behavior of the game
   *     on these entries is not specified (probe PI6)
   */
  public Decoding<List<ItemStack>> read(ListTag items, int size) {
    List<ItemStack> slots = new ArrayList<>(Collections.nCopies(size, ItemStack.empty()));
    boolean[] filled = new boolean[size];
    List<Finding> findings = new ArrayList<>();
    for (int i = 0; i < items.elements().size(); i++) {
      String path = "Items[" + i + "]";
      if (!(items.elements().get(i) instanceof CompoundTag entry)) {
        return failure(FindingCode.TYPE_MISMATCH, path, "expected a compound");
      }
      OptionalInt slot = slotOf(entry, size, filled);
      if (slot.isEmpty()) {
        findings.add(
            new Finding(
                FindingCode.INVALID_SLOT, path + "." + SLOT, "unusable slot, entry skipped"));
        continue;
      }
      Decoding<ItemStack> decoded = stacks.fromNbt(entry);
      for (Finding finding : decoded.findings()) {
        findings.add(
            new Finding(finding.code(), path + finding.path().substring(1), finding.message()));
      }
      if (!(decoded instanceof Decoding.Success<ItemStack> success)) {
        return new Decoding.Failure<>(findings);
      }
      if (!success.value().isEmpty()) {
        slots.set(slot.getAsInt(), success.value());
        filled[slot.getAsInt()] = true;
      }
    }
    return new Decoding.Success<>(List.copyOf(slots), findings);
  }

  private static Decoding<List<ItemStack>> failure(FindingCode code, String path, String message) {
    return new Decoding.Failure<>(List.of(new Finding(code, path, message)));
  }

  private static OptionalInt slotOf(CompoundTag entry, int size, boolean[] filled) {
    Optional<NbtTag> tag = entry.get(SLOT);
    if (tag.isEmpty()
        || !(tag.get() instanceof NumericTag number)
        || number instanceof FloatTag
        || number instanceof DoubleTag) {
      return OptionalInt.empty();
    }
    long slot = number.longValue();
    boolean usable = slot >= 0 && slot < size && !filled[(int) slot];
    return usable ? OptionalInt.of((int) slot) : OptionalInt.empty();
  }

  /**
   * Writes an {@code Items} list.
   *
   * @param slots the content of every slot, in slot order
   * @return the compounds of the non-empty stacks in slot order, each {@code {count, Slot, id,
   *     components?}} as in the structure templates; an empty list when every slot is empty
   */
  public ListTag write(List<ItemStack> slots) {
    List<NbtTag> entries = new ArrayList<>();
    for (int slot = 0; slot < slots.size(); slot++) {
      ItemStack stack = slots.get(slot);
      if (!stack.isEmpty()) {
        entries.add(entry(slot, stacks.toNbt(stack)));
      }
    }
    return ListTag.of(entries);
  }

  private static CompoundTag entry(int slot, CompoundTag stack) {
    SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
    entry.put("count", stack.get("count").orElseThrow());
    entry.put(SLOT, new ByteTag((byte) slot));
    stack.entries().forEach(entry::putIfAbsent);
    return CompoundTag.of(entry);
  }
}
