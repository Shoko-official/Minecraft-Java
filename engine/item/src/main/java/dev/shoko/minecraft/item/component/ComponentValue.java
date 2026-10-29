package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.NbtTag;

/**
 * The value of one component, in its canonical NBT form (IC-R18). Two values are equal when their
 * trees are equal; floats compare by bits, so {@code -0.0} and {@code 0.0} differ. A component
 * present with an empty compound is significant and differs from an absent component (IC-R6b).
 *
 * @param tag the value, never {@code null}
 */
public record ComponentValue(NbtTag tag) {}
