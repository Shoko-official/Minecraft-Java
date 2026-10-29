package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.craft.AnvilCalculator;
import dev.shoko.minecraft.loot.enchant.craft.AnvilInput;
import dev.shoko.minecraft.loot.enchant.craft.AnvilRefusal;
import dev.shoko.minecraft.loot.enchant.craft.AnvilResult;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Adversarial tests on the anvil boundaries (EN-R41 to EN-R43) with the real definitions.
@Tag("oracle")
class AnvilHuntTest {

  private static ItemStack stack(String path) {
    return ItemStack.of(RealData.items().find(Identifier.minecraft(path)).orElseThrow(), 1);
  }

  private static ItemStack penalty(ItemStack stack, int cost) {
    return stack.withComponent(
        Identifier.minecraft("repair_cost"), new ComponentValue(new IntTag(cost)));
  }

  private static ItemStack levels(ItemStack stack, boolean stored, String id, int level) {
    var levels = new EnchantmentLevels(new LinkedHashMap<>()).with(Identifier.parse(id), level);
    return stack.withComponent(
        stored ? WellKnownComponents.STORED_ENCHANTMENTS : WellKnownComponents.ENCHANTMENTS,
        levels.toValue());
  }

  private static AnvilResult run(ItemStack a, ItemStack b, String name, boolean creative) {
    LootData data = RealData.data();
    return AnvilCalculator.combine(new AnvilInput(a, b, name), creative, data);
  }

  @Test
  void provisionalARenameCostingExactly39IsAcceptedAnd40IsRefused() {
    // spec: EN-R41c, EN-R41d (refused above 39)
    var ok = run(penalty(stack("diamond_sword"), 38), ItemStack.empty(), "x", false);
    assertThat(ok).isInstanceOf(AnvilResult.Output.class);
    assertThat(((AnvilResult.Output) ok).levelCost()).isEqualTo(39);
    var no = run(penalty(stack("diamond_sword"), 39), ItemStack.empty(), "x", false);
    assertThat(no).isEqualTo(new AnvilResult.Refused(AnvilRefusal.TOO_EXPENSIVE));
    var creative = run(penalty(stack("diamond_sword"), 39), ItemStack.empty(), "x", true);
    assertThat(creative).isInstanceOf(AnvilResult.Output.class);
  }

  @Test
  void provisionalPenaltiesAtTheIntLimitDoNotOverflow() {
    // spec: EN-R41b/c
    ItemStack max = penalty(stack("diamond_sword"), Integer.MAX_VALUE);
    var refused = run(max, max, "", false);
    assertThat(refused).isInstanceOf(AnvilResult.Refused.class);
    var creative =
        run(
            levels(max, false, "minecraft:sharpness", 1),
            levels(max, false, "minecraft:sharpness", 1),
            "",
            true);
    assertThat(creative).isInstanceOf(AnvilResult.Output.class);
    var out = (AnvilResult.Output) creative;
    assertThat(out.levelCost()).isPositive();
    assertThat(out.result().component(Identifier.minecraft("repair_cost"))).isPresent();
  }

  @Test
  void provisionalANameIsCutAtFiftyCodePoints() {
    // spec: EN-R42c
    String name = "a".repeat(49) + Character.toString(0x1F600) + "bbbb";
    var out = (AnvilResult.Output) run(stack("diamond_sword"), ItemStack.empty(), name, false);
    var tag =
        (StringTag) out.result().component(Identifier.minecraft("custom_name")).orElseThrow().tag();
    assertThat(tag.value().codePointCount(0, tag.value().length())).isEqualTo(50);
  }

  @Test
  void provisionalABookOfTheMaximumLevelDoesNotGoBeyondMax() {
    // spec: EN-R43b (equal levels: +1 up to max_level)
    ItemStack sword = levels(stack("diamond_sword"), false, "minecraft:sharpness", 5);
    ItemStack book = levels(stack("enchanted_book"), true, "minecraft:sharpness", 5);
    var result = run(sword, book, "", false);
    assertThat(result).isInstanceOf(AnvilResult.Refused.class);
  }

  @Test
  void provisionalIncompatibleEnchantmentsOfTheBookAreLostAndCostOneLevel() {
    // spec: EN-R43a
    ItemStack sword = levels(stack("diamond_sword"), false, "minecraft:sharpness", 1);
    ItemStack book = levels(stack("enchanted_book"), true, "minecraft:smite", 1);
    var result = run(sword, book, "", false);
    assertThat(result).isInstanceOf(AnvilResult.Refused.class);
  }

  @Test
  void provisionalUnbreakingThreeBookAndMendingBookCostTheDocumentedLevels() {
    // spec: EN-R43d / EN-R43e (Unbreaking 2/1 -> III book = 3; Mending 4/2 -> book 2)
    var u =
        (AnvilResult.Output)
            run(
                stack("diamond_sword"),
                levels(stack("enchanted_book"), true, "minecraft:unbreaking", 3),
                "",
                false);
    assertThat(u.levelCost()).isEqualTo(3);
    var m =
        (AnvilResult.Output)
            run(
                stack("diamond_sword"),
                levels(stack("enchanted_book"), true, "minecraft:mending", 1),
                "",
                false);
    assertThat(m.levelCost()).isEqualTo(2);
  }

  private static ItemStack worn(String path, int damage) {
    return stack(path)
        .withComponent(WellKnownComponents.DAMAGE, new ComponentValue(new IntTag(damage)));
  }

  @Test
  void provisionalAMergeOfItemsBrokenPastTheMaximumStaysBetweenZeroAndTheMaximum() {
    // spec: EN-R42b (remaining = max(0, max_damage - damage), result in [0, max_damage]), real
    // definitions: the diamond sword has 1561 durability, the bonus is floor(12 percent) = 187
    int max = 1561;
    var both =
        (AnvilResult.Output)
            run(worn("diamond_sword", 2000), worn("diamond_sword", 2000), "", false);
    assertThat(both.result().damage()).isEqualTo(max - 187);
    var intact =
        (AnvilResult.Output) run(worn("diamond_sword", 5000), worn("diamond_sword", 0), "", false);
    assertThat(intact.result().damage()).isZero();
    var viaSacrifice =
        (AnvilResult.Output)
            run(worn("diamond_sword", 1000), worn("diamond_sword", Integer.MAX_VALUE), "", false);
    assertThat(viaSacrifice.result().damage()).isEqualTo(max - (561 + 187));
  }
}
