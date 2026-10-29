package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.registry.ItemCatalog;

/**
 * Reads and writes stacks in the three forms of the game (IC-R24 to IC-R29): the NBT of
 * inventories, the JSON of data files and the text of commands. The item registry decides which
 * identifiers are known; component values go through {@code ComponentCodec}.
 */
public final class StackCodec {

  private StackCodec() {}

  /**
   * Creates a codec.
   *
   * @param items the loaded items
   * @return the codec
   */
  public static StackCodec of(ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a stack from NBT (IC-R24, IC-R26): {@code {id, count?, components?}}.
   *
   * @param tag the compound; extra keys such as {@code Slot} are ignored
   * @return the stack. A missing {@code count} is 1. An unknown {@code id} gives the empty stack
   *     with an {@code UNKNOWN_ITEM} warning, and a count of 0 or less the empty stack with {@code
   *     NON_POSITIVE_COUNT} (provisional, probe PI6). A missing {@code id} or a wrongly typed field
   *     is a failure; so is an error in {@code components}
   */
  public Decoding<ItemStack> fromNbt(CompoundTag tag) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a stack to NBT (IC-R24, IC-R27).
   *
   * @param stack a non-empty stack; the empty stack is written as an absence by the caller
   * @return {@code {id, count, components?}} in this key order, {@code count} an Int, {@code
   *     components} only when the normalized patch is not empty
   * @throws IllegalArgumentException when the stack is empty
   */
  public CompoundTag toNbt(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a stack from JSON (IC-R28): {@code {id, count?, components?}}.
   *
   * @param node the JSON object; a missing {@code count} is 1. The empty object is not accepted
   *     here: recipes handle the derived result of IC-R28 themselves
   * @return the stack, or the failure
   */
  public Decoding<ItemStack> fromJson(JsonNode node) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a stack as compact JSON text that the JSON reader of {@code data} reads back to an equal
   * stack.
   *
   * @param stack a non-empty stack
   * @return the JSON text, {@code count} always written
   * @throws IllegalArgumentException when the stack is empty
   */
  public String toJson(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the text form of the {@code minecraft:item_stack} argument of commands (IC-R29): {@code
   * <id>[<component>=<value>, !<component>, ...]} with SNBT values. The count is not part of this
   * form.
   *
   * @param text the argument text
   * @return the stack with a count of 1, or the failure with {@code TEXT_SYNTAX} or the finding of
   *     the component
   */
  public Decoding<ItemStack> fromCommandText(String text) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the text form of commands.
   *
   * @param stack a non-empty stack
   * @return the identifier followed by the bracketed modifications when the patch is not empty
   * @throws IllegalArgumentException when the stack is empty
   */
  public String toCommandText(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }
}
