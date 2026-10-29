package dev.shoko.minecraft.loot.random;

/**
 * Receives the {@link DrawEvent}s of an evaluation. The observer is part of the loot context; it
 * must not draw from the source it observes.
 */
@FunctionalInterface
public interface DrawObserver {

  /** Observer that ignores every event. */
  DrawObserver NONE = event -> {};

  /**
   * Called once per decision, right after it.
   *
   * @param event the decision and the steps it consumed
   */
  void onDraw(DrawEvent event);
}
