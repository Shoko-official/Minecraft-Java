package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R15, IC-R17 to IC-R19 (normal form, content equality, merge
// condition). All of it is level S, hence the provisional prefix.
class ItemStackContentTest {

  private static ItemStack bread(String foodJson) {
    return TestItems.stack("bread", 1, "food", foodJson);
  }

  @Test
  void provisionalASetToThePrototypeValueIsDropped() {
    // spec: IC-R17
    ItemStack plain = TestItems.stack("bread", 1);
    ItemStack same = bread("{'nutrition':5,'saturation':6.0}");
    assertThat(same.patch().isEmpty()).isTrue();
    assertThat(same).isEqualTo(plain);
  }

  @Test
  void provisionalSettingThePrototypeValueBackRestoresThePlainStack() {
    // spec: IC-R17
    ItemStack plain = TestItems.stack("stone", 1);
    ItemStack back =
        plain
            .withComponent(StackSupport.rarityId(), StackSupport.text("epic"))
            .withComponent(StackSupport.rarityId(), StackSupport.text("common"));
    assertThat(back.patch().isEmpty()).isTrue();
    assertThat(back).isEqualTo(plain);
  }

  @Test
  void provisionalThePatchGivenToOfIsNormalized() {
    // spec: IC-R17 (ItemStack#of stores the patch in normal form)
    ItemStack stack =
        ItemStack.of(TestItems.type("stone"), 2, TestItems.patchOf("rarity", "'common'"));
    assertThat(stack.patch().isEmpty()).isTrue();
    assertThat(stack).isEqualTo(TestItems.stack("stone", 2));
  }

  @Test
  void provisionalRemovingAComponentThePrototypeLacksIsDropped() {
    // spec: IC-R17, IC-R18
    ItemStack plain = TestItems.stack("stone", 1);
    ItemStack removed = plain.withoutComponent(TestItems.id("unbreakable"));
    assertThat(removed.patch().isEmpty()).isTrue();
    assertThat(removed).isEqualTo(plain);
  }

  @Test
  void provisionalRemovingThenRestoringAComponentRestoresThePlainStack() {
    // spec: IC-R17
    ItemStack plain = TestItems.stack("bread", 1);
    ItemStack restored =
        plain
            .withoutComponent(TestItems.id("food"))
            .withComponent(
                TestItems.id("food"), TestItems.value("food", "{'nutrition':5,'saturation':6.0}"));
    assertThat(restored.patch().isEmpty()).isTrue();
    assertThat(restored).isEqualTo(plain);
  }

  @Test
  void provisionalEqualEffectiveComponentsGiveEqualPatchesWhateverTheOrder() {
    // spec: IC-R17 (two stacks with the same effective components have the same normal form)
    ItemStack a =
        TestItems.stack("stone_pickaxe", 1, "damage", "5")
            .withComponent(StackSupport.rarityId(), StackSupport.text("epic"));
    ItemStack b =
        StackSupport.withRarity("stone_pickaxe", 1, "epic")
            .withComponent(TestItems.id("damage"), TestItems.value("damage", "5"));
    assertThat(a.patch()).isEqualTo(b.patch());
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void provisionalSameContentIgnoresTheCount() {
    // spec: IC-R18
    ItemStack small = TestItems.stack("stone", 1);
    ItemStack big = TestItems.stack("stone", 50);
    assertThat(small.sameContent(big)).isTrue();
    assertThat(big.sameContent(small)).isTrue();
    assertThat(small).isNotEqualTo(big);
  }

  @Test
  void provisionalEqualsComparesContentAndCount() {
    // spec: IC-R18 (ItemStack#equals)
    ItemStack a = StackSupport.withRarity("stone", 3, "epic");
    ItemStack b = StackSupport.withRarity("stone", 3, "epic");
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(StackSupport.withRarity("stone", 4, "epic"));
    assertThat(a).isNotEqualTo(TestItems.stack("stone", 3));
    assertThat(a).isNotEqualTo(StackSupport.withRarity("cobblestone", 3, "epic"));
  }

  @Test
  void provisionalAStackIsNeitherNullNorAnotherType() {
    // spec: IC-R18 (ItemStack#equals)
    ItemStack stack = TestItems.stack("stone", 1);
    assertThat(stack).isNotEqualTo(null);
    assertThat(stack).isNotEqualTo("minecraft:stone");
  }

  @Test
  void provisionalAllEmptyStacksAreEquivalent() {
    // spec: IC-R15
    ItemStack fromAir = ItemStack.of(TestItems.catalog().air(), 9);
    ItemStack fromZero = TestItems.stack("stone", 4).withCount(0);
    ItemStack withPatch =
        ItemStack.of(TestItems.type("stone"), 0, TestItems.patchOf("rarity", "'epic'"));
    List<ItemStack> empties = List.of(ItemStack.empty(), fromAir, fromZero, withPatch);
    for (ItemStack a : empties) {
      for (ItemStack b : empties) {
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.sameContent(b)).isTrue();
      }
    }
  }

  @Test
  void provisionalAnEmptyStackHasNoContentInCommonWithARealOne() {
    // spec: IC-R15, IC-R18
    assertThat(ItemStack.empty().sameContent(TestItems.stack("stone", 1))).isFalse();
    assertThat(TestItems.stack("stone", 1).sameContent(ItemStack.empty())).isFalse();
  }

  @Test
  void provisionalDifferentItemsNeverShareContent() {
    // spec: IC-R18
    assertThat(TestItems.stack("stone", 1).sameContent(TestItems.stack("cobblestone", 1)))
        .isFalse();
  }

  @Test
  void provisionalAnyDifferenceOfEffectiveComponentsSeparatesContents() {
    // spec: IC-R19 (enchantments, name, lore, durability, rarity all matter)
    ItemStack base = TestItems.stack("stone_pickaxe", 1);
    ItemStack enchanted =
        TestItems.stack("stone_pickaxe", 1, "enchantments", "{'minecraft:sharpness':1}");
    ItemStack enchantedMore =
        TestItems.stack("stone_pickaxe", 1, "enchantments", "{'minecraft:sharpness':2}");
    ItemStack named =
        base.withComponent(TestItems.id("custom_name"), new ComponentValue(new StringTag("Bob")));
    ItemStack lored =
        base.withComponent(
            TestItems.id("lore"), new ComponentValue(ListTag.of(List.of(new StringTag("a")))));
    ItemStack worn = TestItems.stack("stone_pickaxe", 1, "damage", "5");
    ItemStack epic = StackSupport.withRarity("stone_pickaxe", 1, "epic");
    List<ItemStack> all = List.of(base, enchanted, enchantedMore, named, lored, worn, epic);
    for (int i = 0; i < all.size(); i++) {
      for (int j = 0; j < all.size(); j++) {
        assertThat(all.get(i).sameContent(all.get(j))).as("%d vs %d", i, j).isEqualTo(i == j);
      }
    }
  }

  @Test
  void provisionalAComponentRemovedAndOneAbsentFromThePrototypeAreEquivalent() {
    // spec: IC-R18
    ItemStack plain = TestItems.stack("stone", 1);
    ItemStack dropped = plain.withoutComponent(TestItems.id("food"));
    assertThat(dropped.sameContent(plain)).isTrue();
    assertThat(dropped).isEqualTo(plain);
  }

  @Test
  void provisionalFloatsInComponentsCompareByBits() {
    // spec: IC-R18 (floats by bits as core-nbt R37: 0.0 differs from -0.0)
    ItemStack positive = bread("{'nutrition':1,'saturation':0.0}");
    ItemStack negative = bread("{'nutrition':1,'saturation':-0.0}");
    assertThat(positive.sameContent(negative)).isFalse();
    assertThat(positive).isNotEqualTo(negative);
    assertThat(positive.sameContent(bread("{'nutrition':1,'saturation':0.0}"))).isTrue();
  }
}
