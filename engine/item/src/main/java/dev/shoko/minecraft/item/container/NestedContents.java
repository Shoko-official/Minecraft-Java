package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;

/**
 * Typed view of the {@code container} component: a list of {@code {slot, item}} whose items are
 * stacks that follow IC-R14 to IC-R20 (IC-R23). The nesting depth limit and the treatment of a
 * bundle in a bundle are not specified (probe PI4): the view reads one level and reports {@code
 * NESTING_TOO_DEEP} only for a limit the caller chooses.
 */
public final class NestedContents {

  /** The deepest nesting any view accepts, so that a hostile chain cannot exhaust the stack. */
  private static final int SAFE_DEPTH = 256;

  private static final List<Identifier> NESTING_COMPONENTS =
      List.of(
          WellKnownComponents.CONTAINER,
          Identifier.minecraft("bundle_contents"),
          Identifier.minecraft("charged_projectiles"));

  private final StackCodec stacks;
  private final int maxDepth;

  private NestedContents(StackCodec stacks, int maxDepth) {
    this.stacks = stacks;
    this.maxDepth = Math.min(maxDepth, SAFE_DEPTH);
  }

  /**
   * Creates a view.
   *
   * @param stacks the codec of one stack
   * @param maxDepth the deepest nesting accepted, counting the stacks of the value as depth 1; a
   *     value above 256 is capped at 256
   * @return the view
   */
  public static NestedContents of(StackCodec stacks, int maxDepth) {
    return new NestedContents(stacks, maxDepth);
  }

  /**
   * Reads the stacks of a {@code container} value. A slot that is negative or already used is
   * skipped with an {@code INVALID_SLOT} warning, as in an {@code Items} list. The stacks held by
   * these stacks in their {@code container}, {@code bundle_contents} or {@code charged_projectiles}
   * are read too, to apply the depth limit.
   *
   * @param value the component value
   * @return the nested stacks in list order, or the failure
   */
  public Decoding<List<SlottedStack>> read(ComponentValue value) {
    List<Finding> warnings = new ArrayList<>();
    try {
      List<SlottedStack> read = readSlotted(value.tag(), 1, "$", warnings);
      return new Decoding.Success<>(List.copyOf(read), List.copyOf(warnings));
    } catch (FindingException refusal) {
      return new Decoding.Failure<>(refusal.findings());
    }
  }

  private List<SlottedStack> readSlotted(
      NbtTag value, int depth, String path, List<Finding> warnings) {
    ListTag list = listOf(value, path);
    List<SlottedStack> read = new ArrayList<>();
    Set<Integer> used = new HashSet<>();
    for (int i = 0; i < list.elements().size(); i++) {
      String at = path + "[" + i + "]";
      if (!(list.elements().get(i) instanceof CompoundTag entry)
          || !(entry.get("slot").orElse(null) instanceof NumericTag slot)
          || isDecimal(slot)
          || !(entry.get("item").orElse(null) instanceof CompoundTag item)) {
        throw new FindingException(
            FindingCode.TYPE_MISMATCH, at, "expected {slot, item} with an integer slot");
      }
      long index = slot.longValue();
      if (index < 0 || index > Integer.MAX_VALUE || !used.add((int) index)) {
        warnings.add(
            new Finding(FindingCode.INVALID_SLOT, at + ".slot", "unusable slot, entry skipped"));
        continue;
      }
      Optional<ItemStack> stack = stack(item, at + ".item", warnings);
      if (stack.isPresent()) {
        checkInner(stack.get(), depth, at, warnings);
        read.add(new SlottedStack((int) index, stack.get()));
      }
    }
    return read;
  }

  private void readPlain(NbtTag value, int depth, String path, List<Finding> warnings) {
    ListTag list = listOf(value, path);
    for (int i = 0; i < list.elements().size(); i++) {
      String at = path + "[" + i + "]";
      if (!(list.elements().get(i) instanceof CompoundTag item)) {
        throw new FindingException(FindingCode.TYPE_MISMATCH, at, "expected a stack");
      }
      stack(item, at, warnings).ifPresent(stack -> checkInner(stack, depth, at, warnings));
    }
  }

  private static ListTag listOf(NbtTag value, String path) {
    if (value instanceof ListTag list) {
      return list;
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a list");
  }

  private static boolean isDecimal(NumericTag number) {
    return number instanceof FloatTag || number instanceof DoubleTag;
  }

  /** Reads one nested stack; empty when it reads as the empty stack. */
  private Optional<ItemStack> stack(CompoundTag item, String path, List<Finding> warnings) {
    Decoding<ItemStack> decoded = stacks.fromNbt(item);
    for (Finding finding : decoded.findings()) {
      warnings.add(
          new Finding(finding.code(), path + finding.path().substring(1), finding.message()));
    }
    if (!(decoded instanceof Decoding.Success<ItemStack> success)) {
      throw new FindingException(decoded.findings());
    }
    return success.value().isEmpty() ? Optional.empty() : Optional.of(success.value());
  }

  /** Reads what a stack holds, one level deeper, and refuses a chain over the limit. */
  private void checkInner(ItemStack stack, int depth, String path, List<Finding> warnings) {
    for (Identifier component : NESTING_COMPONENTS) {
      Optional<NbtTag> held = stack.component(component).map(ComponentValue::tag);
      if (held.isEmpty() || !(held.get() instanceof ListTag list) || list.elements().isEmpty()) {
        continue;
      }
      if (depth + 1 > maxDepth) {
        throw new FindingException(
            FindingCode.NESTING_TOO_DEEP, path, "nested deeper than " + maxDepth);
      }
      String at = path + "." + component.path();
      if (component.equals(WellKnownComponents.CONTAINER)) {
        readSlotted(list, depth + 1, at, warnings);
      } else {
        readPlain(list, depth + 1, at, warnings);
      }
    }
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
