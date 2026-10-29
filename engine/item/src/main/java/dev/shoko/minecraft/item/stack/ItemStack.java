package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.registry.ItemType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An item, a count and the modifications of its components (IC-R14). Immutable.
 *
 * <p>All empty stacks are equivalent: they are equal and write as an absence (IC-R15). The count of
 * a stack is not clamped to the stack size of its item (provisional, probe PI1); {@link
 * #stacksFor(int)} is what splits a total.
 */
public final class ItemStack {

  /** Largest count of a stack and bound of the quantity arguments of commands (IC-R16). */
  public static final int MAX_COUNT = 99;

  private static final int DEFAULT_STACK_SIZE = 64;

  private static final ItemType EMPTY_TYPE =
      new ItemType(Identifier.minecraft("air"), 0, ComponentMap.empty());

  private static final ItemStack EMPTY = new ItemStack(EMPTY_TYPE, 0, ComponentPatch.empty());

  private final ItemType type;
  private final int count;
  private final ComponentPatch patch;

  private ItemStack(ItemType type, int count, ComponentPatch patch) {
    this.type = type;
    this.count = count;
    this.patch = patch;
  }

  /**
   * Returns the empty stack (IC-R15).
   *
   * @return the empty stack
   */
  public static ItemStack empty() {
    return EMPTY;
  }

  /**
   * Creates a stack with the components of the prototype.
   *
   * @param type the item
   * @param count the count; 0 or less, or air, gives the empty stack (IC-R15)
   * @return the stack
   */
  public static ItemStack of(ItemType type, int count) {
    return of(type, count, ComponentPatch.empty());
  }

  /**
   * Creates a stack with modified components. The patch is stored in the normal form of IC-R17.
   *
   * @param type the item
   * @param count the count; 0 or less, or air, gives the empty stack (IC-R15)
   * @param patch the modifications over the prototype
   * @return the stack
   */
  public static ItemStack of(ItemType type, int count, ComponentPatch patch) {
    if (count <= 0 || type.isAir()) {
      return EMPTY;
    }
    return new ItemStack(type, count, patch.normalizedAgainst(type.prototype()));
  }

  /**
   * Tells whether the stack is empty (IC-R15).
   *
   * @return {@code true} when the item is air or the count is 0 or less
   */
  public boolean isEmpty() {
    return count <= 0;
  }

  /**
   * Returns the item.
   *
   * @return the item; air for the empty stack
   */
  public ItemType type() {
    return type;
  }

  /**
   * Returns the count.
   *
   * @return the count, 0 for the empty stack
   */
  public int count() {
    return count;
  }

  /**
   * Returns the modifications over the prototype, normalized (IC-R17).
   *
   * @return the patch
   */
  public ComponentPatch patch() {
    return patch;
  }

  /**
   * Returns the effective components: the prototype covered by the patch (IC-R14).
   *
   * @return the components
   */
  public ComponentMap components() {
    return type.prototype().apply(patch);
  }

  /**
   * Reads one effective component.
   *
   * @param id a component identifier
   * @return the value, or empty when the component is absent or removed
   */
  public Optional<ComponentValue> component(Identifier id) {
    ComponentChange change = patch.changes().get(id);
    if (change == null) {
      return type.prototype().get(id);
    }
    return change instanceof ComponentChange.Set set ? Optional.of(set.value()) : Optional.empty();
  }

  /**
   * Returns the stack limit of this content.
   *
   * @return the effective {@code max_stack_size} (IC-R5), at least 1 and at most {@link
   *     #MAX_COUNT}; 64 for the empty stack
   */
  public int maxStackSize() {
    if (isEmpty()) {
      return DEFAULT_STACK_SIZE;
    }
    return Math.clamp(
        intComponent(WellKnownComponents.MAX_STACK_SIZE, DEFAULT_STACK_SIZE), 1, MAX_COUNT);
  }

  private int intComponent(Identifier id, int fallback) {
    return component(id)
        .map(ComponentValue::tag)
        .filter(IntTag.class::isInstance)
        .map(tag -> ((IntTag) tag).value())
        .orElse(fallback);
  }

  /**
   * Returns a stack of the same content with another count.
   *
   * @param count the count; 0 or less gives the empty stack
   * @return the stack
   */
  public ItemStack withCount(int count) {
    if (count <= 0 || isEmpty()) {
      return EMPTY;
    }
    return count == this.count ? this : new ItemStack(type, count, patch);
  }

  /**
   * Returns a stack with one component set.
   *
   * @param id a component identifier
   * @param value the value
   * @return the stack
   */
  public ItemStack withComponent(Identifier id, ComponentValue value) {
    return isEmpty() ? EMPTY : of(type, count, patch.withSet(id, value));
  }

  /**
   * Returns a stack with one component removed from the effective components.
   *
   * @param id a component identifier
   * @return the stack
   */
  public ItemStack withoutComponent(Identifier id) {
    return isEmpty() ? EMPTY : of(type, count, patch.withRemoved(id));
  }

  /**
   * Compares the content of two stacks, the count ignored (IC-R18): the same item and the same
   * effective components, value by value, floats by bits. A removed component and a component
   * absent from the prototype are equivalent. Two empty stacks have the same content.
   *
   * @param other another stack
   * @return {@code true} when the stacks can merge
   */
  public boolean sameContent(ItemStack other) {
    if (isEmpty() || other.isEmpty()) {
      return isEmpty() && other.isEmpty();
    }
    return type.id().equals(other.type.id()) && patch.equals(other.patch);
  }

  /**
   * Receives units of another stack, up to the stack limit (IC-R19).
   *
   * @param source the stack that gives; if this stack is empty it receives the content of {@code
   *     source}, otherwise the contents must be equal or nothing moves
   * @return this stack after the move and what remains of the source
   */
  public MergeResult mergeFrom(ItemStack source) {
    if (source.isEmpty()) {
      return new MergeResult(this, EMPTY);
    }
    if (!isEmpty() && !sameContent(source)) {
      return new MergeResult(this, source);
    }
    ItemStack content = isEmpty() ? source : this;
    int held = isEmpty() ? 0 : count;
    int moved = Math.min(Math.max(0, content.maxStackSize() - held), source.count);
    return new MergeResult(content.withCount(held + moved), source.withCount(source.count - moved));
  }

  /**
   * Takes units out of this stack (IC-R20).
   *
   * @param amount the count wanted, bounded by the count available
   * @return the taken stack, of the same content, and what remains
   */
  public SplitResult split(int amount) {
    int taken = Math.clamp(amount, 0, count);
    return new SplitResult(withCount(taken), withCount(count - taken));
  }

  /**
   * Distributes a total in stacks of this content, as {@code give} does (IC-R16, V-I14).
   *
   * @param total the total count
   * @return the stacks of {@link #maxStackSize()} units, the last holding the rest; empty for a
   *     total of 0 or less
   */
  public List<ItemStack> stacksFor(int total) {
    List<ItemStack> stacks = new ArrayList<>();
    if (isEmpty()) {
      return stacks;
    }
    int max = maxStackSize();
    for (int left = total; left > 0; left -= max) {
      stacks.add(withCount(Math.min(max, left)));
    }
    return stacks;
  }

  /**
   * Tells whether the item has a durability (IC-R21).
   *
   * @return {@code true} when {@code max_damage} is present
   */
  public boolean isDamageable() {
    return component(WellKnownComponents.MAX_DAMAGE).isPresent();
  }

  /**
   * Returns the wear (IC-R21).
   *
   * @return {@code damage}, 0 when absent
   */
  public int damage() {
    return intComponent(WellKnownComponents.DAMAGE, 0);
  }

  /**
   * Returns the durability (IC-R21).
   *
   * @return {@code max_damage}, 0 when absent
   */
  public int maxDamage() {
    return intComponent(WellKnownComponents.MAX_DAMAGE, 0);
  }

  /**
   * Wears the item by use (IC-R21, provisional).
   *
   * @param amount the wear to add
   * @return the stack with {@code damage} raised, the empty stack when {@code damage} reaches
   *     {@code max_damage}; the same stack when the item is not damageable or {@code unbreakable}
   */
  public ItemStack wornBy(int amount) {
    if (!isDamageable() || component(WellKnownComponents.UNBREAKABLE).isPresent()) {
      return this;
    }
    long worn = (long) damage() + amount;
    if (worn >= maxDamage()) {
      return EMPTY;
    }
    return withComponent(WellKnownComponents.DAMAGE, new ComponentValue(new IntTag((int) worn)));
  }

  /**
   * Compares by content and count. Empty stacks are equal.
   *
   * @param other another object
   * @return {@code true} when equal
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ItemStack that)) {
      return false;
    }
    return sameContent(that) && (isEmpty() || count == that.count);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return isEmpty() ? 0 : (31 * type.id().hashCode() + patch.hashCode()) * 31 + count;
  }

  /**
   * Describes the stack for logs and test failures.
   *
   * @return the item, count and patch; its wording is not part of the contract
   */
  @Override
  public String toString() {
    return isEmpty() ? "ItemStack[empty]" : count + "x " + type.id() + patch.changes();
  }
}
