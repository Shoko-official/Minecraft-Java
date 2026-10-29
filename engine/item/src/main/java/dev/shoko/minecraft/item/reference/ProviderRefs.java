package dev.shoko.minecraft.item.reference;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
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
    return compound(stack, WellKnownComponents.COOKING_FUEL)
        .flatMap(
            fuel ->
                provider(fuel, "burn_time")
                    .flatMap(
                        burn ->
                            provider(fuel, "speed_multiplier")
                                .map(speed -> new FuelRef(burn, speed))));
  }

  /**
   * Reads the compost reference.
   *
   * @param stack a stack
   * @return the reference, or empty when the stack cannot be composted
   */
  public static Optional<CompostRef> compostable(ItemStack stack) {
    return compound(stack, WellKnownComponents.COMPOSTABLE)
        .flatMap(compost -> provider(compost, "layers").map(CompostRef::new));
  }

  /**
   * Reads the brewing fuel reference.
   *
   * @param stack a stack
   * @return the reference, or empty when the stack is not a brewing fuel
   */
  public static Optional<BrewingFuelRef> brewingFuel(ItemStack stack) {
    return compound(stack, WellKnownComponents.BREWING_FUEL)
        .flatMap(
            fuel ->
                provider(fuel, "speed_multiplier")
                    .flatMap(
                        speed ->
                            provider(fuel, "uses").map(uses -> new BrewingFuelRef(speed, uses))));
  }

  private static Optional<CompoundTag> compound(ItemStack stack, Identifier component) {
    return stack
        .component(component)
        .map(ComponentValue::tag)
        .filter(CompoundTag.class::isInstance)
        .map(CompoundTag.class::cast);
  }

  private static Optional<Identifier> provider(CompoundTag compound, String field) {
    return compound
        .get(field)
        .filter(StringTag.class::isInstance)
        .flatMap(tag -> Identifier.tryParse(((StringTag) tag).value()));
  }
}
