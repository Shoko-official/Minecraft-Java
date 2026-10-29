package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R31 (NBT layout of chests, barrels, dispensers), IC-R32
// (LootTable String, LootTableSeed Long, absence means no table).
class InventoryLayoutsTest {

  private static final Identifier TABLE = Identifier.parse("minecraft:chests/simple_dungeon");

  private static InventoryLayouts layouts() {
    return InventoryLayouts.of(ContainerItemsCodec.of(TestItems.stackCodec()));
  }

  private static List<String> keys(CompoundTag tag) {
    return List.copyOf(tag.entries().keySet());
  }

  private static List<String> texts(ContainerContents contents) {
    return contents.slots().stream().map(Cells::text).toList();
  }

  @Test
  void readContentsGivesTheItemsOfTheBlockEntity() {
    // spec: IC-R31
    CompoundTag chest =
        Nbt.compound(
            "{id:'minecraft:chest',Items:[{count:3,Slot:4b,id:'minecraft:stone'},"
                + "{count:1,Slot:0b,id:'minecraft:stick'}]}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 6));
    assertThat(texts(contents)).containsExactly("stick:1", "-", "-", "-", "stone:3", "-");
  }

  @Test
  void readContentsWithoutLootTableBindsNothing() {
    // spec: IC-R32 (absence means a materialized container or one without loot)
    CompoundTag chest = Nbt.compound("{Items:[]}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 3));
    assertThat(contents.loot()).isInstanceOf(LootBinding.None.class);
    assertThat(texts(contents)).containsExactly("-", "-", "-");
  }

  @Test
  void readContentsBindsTheLootTableAndItsSeed() {
    // spec: IC-R32 (LootTable String, LootTableSeed Long)
    CompoundTag chest =
        Nbt.compound("{LootTable:'minecraft:chests/simple_dungeon',LootTableSeed:42L}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 27));
    assertThat(contents.loot()).isEqualTo(new LootBinding.Bound(TABLE, true, 42L));
  }

  @Test
  void readContentsBindsALootTableWithoutSeed() {
    // spec: IC-R32 (LootTableSeed is absent from every template)
    CompoundTag chest = Nbt.compound("{LootTable:'minecraft:chests/simple_dungeon'}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 27));
    assertThat(contents.loot())
        .isInstanceOfSatisfying(
            LootBinding.Bound.class,
            bound -> {
              assertThat(bound.table()).isEqualTo(TABLE);
              assertThat(bound.seeded()).isFalse();
            });
  }

  @Test
  void provisionalAChestWithoutItemsReadsAsEmptySlots() {
    // spec: IC-R31, IC-R32 (350 of 473 template chests have a LootTable and no Items; the default
    // of an absent field is provisional)
    CompoundTag chest =
        Nbt.compound("{id:'minecraft:chest',LootTable:'minecraft:chests/simple_dungeon'}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 27));
    assertThat(contents.slots()).hasSize(27).allMatch(ItemStack::isEmpty);
    assertThat(contents.loot()).isInstanceOf(LootBinding.Bound.class);
  }

  @Test
  void keysTheLayoutDoesNotKnowAreNotAnError() {
    // spec: IC-R31 (components, x, y, z and id are found next to Items in the templates)
    CompoundTag chest =
        Nbt.compound(
            "{id:'minecraft:chest',components:{},x:1,y:2,z:3,"
                + "Items:[{count:1,Slot:0b,id:'minecraft:stone'}],Unknown:[1,2]}");
    ContainerContents contents = Findings.value(layouts().readContents(chest, 2));
    assertThat(texts(contents)).containsExactly("stone:1", "-");
  }

  @Test
  void writeContentsWithoutLootWritesOnlyItems() {
    // spec: IC-R31, IC-R25 (Items is written even when empty)
    ContainerContents contents =
        new ContainerContents(Cells.stacks("-", "stone:2"), new LootBinding.None());
    CompoundTag written = layouts().writeContents(contents);
    assertThat(keys(written)).containsExactly("Items");
    assertThat(written.getList("Items").elements()).hasSize(1);
    CompoundTag empty =
        layouts().writeContents(new ContainerContents(Cells.stacks("-"), new LootBinding.None()));
    assertThat(keys(empty)).containsExactly("Items");
    assertThat(empty.getList("Items").elements()).isEmpty();
  }

  @Test
  void writeContentsWritesALootTableThenItsSeedWhenSeeded() {
    // spec: IC-R32 (String then Long)
    ContainerContents contents =
        new ContainerContents(Cells.stacks("-"), new LootBinding.Bound(TABLE, true, 42L));
    CompoundTag written = layouts().writeContents(contents);
    assertThat(keys(written)).containsExactly("Items", "LootTable", "LootTableSeed");
    assertThat(written.get("LootTable")).contains(new StringTag("minecraft:chests/simple_dungeon"));
    assertThat(written.get("LootTableSeed")).contains(new LongTag(42L));
  }

  @Test
  void writeContentsOmitsTheSeedWhenNotSeeded() {
    // spec: IC-R32
    ContainerContents contents =
        new ContainerContents(Cells.stacks("-"), new LootBinding.Bound(TABLE, false, 0L));
    CompoundTag written = layouts().writeContents(contents);
    assertThat(keys(written)).containsExactly("Items", "LootTable");
  }

  @Test
  void writeThenReadContentsGivesBackTheContents() {
    // spec: IC-R31, IC-R32 (round trip for the three loot states)
    List<ItemStack> slots = Cells.stacks("stone:64", "-", "dirt:3");
    List<LootBinding> bindings =
        List.of(
            new LootBinding.None(),
            new LootBinding.Bound(TABLE, false, 0L),
            new LootBinding.Bound(TABLE, true, -7L));
    for (LootBinding binding : bindings) {
      ContainerContents contents = new ContainerContents(slots, binding);
      CompoundTag written = layouts().writeContents(contents);
      assertThat(Findings.value(layouts().readContents(written, 3))).isEqualTo(contents);
    }
  }
}
