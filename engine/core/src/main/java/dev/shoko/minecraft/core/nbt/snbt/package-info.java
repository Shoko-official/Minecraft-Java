/**
 * Text form of NBT values (SNBT): {@link dev.shoko.minecraft.core.nbt.snbt.SnbtParser} reads it
 * (R39 to R51) and {@link dev.shoko.minecraft.core.nbt.snbt.SnbtWriter} writes it (R52 to R56).
 *
 * <p>Spec: {@code docs/specs/core-nbt.md}. Both classes are stateless. Parse failures are {@link
 * dev.shoko.minecraft.core.nbt.NbtException}.
 */
package dev.shoko.minecraft.core.nbt.snbt;
