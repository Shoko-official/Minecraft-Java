package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R45 (1 to 3 gunpowder, 0 to 7 stars) and RC-R49a (repair). Boundaries of
// the special recipes, on grids built for the purpose.
class FireworkAndRepairHuntTest {

  private static final String ROCKET =
      "{'type':'minecraft:crafting_special_firework_rocket','fuel':'minecraft:gunpowder',"
          + "'shell':'minecraft:paper','star':'minecraft:firework_star',"
          + "'result':{'count':3,'id':'minecraft:firework_rocket'}}";
  private static final String REPAIR = "{'type':'minecraft:crafting_special_repairitem'}";

  private static Optional<CraftingMatch> recognize(String path, String json, CraftingGrid grid) {
    RecipeSet set = HuntFixtures.recipes(path, json);
    return CraftingRecognizer.of(set, HuntFixtures.catalog()).recognize(grid);
  }

  private static CraftingGrid rocketGrid(int width, int height, int powder, int stars) {
    List<ItemStack> cells = new ArrayList<>();
    cells.add(HuntFixtures.stack("paper"));
    for (int i = 0; i < powder; i++) {
      cells.add(HuntFixtures.stack("gunpowder"));
    }
    for (int i = 0; i < stars; i++) {
      cells.add(HuntFixtures.stack("firework_star"));
    }
    return HuntFixtures.grid(width, height, cells.toArray(new ItemStack[0]));
  }

  private static boolean isRocket(Optional<CraftingMatch> match) {
    return match.map(m -> m.result().type().id().path().equals("firework_rocket")).orElse(false);
  }

  @Test
  void provisionalARocketTakesOneToThreeGunpowder() {
    // spec: RC-R45
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(3, 3, 1, 0)))).isTrue();
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(3, 3, 3, 0)))).isTrue();
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(3, 3, 4, 0)))).isFalse();
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(3, 3, 0, 0)))).isFalse();
  }

  @Test
  void provisionalARocketTakesAtMostSevenStars() {
    // spec: RC-R45 (a 4x4 grid is needed to hold paper, powder and eight stars)
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(4, 4, 1, 7)))).isTrue();
    assertThat(isRocket(recognize("rocket", ROCKET, rocketGrid(4, 4, 1, 8)))).isFalse();
  }

  @Test
  void provisionalARocketKeepsTheDurationOfItsGunpowder() {
    // spec: RC-R45 "la duree de vol vaut le nombre de poudres": the count of the result is 3
    CraftingMatch match = recognize("rocket", ROCKET, rocketGrid(3, 3, 2, 0)).orElseThrow();
    assertThat(match.result().count()).isEqualTo(3);
  }

  private static ItemStack worn(String item, int damage) {
    return HuntFixtures.stack(item)
        .withComponent(Identifier.minecraft("damage"), TestItems.value("damage", "" + damage));
  }

  @Test
  void provisionalRepairAddsTheRemainingDurabilitiesAndAFivePercentBonus() {
    // spec: RC-R49a "5 pour cent selon la memoire de la documentation publique, a confirmer"
    // stone_pickaxe has max_damage 131: remaining 31 + 31, bonus 131 * 5 / 100 = 6
    CraftingGrid grid =
        HuntFixtures.grid(2, 2, worn("stone_pickaxe", 100), worn("stone_pickaxe", 100));
    CraftingMatch match = recognize("repair", REPAIR, grid).orElseThrow();
    assertThat(match.result().damage()).isEqualTo(131 - (31 + 31 + 6));
  }

  @Test
  void provisionalRepairNeverLeavesANegativeDamage() {
    // spec: RC-R49a (the durability is capped at max_damage)
    CraftingGrid grid = HuntFixtures.grid(2, 2, worn("stone_pickaxe", 1), worn("stone_pickaxe", 1));
    CraftingMatch match = recognize("repair", REPAIR, grid).orElseThrow();
    assertThat(match.result().damage()).isBetween(0, 131);
  }

  @Test
  void provisionalRepairNeedsTwoItemsOfTheSameKind() {
    // spec: RC-R49a "deux objets a durabilite de meme type"
    CraftingGrid grid = HuntFixtures.grid(2, 2, worn("stone_pickaxe", 10), worn("diamond_axe", 10));
    assertThat(recognize("repair", REPAIR, grid)).isEmpty();
    CraftingGrid single = HuntFixtures.grid(2, 2, worn("stone_pickaxe", 10));
    assertThat(recognize("repair", REPAIR, single)).isEmpty();
  }
}
