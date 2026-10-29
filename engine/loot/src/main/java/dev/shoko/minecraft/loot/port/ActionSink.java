package dev.shoko.minecraft.loot.port;

/**
 * Receives the side effects that enchantment effects ask for (LT-R17). The pure draw of a loot
 * table never uses it. {@code entity} and {@code game} execute the intents; tests record them in a
 * list.
 */
@FunctionalInterface
public interface ActionSink {

  /**
   * Accepts one intent, in the order the effects were evaluated.
   *
   * @param intent what should happen
   */
  void accept(EffectIntent intent);
}
