package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.reference.BrewingFuelRef;
import dev.shoko.minecraft.item.reference.CompostRef;
import dev.shoko.minecraft.item.reference.FuelRef;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.world.block.BlockState;

/**
 * Evaluates, for {@code game}, the providers that the items of {@code item} only reference (ADR
 * 0004 amendment 1, decision 2; LT-R50): fuel duration and cooking speed, compost layers, brewing
 * speed and uses. The context is reduced to the state of the block of the machine ({@code
 * LootContext.forBlock}), which is what {@code block/fast_cooking} and the composter level read.
 * The result goes back to {@code game}, which passes it to the cooking, brewing or composting logic
 * of {@code item}.
 */
public final class MachineProviders {

  private MachineProviders() {}

  /**
   * Evaluates the burn time of a fuel.
   *
   * @param data the loaded data
   * @param fuel the reference read by {@code ProviderRefs.cookingFuel}
   * @param machine the state of the furnace, smoker or blast furnace
   * @param random the source consumed by the provider
   * @return the burn time in ticks
   * @throws IllegalStateException when the provider is not loaded
   */
  public static int burnTime(LootData data, FuelRef fuel, BlockState machine, RandomSource random) {
    return intProvider(data, fuel.burnTime()).sample(LootContext.forBlock(data, machine, random));
  }

  /**
   * Evaluates the cooking speed multiplier of a fuel.
   *
   * @param data the loaded data
   * @param fuel the reference
   * @param machine the state of the machine
   * @param random the source consumed by the provider
   * @return the multiplier
   * @throws IllegalStateException when the provider is not loaded
   */
  public static float cookingSpeed(
      LootData data, FuelRef fuel, BlockState machine, RandomSource random) {
    return floatProvider(data, fuel.speedMultiplier())
        .sample(LootContext.forBlock(data, machine, random));
  }

  /**
   * Evaluates the layers an item adds to a composter. The provider draws: {@code low} adds a layer
   * with a 30 percent chance in the data, and always one on an empty composter.
   *
   * @param data the loaded data
   * @param compost the reference read by {@code ProviderRefs.compostable}
   * @param composter the state of the composter
   * @param random the source consumed by the provider
   * @return the number of layers added
   * @throws IllegalStateException when the provider is not loaded
   */
  public static int compostLayers(
      LootData data, CompostRef compost, BlockState composter, RandomSource random) {
    return intProvider(data, compost.layers())
        .sample(LootContext.forBlock(data, composter, random));
  }

  /**
   * Evaluates the brewing speed multiplier of a brewing fuel.
   *
   * @param data the loaded data
   * @param fuel the reference read by {@code ProviderRefs.brewingFuel}
   * @param stand the state of the brewing stand
   * @param random the source consumed by the provider
   * @return the multiplier
   * @throws IllegalStateException when the provider is not loaded
   */
  public static float brewingSpeed(
      LootData data, BrewingFuelRef fuel, BlockState stand, RandomSource random) {
    return floatProvider(data, fuel.speedMultiplier())
        .sample(LootContext.forBlock(data, stand, random));
  }

  /**
   * Evaluates the number of uses of a brewing fuel.
   *
   * @param data the loaded data
   * @param fuel the reference
   * @param stand the state of the brewing stand
   * @param random the source consumed by the provider
   * @return the uses of one item
   * @throws IllegalStateException when the provider is not loaded
   */
  public static int brewingUses(
      LootData data, BrewingFuelRef fuel, BlockState stand, RandomSource random) {
    return intProvider(data, fuel.uses()).sample(LootContext.forBlock(data, stand, random));
  }

  private static IntProvider intProvider(LootData data, Identifier id) {
    return data.numbers()
        .intProvider(id)
        .orElseThrow(() -> new IllegalStateException("provider not loaded: " + id));
  }

  private static FloatProvider floatProvider(LootData data, Identifier id) {
    return data.numbers()
        .floatProvider(id)
        .orElseThrow(() -> new IllegalStateException("provider not loaded: " + id));
  }
}
