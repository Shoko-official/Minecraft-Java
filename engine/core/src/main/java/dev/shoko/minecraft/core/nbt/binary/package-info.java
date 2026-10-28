/**
 * Binary form of NBT values (R1 to R35): {@link dev.shoko.minecraft.core.nbt.binary.NbtReader} and
 * {@link dev.shoko.minecraft.core.nbt.binary.NbtWriter}, inside the containers of {@link
 * dev.shoko.minecraft.core.nbt.binary.NbtCompression}.
 *
 * <p>Spec: {@code docs/specs/core-nbt.md}. Readers and writers hold no state between calls and are
 * safe to share between threads. Failures are {@link dev.shoko.minecraft.core.nbt.NbtException}.
 */
package dev.shoko.minecraft.core.nbt.binary;
