package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R31 (furnace, brewing stand, campfire layouts), IC-R33
// (RecipesUsed), V-I7 and V-I8 (shapes of the structure templates, with the fake items).
class InventoryLayoutsMachinesTest {

  private static final String FURNACE =
      "{id:'minecraft:furnace',Items:[{count:1,Slot:1b,id:'minecraft:stick'},"
          + "{count:24,Slot:2b,id:'minecraft:stone'}],lit_time_remaining:0s,lit_total_time:0s,"
          + "cooking_time_spent:0s,cooking_total_time:200s,RecipesUsed:{'minecraft:stone':24}}";

  private static final String BREWING =
      "{id:'minecraft:brewing_stand',Items:[{count:1,Slot:0b,id:'minecraft:potion',"
          + "components:{'minecraft:potion_contents':{potion:'minecraft:strong_healing'}}}],"
          + "BrewTime:0s,Fuel:0b}";

  private static InventoryLayouts layouts() {
    return InventoryLayouts.of(ContainerItemsCodec.of(TestItems.stackCodec()));
  }

  private static List<String> keys(CompoundTag tag) {
    return List.copyOf(tag.entries().keySet());
  }

  private static List<String> texts(List<ItemStack> stacks) {
    return stacks.stream().map(Cells::text).toList();
  }

  private static SequencedMap<Identifier, Integer> used(String... idsAndCounts) {
    SequencedMap<Identifier, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < idsAndCounts.length; i += 2) {
      map.put(Identifier.parse(idsAndCounts[i]), Integer.parseInt(idsAndCounts[i + 1]));
    }
    return map;
  }

  @Test
  void readFurnaceGivesTheThreeSlotsAndTheCounters() {
    // spec: IC-R31, V-I8
    FurnaceData furnace = Findings.value(layouts().readFurnace(Nbt.compound(FURNACE)));
    assertThat(texts(furnace.slots())).containsExactly("-", "stick:1", "stone:24");
    assertThat(furnace.litTimeRemaining()).isZero();
    assertThat(furnace.litTotalTime()).isZero();
    assertThat(furnace.cookingTimeSpent()).isZero();
    assertThat(furnace.cookingTotalTime()).isEqualTo(200);
  }

  @Test
  void readFurnaceGivesRecipesUsedAsIdentifierToCount() {
    // spec: IC-R33 (recipe identifier to a count), V-I8
    FurnaceData furnace = Findings.value(layouts().readFurnace(Nbt.compound(FURNACE)));
    assertThat(furnace.recipesUsed()).isEqualTo(used("minecraft:stone", "24"));
  }

  @Test
  void readFurnaceKeepsTheStoredOrderOfRecipesUsed() {
    // spec: IC-R33 (in stored order, the Javadoc of FurnaceData)
    CompoundTag tag =
        Nbt.compound("{Items:[],RecipesUsed:{'minecraft:z':1,'minecraft:a':2,'minecraft:m':3}}");
    FurnaceData furnace = Findings.value(layouts().readFurnace(tag));
    assertThat(furnace.recipesUsed().keySet())
        .extracting(Identifier::toString)
        .containsExactly("minecraft:z", "minecraft:a", "minecraft:m");
  }

  @Test
  void provisionalAnEmptyFurnaceCompoundTakesTheDefaultsOfItsTypes() {
    // spec: IC-R31 (an absent field takes 0 or the empty list: provisional)
    FurnaceData furnace = Findings.value(layouts().readFurnace(CompoundTag.empty()));
    assertThat(texts(furnace.slots())).containsExactly("-", "-", "-");
    assertThat(furnace.litTimeRemaining()).isZero();
    assertThat(furnace.litTotalTime()).isZero();
    assertThat(furnace.cookingTimeSpent()).isZero();
    assertThat(furnace.cookingTotalTime()).isZero();
    assertThat(furnace.recipesUsed()).isEmpty();
  }

  @Test
  void writeFurnaceUsesTheKeysAndTypesOfTheTemplates() {
    // spec: IC-R31 (Items, then the four Short fields, then RecipesUsed with Int values)
    FurnaceData furnace =
        new FurnaceData(
            Cells.stacks("-", "stick:1", "stone:24"),
            10,
            1600,
            20,
            200,
            used("minecraft:stone", "24", "minecraft:glass", "3"));
    CompoundTag written = layouts().writeFurnace(furnace);
    assertThat(keys(written))
        .containsExactly(
            "Items",
            "lit_time_remaining",
            "lit_total_time",
            "cooking_time_spent",
            "cooking_total_time",
            "RecipesUsed");
    assertThat(written.get("lit_time_remaining")).contains(new ShortTag((short) 10));
    assertThat(written.get("lit_total_time")).contains(new ShortTag((short) 1600));
    assertThat(written.get("cooking_time_spent")).contains(new ShortTag((short) 20));
    assertThat(written.get("cooking_total_time")).contains(new ShortTag((short) 200));
    CompoundTag recipes = written.getCompound("RecipesUsed");
    assertThat(keys(recipes)).containsExactly("minecraft:stone", "minecraft:glass");
    assertThat(recipes.get("minecraft:stone")).contains(new IntTag(24));
    assertThat(written.getList("Items").elements()).hasSize(2);
  }

  @Test
  void writeThenReadFurnaceGivesBackTheState() {
    // spec: IC-R31, IC-R33 (round trip)
    FurnaceData furnace =
        new FurnaceData(
            Cells.stacks("potato:5", "coal:3", "baked_potato:2"),
            100,
            1600,
            7,
            200,
            used("minecraft:b", "2", "minecraft:a", "1"));
    CompoundTag written = layouts().writeFurnace(furnace);
    assertThat(Findings.value(layouts().readFurnace(written))).isEqualTo(furnace);
  }

  @Test
  void readBrewingStandGivesFiveSlotsBrewTimeAndFuel() {
    // spec: IC-R31, V-I7
    BrewingStandData stand = Findings.value(layouts().readBrewingStand(Nbt.compound(BREWING)));
    assertThat(stand.slots()).hasSize(5);
    assertThat(stand.slots().get(0))
        .isEqualTo(
            TestItems.stack(
                "potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"));
    assertThat(stand.slots().subList(1, 5)).allMatch(ItemStack::isEmpty);
    assertThat(stand.brewTime()).isZero();
    assertThat(stand.fuel()).isZero();
  }

  @Test
  void provisionalAnEmptyBrewingStandCompoundTakesTheDefaultsOfItsTypes() {
    // spec: IC-R31 (provisional defaults)
    BrewingStandData stand = Findings.value(layouts().readBrewingStand(CompoundTag.empty()));
    assertThat(stand.slots()).hasSize(5).allMatch(ItemStack::isEmpty);
    assertThat(stand.brewTime()).isZero();
    assertThat(stand.fuel()).isZero();
  }

  @Test
  void writeBrewingStandUsesAShortBrewTimeAndAByteFuel() {
    // spec: IC-R31 (BrewTime Short, Fuel Byte from 0 to 20)
    BrewingStandData stand =
        new BrewingStandData(Cells.stacks("-", "-", "-", "nether_wart:2", "blaze_powder"), 400, 20);
    CompoundTag written = layouts().writeBrewingStand(stand);
    assertThat(keys(written)).containsExactly("Items", "BrewTime", "Fuel");
    assertThat(written.get("BrewTime")).contains(new ShortTag((short) 400));
    assertThat(written.get("Fuel")).contains(new ByteTag((byte) 20));
    assertThat(written.getList("Items").elements()).hasSize(2);
  }

  @Test
  void writeThenReadBrewingStandGivesBackTheState() {
    // spec: IC-R31 (round trip)
    BrewingStandData stand =
        new BrewingStandData(
            List.of(
                Cells.stack("potion"),
                ItemStack.empty(),
                Cells.stack("splash_potion"),
                Cells.stack("gunpowder:3"),
                Cells.stack("blaze_powder:9")),
            123,
            17);
    CompoundTag written = layouts().writeBrewingStand(stand);
    assertThat(Findings.value(layouts().readBrewingStand(written))).isEqualTo(stand);
  }

  @Test
  void readCampfireGivesFourSlotsAndTheTwoIntArrays() {
    // spec: IC-R31 (CookingTimes and CookingTotalTimes are Int_Array of 4)
    CompoundTag tag =
        Nbt.compound(
            "{Items:[{count:1,Slot:0b,id:'minecraft:potato'},{count:1,Slot:2b,id:'minecraft:potato'}],"
                + "CookingTimes:[I;10,0,5,0],CookingTotalTimes:[I;600,600,600,600]}");
    CampfireData campfire = Findings.value(layouts().readCampfire(tag));
    assertThat(texts(campfire.slots())).containsExactly("potato:1", "-", "potato:1", "-");
    assertThat(campfire.cookingTimes()).containsExactly(10, 0, 5, 0);
    assertThat(campfire.cookingTotalTimes()).containsExactly(600, 600, 600, 600);
  }

  @Test
  void writeCampfireUsesIntArrays() {
    // spec: IC-R31
    CampfireData campfire =
        new CampfireData(
            Cells.stacks("potato", "-", "-", "potato:1"),
            List.of(10, 0, 0, 3),
            List.of(600, 600, 600, 600));
    CompoundTag written = layouts().writeCampfire(campfire);
    assertThat(keys(written)).containsExactly("Items", "CookingTimes", "CookingTotalTimes");
    assertThat(written.get("CookingTimes")).contains(IntArrayTag.of(10, 0, 0, 3));
    assertThat(written.get("CookingTotalTimes")).contains(IntArrayTag.of(600, 600, 600, 600));
    assertThat(written.getList("Items").elements()).hasSize(2);
  }

  @Test
  void writeThenReadCampfireGivesBackTheState() {
    // spec: IC-R31 (round trip)
    CampfireData campfire =
        new CampfireData(
            Cells.stacks("potato", "baked_potato", "-", "potato"),
            List.of(1, 2, 3, 4),
            List.of(600, 600, 600, 600));
    CompoundTag written = layouts().writeCampfire(campfire);
    assertThat(Findings.value(layouts().readCampfire(written))).isEqualTo(campfire);
  }
}
