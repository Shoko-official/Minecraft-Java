package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R21, V-I2 (durability and wear), probe PI12. Level S.
class ItemStackDurabilityTest {

  private static ItemStack pickaxe(int damage) {
    return TestItems.stack("stone_pickaxe", 1, "damage", Integer.toString(damage));
  }

  @Test
  void provisionalAnItemWithMaxDamageIsDamageable() {
    // spec: IC-R21, V-I2 (stone pickaxe: max_damage 131, damage 0)
    ItemStack fresh = TestItems.stack("stone_pickaxe", 1);
    assertThat(fresh.isDamageable()).isTrue();
    assertThat(fresh.maxDamage()).isEqualTo(131);
    assertThat(fresh.damage()).isZero();
  }

  @Test
  void provisionalAnItemWithoutMaxDamageIsNotDamageable() {
    // spec: IC-R21 (the accessors answer 0 when the component is absent)
    ItemStack stone = TestItems.stack("stone", 1);
    assertThat(stone.isDamageable()).isFalse();
    assertThat(stone.maxDamage()).isZero();
    assertThat(stone.damage()).isZero();
  }

  @Test
  void provisionalDamageReadsTheModifiedComponent() {
    // spec: IC-R21
    assertThat(pickaxe(100).damage()).isEqualTo(100);
    assertThat(pickaxe(100).maxDamage()).isEqualTo(131);
  }

  @Test
  void provisionalWearRaisesTheDamage() {
    // spec: IC-R21
    ItemStack worn = pickaxe(100).wornBy(5);
    assertThat(worn.isEmpty()).isFalse();
    assertThat(worn.damage()).isEqualTo(105);
    assertThat(worn.maxDamage()).isEqualTo(131);
    assertThat(worn.count()).isEqualTo(1);
  }

  @Test
  void provisionalWearFromANewItemSetsTheDamage() {
    // spec: IC-R21
    assertThat(TestItems.stack("stone_pickaxe", 1).wornBy(1).damage()).isEqualTo(1);
  }

  @Test
  void provisionalWearKeepsTheOtherComponents() {
    // spec: IC-R21
    ItemStack epic = StackSupport.withRarity("stone_pickaxe", 1, "epic");
    ItemStack worn = epic.wornBy(3);
    assertThat(worn.component(StackSupport.rarityId())).contains(StackSupport.text("epic"));
  }

  @Test
  void provisionalTheItemBreaksWhenTheDamageReachesMaxDamage() {
    // spec: IC-R21, probe PI12
    assertThat(pickaxe(130).wornBy(1).isEmpty()).isTrue();
    assertThat(pickaxe(100).wornBy(31).isEmpty()).isTrue();
  }

  @Test
  void provisionalTheItemSurvivesOneUnitBeforeMaxDamage() {
    // spec: IC-R21, probe PI12
    ItemStack worn = pickaxe(129).wornBy(1);
    assertThat(worn.isEmpty()).isFalse();
    assertThat(worn.damage()).isEqualTo(130);
  }

  @Test
  void provisionalWearBeyondMaxDamageBreaksTheItem() {
    // spec: IC-R21
    assertThat(pickaxe(0).wornBy(1000).isEmpty()).isTrue();
  }

  @Test
  void provisionalAnUnbreakableItemDoesNotWear() {
    // spec: IC-R21 (unbreakable prevents wear); the presence value is an empty compound, IC-R6b
    ItemStack unbreakable =
        pickaxe(10).withComponent(TestItems.id("unbreakable"), StackSupport.presence());
    ItemStack worn = unbreakable.wornBy(500);
    assertThat(worn).isEqualTo(unbreakable);
    assertThat(worn.damage()).isEqualTo(10);
  }

  @Test
  void provisionalAnItemWithoutDurabilityDoesNotWear() {
    // spec: IC-R21 (Javadoc of wornBy: the same stack when the item is not damageable)
    ItemStack stone = TestItems.stack("stone", 3);
    assertThat(stone.wornBy(5)).isEqualTo(stone);
  }

  @Test
  void provisionalWearDoesNotChangeTheOriginalStack() {
    // spec: IC-R21 (immutability)
    ItemStack fresh = pickaxe(10);
    assertThat(fresh.wornBy(5).damage()).isEqualTo(15);
    assertThat(fresh.damage()).isEqualTo(10);
  }
}
