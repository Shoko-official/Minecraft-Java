package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.enchant.effect.EffectEvaluator;
import java.util.Map;

/** Repair by experience, the {@code repair_with_xp} effect of {@code mending} (EN-R44b). */
public final class XpRepair {

  private static final Identifier REPAIR_WITH_XP = Identifier.minecraft("repair_with_xp");
  private static final float POINTS_PER_EXPERIENCE = 2.0f;

  private XpRepair() {}

  /**
   * Repairs an item with picked-up experience: each point repairs {@code 2 * factor} durability
   * points, where the factor is the value of the {@code minecraft:repair_with_xp} component
   * evaluated over the item (start 1, EN-R19c); the formula is provisional (probe PE4).
   *
   * @param stack an item in an active slot, possibly with {@code mending}
   * @param experience the points of experience picked up
   * @param context the context, for the definitions and the conditions
   * @return the repaired item and the experience not used; unchanged when the item has no such
   *     effect or no damage
   */
  public static RepairResult repair(ItemStack stack, int experience, LootContext context) {
    if (stack.isEmpty() || experience <= 0 || !stack.isDamageable() || stack.damage() <= 0) {
      return new RepairResult(stack, Math.max(0, experience));
    }
    float factor = 1.0f;
    boolean effect = false;
    for (Map.Entry<Identifier, Integer> enchanted :
        ItemEnchantments.of(stack).levels().entrySet()) {
      EnchantmentDefinition definition =
          context.data().enchantments().find(enchanted.getKey()).orElse(null);
      if (definition != null && definition.effects().get(REPAIR_WITH_XP).isPresent()) {
        effect = true;
        factor =
            EffectEvaluator.value(
                definition.effects(),
                REPAIR_WITH_XP,
                enchanted.getValue(),
                factor,
                context.withEnchantment(enchanted.getValue(), true, stack));
      }
    }
    int perPoint = (int) Math.floor(POINTS_PER_EXPERIENCE * factor);
    if (!effect || perPoint <= 0) {
      return new RepairResult(stack, experience);
    }
    int repaired = (int) Math.min(stack.damage(), (long) experience * perPoint);
    int spent = (repaired + perPoint - 1) / perPoint;
    return new RepairResult(
        StackEnchants.withDamage(stack, stack.damage() - repaired), experience - spent);
  }
}
