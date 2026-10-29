package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.registry.ItemType;
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

  private ItemStack() {}

  /**
   * Returns the empty stack (IC-R15).
   *
   * @return the empty stack
   */
  public static ItemStack empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates a stack with the components of the prototype.
   *
   * @param type the item
   * @param count the count; 0 or less, or air, gives the empty stack (IC-R15)
   * @return the stack
   */
  public static ItemStack of(ItemType type, int count) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the stack is empty (IC-R15).
   *
   * @return {@code true} when the item is air or the count is 0 or less
   */
  public boolean isEmpty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the item.
   *
   * @return the item; air for the empty stack
   */
  public ItemType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the count.
   *
   * @return the count, 0 for the empty stack
   */
  public int count() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the modifications over the prototype, normalized (IC-R17).
   *
   * @return the patch
   */
  public ComponentPatch patch() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the effective components: the prototype covered by the patch (IC-R14).
   *
   * @return the components
   */
  public ComponentMap components() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads one effective component.
   *
   * @param id a component identifier
   * @return the value, or empty when the component is absent or removed
   */
  public Optional<ComponentValue> component(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the stack limit of this content.
   *
   * @return the effective {@code max_stack_size} (IC-R5); 64 for the empty stack
   */
  public int maxStackSize() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a stack of the same content with another count.
   *
   * @param count the count; 0 or less gives the empty stack
   * @return the stack
   */
  public ItemStack withCount(int count) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a stack with one component set.
   *
   * @param id a component identifier
   * @param value the value
   * @return the stack
   */
  public ItemStack withComponent(Identifier id, ComponentValue value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a stack with one component removed from the effective components.
   *
   * @param id a component identifier
   * @return the stack
   */
  public ItemStack withoutComponent(Identifier id) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Receives units of another stack, up to the stack limit (IC-R19).
   *
   * @param source the stack that gives; if this stack is empty it receives the content of {@code
   *     source}, otherwise the contents must be equal or nothing moves
   * @return this stack after the move and what remains of the source
   */
  public MergeResult mergeFrom(ItemStack source) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Takes units out of this stack (IC-R20).
   *
   * @param amount the count wanted, bounded by the count available
   * @return the taken stack, of the same content, and what remains
   */
  public SplitResult split(int amount) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Distributes a total in stacks of this content, as {@code give} does (IC-R16, V-I14).
   *
   * @param total the total count
   * @return the stacks of {@link #maxStackSize()} units, the last holding the rest; empty for a
   *     total of 0 or less
   */
  public List<ItemStack> stacksFor(int total) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the item has a durability (IC-R21).
   *
   * @return {@code true} when {@code max_damage} is present
   */
  public boolean isDamageable() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the wear (IC-R21).
   *
   * @return {@code damage}, 0 when absent
   */
  public int damage() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the durability (IC-R21).
   *
   * @return {@code max_damage}, 0 when absent
   */
  public int maxDamage() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Wears the item by use (IC-R21, provisional).
   *
   * @param amount the wear to add
   * @return the stack with {@code damage} raised, the empty stack when {@code damage} reaches
   *     {@code max_damage}; the same stack when the item is not damageable or {@code unbreakable}
   */
  public ItemStack wornBy(int amount) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares by content and count. Empty stacks are equal.
   *
   * @param other another object
   * @return {@code true} when equal
   */
  @Override
  public boolean equals(Object other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Describes the stack for logs and test failures.
   *
   * @return the item, count and patch; its wording is not part of the contract
   */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}
