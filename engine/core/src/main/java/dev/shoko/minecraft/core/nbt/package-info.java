/**
 * NBT values (Java edition) and the single failure type of their binary and text forms.
 *
 * <p>Spec: {@code docs/specs/core-nbt.md}; rule numbers Rn in the Javadoc refer to it.
 *
 * <ul>
 *   <li>Model: {@link dev.shoko.minecraft.core.nbt.NbtTag} and its immutable implementations, typed
 *       by {@link dev.shoko.minecraft.core.nbt.TagType}; file roots are {@link
 *       dev.shoko.minecraft.core.nbt.NamedTag}.
 *   <li>Every failure of the sibling packages {@code nbt.binary} (binary form) and {@code nbt.snbt}
 *       (SNBT): the checked {@link dev.shoko.minecraft.core.nbt.NbtException}.
 * </ul>
 *
 * <p>This package depends on neither sibling. Tags are immutable and safe to share between threads.
 * No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.core.nbt;
