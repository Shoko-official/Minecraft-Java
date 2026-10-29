package dev.shoko.minecraft.data.pack.meta;

/**
 * A regular expression of {@code filter.block} is too costly to evaluate on a name: it read more
 * characters than the budget of {@link FilterRule#matches(String, String)} allows, or it needs more
 * stack than even the large evaluation stack gives. A pack author can write such an expression on
 * purpose, so the pack that holds it is refused rather than the load frozen.
 */
public final class FilterBudgetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  FilterBudgetException(String message) {
    super(message, null, false, false);
  }
}
