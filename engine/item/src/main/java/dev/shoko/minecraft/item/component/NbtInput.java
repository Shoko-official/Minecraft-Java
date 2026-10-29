package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.List;
import java.util.SequencedMap;

/**
 * NBT values for the {@link ShapeReader}: any numeric tag stands for a number of its kind, so a
 * value written with another numeric type than the canonical one is converted, not refused.
 */
final class NbtInput implements ShapeInput<NbtTag> {

  static final NbtInput INSTANCE = new NbtInput();

  private NbtInput() {}

  @Override
  public Kind kind(NbtTag value) {
    return switch (value) {
      case CompoundTag _ -> Kind.OBJECT;
      case ListTag _ -> Kind.LIST;
      case StringTag _ -> Kind.TEXT;
      case ByteTag _, ShortTag _, IntTag _, LongTag _ -> Kind.INTEGER;
      case FloatTag _, DoubleTag _ -> Kind.DECIMAL;
      default -> Kind.OTHER;
    };
  }

  @Override
  public SequencedMap<String, NbtTag> members(NbtTag object) {
    return ((CompoundTag) object).entries();
  }

  @Override
  public List<NbtTag> elements(NbtTag list) {
    return ((ListTag) list).elements();
  }

  @Override
  public String text(NbtTag value) {
    return ((StringTag) value).value();
  }

  @Override
  public long integer(NbtTag value) {
    return ((NumericTag) value).longValue();
  }

  @Override
  public double decimal(NbtTag value) {
    return ((NumericTag) value).doubleValue();
  }

  @Override
  public boolean bool(NbtTag value) {
    return ((NumericTag) value).longValue() != 0;
  }

  @Override
  public boolean integersAreBooleans() {
    return true;
  }

  @Override
  public NbtTag any(NbtTag value, String path) {
    return value;
  }
}
