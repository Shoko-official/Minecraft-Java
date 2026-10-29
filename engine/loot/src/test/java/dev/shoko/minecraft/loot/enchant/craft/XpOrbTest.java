package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R44b (durability repaired = min(damage, xp x value), xp consumed = repaired / value, the
// rest is given back), EN-R48 (examples with mending, value 2; H-xp-arrondi-a), V-E19, probe PE16.
class XpOrbTest {

  private static ItemStack mendingPick(int damage) {
    return LootItems.damaged(
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:mending", 1)),
        damage);
  }

  private static LootContext context() {
    return Ctx.context(Lcg48Random.ofSeed(1));
  }

  @Test
  void provisionalAnOrbOfTenOnAnItemDamagedByEightRepairsEightAndGivesSixBack() {
    // spec: V-E19, EN-R48: repaired 8, consumed 4, given back 6
    RepairResult result = XpRepair.repair(mendingPick(8), 10, context());
    assertThat(result.stack().damage()).isZero();
    assertThat(result.experienceLeft()).isEqualTo(6);
  }

  @Test
  void provisionalAnOrbOfTenOnAnItemDamagedByThirtyRepairsTwentyAndKeepsNothing() {
    // spec: V-E19, EN-R48: repaired 20, consumed 10, given back 0
    RepairResult result = XpRepair.repair(mendingPick(30), 10, context());
    assertThat(result.stack().damage()).isEqualTo(10);
    assertThat(result.experienceLeft()).isZero();
  }

  @Test
  void provisionalAnOrbOfOneOnAnItemDamagedByOneIsFullyConsumed() {
    // spec: V-E19, EN-R48 (5), H-xp-arrondi-a: ceiling of repaired / value, so 1 consumed
    RepairResult result = XpRepair.repair(mendingPick(1), 1, context());
    assertThat(result.stack().damage()).isZero();
    assertThat(result.experienceLeft()).isZero();
  }

  @Test
  void provisionalAnOddDamageConsumesTheCeilingOfHalfIt() {
    // spec: EN-R48 (5), H-xp-arrondi-a: damage 7 needs 4 points of 10
    RepairResult result = XpRepair.repair(mendingPick(7), 10, context());
    assertThat(result.stack().damage()).isZero();
    assertThat(result.experienceLeft()).isEqualTo(6);
  }

  @Test
  void provisionalTheRepairedAmountNeverExceedsTheDamage() {
    // spec: EN-R44b (min(damage, ...))
    for (int damage : new int[] {1, 2, 3, 50, 249}) {
      for (int xp : new int[] {1, 2, 100, 5000}) {
        RepairResult result = XpRepair.repair(mendingPick(damage), xp, context());
        assertThat(result.stack().damage()).isBetween(0, damage);
        assertThat(result.experienceLeft()).isBetween(0, xp);
        int repaired = damage - result.stack().damage();
        assertThat(repaired).isLessThanOrEqualTo(2 * (xp - result.experienceLeft()));
      }
    }
  }
}
