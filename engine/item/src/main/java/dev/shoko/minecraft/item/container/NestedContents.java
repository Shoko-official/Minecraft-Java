package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Typed view of the {@code container} component: a list of {@code {slot, item}} whose items are
 * stacks that follow IC-R14 to IC-R20 (IC-R23). The nesting depth limit and the treatment of a
 * bundle in a bundle are not specified (probe PI4): the view reads one level and reports {@code
 * NESTING_TOO_DEEP} only for a limit the caller chooses.
 */
public final class NestedContents {

  private final StackCodec stacks;
  private final int maxDepth;

  private NestedContents(StackCodec stacks, int maxDepth) {
    this.stacks = stacks;
    this.maxDepth = maxDepth;
  }

  /**
   * Creates a view.
   *
   * @param stacks the codec of one stack
   * @param maxDepth the deepest nesting accepted, counting the stacks of the value as depth 1
   * @return the view
   */
  public static NestedContents of(StackCodec stacks, int maxDepth) {
    return new NestedContents(stacks, maxDepth);
  }

  /**
   * Reads the stacks of a {@code container} value.
   *
   * @param value the component value
   * @return the nested stacks in list order, or the failure
   */
  public Decoding<List<SlottedStack>> read(ComponentValue value) {
    List<Finding> warnings = new ArrayList<>();
    try {
      List<SlottedStack> read = readAt(value, 1, "$", warnings);
      return new Decoding.Success<>(List.copyOf(read), List.copyOf(warnings));
    } catch (FindingException refusal) {
      return new Decoding.Failure<>(refusal.findings());
    }
  }

  private List<SlottedStack> readAt(
      ComponentValue value, int depth, String path, List<Finding> warnings) {
    if (!(value.tag() instanceof ListTag list)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a list");
    }
    List<SlottedStack> read = new ArrayList<>();
    for (int i = 0; i < list.elements().size(); i++) {
      String at = path + "[" + i + "]";
      Optional<SlottedStack> entry = entry(list.elements().get(i), at, warnings);
      if (entry.isPresent()) {
        checkInner(entry.get().stack(), depth, at, warnings);
        read.add(entry.get());
      }
    }
    return read;
  }

  /** Reads one {@code {slot, item}} entry; empty when the item reads as the empty stack. */
  private Optional<SlottedStack> entry(NbtTag element, String path, List<Finding> warnings) {
    if (!(element instanceof CompoundTag entry)
        || !(entry.get("slot").orElse(null) instanceof NumericTag slot)
        || !(entry.get("item").orElse(null) instanceof CompoundTag item)) {
      throw new FindingException(
          FindingCode.TYPE_MISMATCH, path, "expected {slot, item} with a numeric slot");
    }
    Decoding<ItemStack> decoded = stacks.fromNbt(item);
    for (Finding finding : decoded.findings()) {
      warnings.add(
          new Finding(
              finding.code(), path + ".item" + finding.path().substring(1), finding.message()));
    }
    if (!(decoded instanceof Decoding.Success<ItemStack> success)) {
      throw new FindingException(decoded.findings());
    }
    return success.value().isEmpty()
        ? Optional.empty()
        : Optional.of(new SlottedStack(slot.intValue(), success.value()));
  }

  private void checkInner(ItemStack stack, int depth, String path, List<Finding> warnings) {
    Optional<ComponentValue> container = stack.component(WellKnownComponents.CONTAINER);
    if (container.isEmpty()
        || !(container.get().tag() instanceof ListTag list)
        || list.elements().isEmpty()) {
      return;
    }
    if (depth + 1 > maxDepth) {
      throw new FindingException(
          FindingCode.NESTING_TOO_DEEP, path, "nested deeper than " + maxDepth);
    }
    readAt(container.get(), depth + 1, path + ".container", warnings);
  }

  /**
   * Builds a {@code container} value.
   *
   * @param stacks the nested stacks
   * @return the value
   */
  public ComponentValue write(List<SlottedStack> stacks) {
    List<NbtTag> entries = new ArrayList<>();
    for (SlottedStack slotted : stacks) {
      SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
      entry.put("slot", new IntTag(slotted.slot()));
      entry.put("item", this.stacks.toNbt(slotted.stack()));
      entries.add(CompoundTag.of(entry));
    }
    return new ComponentValue(ListTag.of(entries));
  }
}
