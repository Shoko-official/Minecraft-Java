package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.loot.random.DrawEvent;
import dev.shoko.minecraft.loot.random.DrawObserver;
import dev.shoko.minecraft.loot.random.DrawReason;
import java.util.ArrayList;
import java.util.List;

/** A {@link DrawObserver} that keeps every event, in order. */
public final class DrawLog implements DrawObserver {

  private final List<DrawEvent> events = new ArrayList<>();

  @Override
  public void onDraw(DrawEvent event) {
    events.add(event);
  }

  /** All events in order of occurrence. */
  public List<DrawEvent> events() {
    return List.copyOf(events);
  }

  /** The reasons of all events, in order. */
  public List<DrawReason> reasons() {
    return events.stream().map(DrawEvent::reason).toList();
  }

  /** The reasons of the events that consumed at least one step. */
  public List<DrawReason> consumingReasons() {
    return events.stream()
        .filter(e -> e.stepsAfter() > e.stepsBefore())
        .map(DrawEvent::reason)
        .toList();
  }

  /** The total number of steps consumed by all events. */
  public long totalSteps() {
    return events.stream().mapToLong(e -> e.stepsAfter() - e.stepsBefore()).sum();
  }
}
