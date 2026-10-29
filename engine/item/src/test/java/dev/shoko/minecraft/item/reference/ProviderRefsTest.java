package dev.shoko.minecraft.item.reference;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R32 (cooking_fuel), RC-R34 (compostable), RC-R35 (brewing_fuel) and DC1:
// item only reads the identifiers of the providers from the prototype; loot evaluates them. The
// shapes are the ones of the prototypes (D).
class ProviderRefsTest {

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static final String WOOD_FUEL =
      "{'burn_time':'minecraft:cooking/time_wood_blocks',"
          + "'speed_multiplier':'minecraft:cooking/speed_default'}";

  @Test
  void aFuelItemReferencesItsBurnTimeAndSpeedProviders() {
    // spec: RC-R32
    assertThat(ProviderRefs.cookingFuel(Cells.stack("coal")))
        .contains(
            new FuelRef(id("minecraft:cooking/time_coal"), id("minecraft:cooking/speed_default")));
  }

  @Test
  void anItemThatBurnsNothingHasNoFuelReference() {
    // spec: RC-R32
    assertThat(ProviderRefs.cookingFuel(Cells.stack("stone"))).isEmpty();
    assertThat(ProviderRefs.cookingFuel(Cells.stack("apple"))).isEmpty();
  }

  @Test
  void theEmptyStackHasNoReferenceOfAnyKind() {
    // spec: RC-R32, RC-R34, RC-R35
    assertThat(ProviderRefs.cookingFuel(ItemStack.empty())).isEmpty();
    assertThat(ProviderRefs.compostable(ItemStack.empty())).isEmpty();
    assertThat(ProviderRefs.brewingFuel(ItemStack.empty())).isEmpty();
  }

  @Test
  void aCompostableItemReferencesItsLayersProvider() {
    // spec: RC-R34
    assertThat(ProviderRefs.compostable(Cells.stack("apple")))
        .contains(new CompostRef(id("minecraft:compostable/medium_high")));
    assertThat(ProviderRefs.compostable(Cells.stack("stone"))).isEmpty();
    assertThat(ProviderRefs.compostable(Cells.stack("coal"))).isEmpty();
  }

  @Test
  void aBrewingFuelReferencesItsSpeedAndUsesProviders() {
    // spec: RC-R35
    assertThat(ProviderRefs.brewingFuel(Cells.stack("blaze_powder")))
        .contains(
            new BrewingFuelRef(
                id("minecraft:brewing/speed_default"), id("minecraft:brewing/uses_default")));
    assertThat(ProviderRefs.brewingFuel(Cells.stack("coal"))).isEmpty();
  }

  @Test
  void theCountOfTheStackDoesNotChangeTheReference() {
    // spec: RC-R32
    assertThat(ProviderRefs.cookingFuel(Cells.stack("coal:64")))
        .isEqualTo(ProviderRefs.cookingFuel(Cells.stack("coal:1")));
    assertThat(ProviderRefs.cookingFuel(Cells.stack("coal:64"))).isPresent();
  }

  @Test
  void aComponentRemovedFromTheStackGivesNoReference() {
    // spec: RC-R32, RC-R34, RC-R35 (the effective components are read, not the prototype)
    assertThat(
            ProviderRefs.cookingFuel(
                Cells.stack("coal").withoutComponent(WellKnownComponents.COOKING_FUEL)))
        .isEmpty();
    assertThat(
            ProviderRefs.compostable(
                Cells.stack("apple").withoutComponent(WellKnownComponents.COMPOSTABLE)))
        .isEmpty();
    assertThat(
            ProviderRefs.brewingFuel(
                Cells.stack("blaze_powder").withoutComponent(WellKnownComponents.BREWING_FUEL)))
        .isEmpty();
  }

  @Test
  void aComponentSetOnTheStackMakesAnyItemAFuel() {
    // spec: RC-R32 (the effective components are read)
    ItemStack burning = TestItems.stack("stone", 1, "cooking_fuel", WOOD_FUEL);
    assertThat(ProviderRefs.cookingFuel(burning))
        .contains(
            new FuelRef(
                id("minecraft:cooking/time_wood_blocks"), id("minecraft:cooking/speed_default")));
  }

  @Test
  void aComponentSetOnTheStackWinsOverThePrototype() {
    // spec: RC-R32
    ItemStack other = TestItems.stack("coal", 1, "cooking_fuel", WOOD_FUEL);
    assertThat(ProviderRefs.cookingFuel(other))
        .map(FuelRef::burnTime)
        .contains(id("minecraft:cooking/time_wood_blocks"));
  }

  @Test
  void aCompostComponentSetOnTheStackWinsOverThePrototype() {
    // spec: RC-R34
    ItemStack low =
        TestItems.stack("apple", 1, "compostable", "{'layers':'minecraft:compostable/low'}");
    assertThat(ProviderRefs.compostable(low))
        .contains(new CompostRef(id("minecraft:compostable/low")));
    ItemStack stone =
        TestItems.stack("stone", 1, "compostable", "{'layers':'minecraft:compostable/low'}");
    assertThat(ProviderRefs.compostable(stone)).isPresent();
  }

  @Test
  void aBrewingFuelComponentSetOnTheStackWinsOverThePrototype() {
    // spec: RC-R35
    ItemStack fuel =
        TestItems.stack(
            "blaze_powder",
            1,
            "brewing_fuel",
            "{'speed_multiplier':'minecraft:brewing/speed_fast','uses':'minecraft:brewing/uses_few'}");
    assertThat(ProviderRefs.brewingFuel(fuel))
        .contains(
            new BrewingFuelRef(
                id("minecraft:brewing/speed_fast"), id("minecraft:brewing/uses_few")));
  }

  @Test
  void aComponentWithAnUnusableShapeIsTreatedAsAbsent() {
    // spec: RC-R32, RC-R34, RC-R35 (the Javadoc of ProviderRefs)
    ComponentValue text = new ComponentValue(new StringTag("not a reference"));
    assertThat(
            ProviderRefs.cookingFuel(
                Cells.stack("coal").withComponent(WellKnownComponents.COOKING_FUEL, text)))
        .isEmpty();
    assertThat(
            ProviderRefs.compostable(
                Cells.stack("apple").withComponent(WellKnownComponents.COMPOSTABLE, text)))
        .isEmpty();
    assertThat(
            ProviderRefs.brewingFuel(
                Cells.stack("blaze_powder").withComponent(WellKnownComponents.BREWING_FUEL, text)))
        .isEmpty();
  }
}
