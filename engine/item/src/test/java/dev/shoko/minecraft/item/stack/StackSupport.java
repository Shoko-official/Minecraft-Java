package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;

/**
 * Helpers shared by the stack tests. String-valued components are the only ones whose canonical NBT
 * form is certain (a String tag), so they carry most of the modifications.
 */
final class StackSupport {

  private StackSupport() {}

  /** Reads SNBT where {@code '} stands for {@code "}. */
  static CompoundTag nbt(String relaxed) {
    return Nbt.compound(relaxed.replace('\'', '"'));
  }

  static Identifier rarityId() {
    return TestItems.id("rarity");
  }

  /** The value of a string component such as {@code rarity}. */
  static ComponentValue text(String value) {
    return new ComponentValue(new StringTag(value));
  }

  /** A stack whose rarity is replaced. */
  static ItemStack withRarity(String path, int count, String rarity) {
    return TestItems.stack(path, count).withComponent(rarityId(), text(rarity));
  }

  /** The value of the {@code unbreakable} presence component: an empty compound (IC-R6b). */
  static ComponentValue presence() {
    return new ComponentValue(CompoundTag.empty());
  }
}
