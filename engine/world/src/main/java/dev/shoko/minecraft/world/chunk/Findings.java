package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.WarningSink;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Internal: the warnings of one read, in the order they were made. */
final class Findings implements WarningSink {

  private final List<ReadWarning> warnings = new ArrayList<>();

  @Override
  public void warn(WarningCode code, String message) {
    warnings.add(new ReadWarning(code, message));
  }

  List<ReadWarning> warnings() {
    return List.copyOf(warnings);
  }

  /** Returns a sink that puts a prefix in front of every message it forwards. */
  WarningSink prefixed(String prefix) {
    return (code, message) -> warn(code, prefix + message);
  }

  /**
   * Reads an optional field of a fixed tag type. An absent field gives empty without a warning; a
   * field of another type gives empty and a {@link WarningCode#FIELD_WRONG_TYPE} warning (CF-R35).
   */
  <T extends NbtTag> Optional<T> optional(
      CompoundTag parent, String key, Class<T> type, String path) {
    NbtTag tag = parent.entries().get(key);
    if (tag == null) {
      return Optional.empty();
    }
    if (type.isInstance(tag)) {
      return Optional.of(type.cast(tag));
    }
    warn(
        WarningCode.FIELD_WRONG_TYPE,
        path + key + " is a " + tag.type() + ", expected " + type.getSimpleName() + ", ignored");
    return Optional.empty();
  }
}
