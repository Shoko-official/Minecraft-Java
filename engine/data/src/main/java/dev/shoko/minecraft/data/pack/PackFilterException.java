package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.pack.meta.FilterBudgetException;
import java.io.IOException;

/**
 * A block filter of a pack is too costly to evaluate (see {@link FilterBudgetException}): the stack
 * cannot be built with that pack. The caller leaves the pack out and reports it.
 */
public final class PackFilterException extends IOException {

  private static final long serialVersionUID = 1L;

  private final transient String pack;

  PackFilterException(String pack, FilterBudgetException cause) {
    super("the block filter of pack " + pack + " is too costly to evaluate", cause);
    this.pack = pack;
  }

  /**
   * Returns the pack whose filter is unmanageable.
   *
   * @return the pack identifier
   */
  public String pack() {
    return pack;
  }
}
