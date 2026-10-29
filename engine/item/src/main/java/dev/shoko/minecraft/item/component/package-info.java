/**
 * Data components: the 122 component types, the maps and patches that hold their values, and the
 * decoding of their JSON and NBT forms.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R6 to IC-R13, IC-R22). A component
 * value is held in its canonical NBT form ({@link
 * dev.shoko.minecraft.item.component.ComponentValue}): NBT is the form of inventories and block
 * entities, and the equality of values is the structural equality of {@code core-nbt}, floats
 * compared by bits (IC-R18). The JSON form of recipes and the text form of commands convert to and
 * from it.
 *
 * <p>Component values and maps are immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.component;
