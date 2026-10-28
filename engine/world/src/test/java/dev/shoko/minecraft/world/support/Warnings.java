package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.WarningSink;
import java.util.ArrayList;
import java.util.List;

/** A {@link WarningSink} that keeps what it receives, in order. */
public final class Warnings implements WarningSink {

  private final List<ReadWarning> received = new ArrayList<>();

  @Override
  public void warn(WarningCode code, String message) {
    received.add(new ReadWarning(code, message));
  }

  public List<ReadWarning> all() {
    return List.copyOf(received);
  }

  public List<WarningCode> codes() {
    return received.stream().map(ReadWarning::code).toList();
  }

  public boolean isEmpty() {
    return received.isEmpty();
  }
}
