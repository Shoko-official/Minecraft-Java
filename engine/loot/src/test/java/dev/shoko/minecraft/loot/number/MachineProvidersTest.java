package dev.shoko.minecraft.loot.number;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.reference.BrewingFuelRef;
import dev.shoko.minecraft.item.reference.CompostRef;
import dev.shoko.minecraft.item.reference.FuelRef;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.world.block.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R50 (providers evaluated by loot for item), ADR 0004 amendment 1 decision 2, LT-R61a
// (block/fast_cooking)
class MachineProvidersTest {

  private static final BlockState FURNACE = Blocks.plain("furnace", 10, 100);
  private static final BlockState SMOKER = Blocks.plain("smoker", 11, 110);
  private static final BlockState COMPOSTER = Blocks.plain("composter", 12, 120);

  private static IntProvider.Constant c(int value) {
    return new IntProvider.Constant(value);
  }

  private static LootCondition fastCooking() {
    return new LootCondition.MatchBlock(
        new BlockMatch(Sets.blocks("smoker", "blast_furnace"), new LinkedHashMap<>()));
  }

  private static LootData data() {
    Map<String, IntProvider> ints = new LinkedHashMap<>();
    ints.put("cooking/time_coal", c(1600));
    ints.put("cooking/time_fast", new IntProvider.Conditional(fastCooking(), c(800), c(1600)));
    ints.put("compostable/low", Tables.uniform(0, 1));
    ints.put("compostable/medium", c(1));
    ints.put("brewing/uses_default", c(20));
    Map<String, FloatProvider> floats = new LinkedHashMap<>();
    floats.put("cooking/speed_default", new FloatProvider.Constant(1.0f));
    floats.put(
        "cooking/speed_fast",
        new FloatProvider.Conditional(
            fastCooking(), new FloatProvider.Constant(2.0f), new FloatProvider.Constant(1.0f)));
    floats.put("brewing/speed_default", new FloatProvider.Constant(1.0f));
    return Ctx.data(Map.of(), Ctx.standardPredicates(), Ctx.providers(ints, floats));
  }

  private static Identifier id(String path) {
    return Identifier.parse("minecraft:" + path);
  }

  @Test
  void aFuelHasAConstantBurnTime() {
    // spec: LT-R50 (cooking/time_coal)
    FuelRef coal = new FuelRef(id("cooking/time_coal"), id("cooking/speed_default"));
    assertThat(MachineProviders.burnTime(data(), coal, FURNACE, ScriptedRandom.of()))
        .isEqualTo(1600);
  }

  @Test
  void theBurnTimeReadsTheStateOfTheMachine() {
    // spec: LT-R50, LT-R61a (conditional on the block of the machine)
    FuelRef fuel = new FuelRef(id("cooking/time_fast"), id("cooking/speed_default"));
    assertThat(MachineProviders.burnTime(data(), fuel, FURNACE, ScriptedRandom.of()))
        .isEqualTo(1600);
    assertThat(MachineProviders.burnTime(data(), fuel, SMOKER, ScriptedRandom.of())).isEqualTo(800);
  }

  @Test
  void theCookingSpeedReadsTheStateOfTheMachine() {
    // spec: LT-R50, LT-R61a (block/fast_cooking: smoker, blast_furnace)
    FuelRef fuel = new FuelRef(id("cooking/time_coal"), id("cooking/speed_fast"));
    assertThat(MachineProviders.cookingSpeed(data(), fuel, FURNACE, ScriptedRandom.of()))
        .isEqualTo(1.0f);
    assertThat(MachineProviders.cookingSpeed(data(), fuel, SMOKER, ScriptedRandom.of()))
        .isEqualTo(2.0f);
  }

  @Test
  void compostLayersCanBeConstant() {
    // spec: LT-R50 (compostable layers)
    CompostRef medium = new CompostRef(id("compostable/medium"));
    assertThat(MachineProviders.compostLayers(data(), medium, COMPOSTER, ScriptedRandom.of()))
        .isEqualTo(1);
  }

  @Test
  void provisionalCompostLayersDrawFromTheSourceOfTheCaller() {
    // spec: LT-R50, MachineProviders#compostLayers contract (the provider draws)
    CompostRef low = new CompostRef(id("compostable/low"));
    ScriptedRandom high = ScriptedRandom.of(0.999);
    ScriptedRandom none = ScriptedRandom.of(0.0);
    assertThat(MachineProviders.compostLayers(data(), low, COMPOSTER, high)).isEqualTo(1);
    assertThat(MachineProviders.compostLayers(data(), low, COMPOSTER, none)).isZero();
    assertThat(high.steps()).isEqualTo(1);
  }

  @Test
  void brewingSpeedAndUsesAreEvaluated() {
    // spec: LT-R50 (brewing/speed_default, brewing/uses_default)
    BrewingFuelRef blaze =
        new BrewingFuelRef(id("brewing/speed_default"), id("brewing/uses_default"));
    BlockState stand = Blocks.plain("brewing_stand", 13, 130);
    assertThat(MachineProviders.brewingSpeed(data(), blaze, stand, ScriptedRandom.of()))
        .isEqualTo(1.0f);
    assertThat(MachineProviders.brewingUses(data(), blaze, stand, ScriptedRandom.of()))
        .isEqualTo(20);
  }

  @Test
  void aProviderThatIsNotLoadedIsAnIllegalState() {
    // spec: MachineProviders contract (IllegalStateException)
    LootData data = data();
    FuelRef missing = new FuelRef(id("cooking/time_none"), id("cooking/speed_default"));
    assertThatThrownBy(() -> MachineProviders.burnTime(data, missing, FURNACE, ScriptedRandom.of()))
        .isInstanceOf(IllegalStateException.class);
    FuelRef noSpeed = new FuelRef(id("cooking/time_coal"), id("cooking/speed_none"));
    assertThatThrownBy(
            () -> MachineProviders.cookingSpeed(data, noSpeed, FURNACE, ScriptedRandom.of()))
        .isInstanceOf(IllegalStateException.class);
  }
}
