package dev.shoko.minecraft.item.reference;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Optional;

/**
 * Reads the provider references of a stack from its effective components (RC-R32, RC-R34, RC-R35).
 * A component that is present but whose shape is unusable is treated as absent: the prototypes are
 * checked when the catalog loads.
 */
public final class ProviderRefs {

  private ProviderRefs() {}

  /**
   * Reads the fuel reference.
   *
   * @param stack a stack
   * @return the reference, or empty when the stack is not a fuel
   */
  public static Optional<FuelRef> cookingFuel(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the compost reference.
   *
   * @param stack a stack
   * @return the reference, or empty when the stack cannot be composted
   */
  public static Optional<CompostRef> compostable(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the brewing fuel reference.
   *
   * @param stack a stack
   * @return the reference, or empty when the stack is not a brewing fuel
   */
  public static Optional<BrewingFuelRef> brewingFuel(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }
}
