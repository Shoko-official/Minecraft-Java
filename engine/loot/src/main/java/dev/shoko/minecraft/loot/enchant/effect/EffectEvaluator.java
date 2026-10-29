package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.port.ActionSink;
import dev.shoko.minecraft.loot.port.EntityView;

/**
 * Evaluates the effect components of enchantments (EN-R13 to EN-R19, EN-R31, EN-R32). The final
 * consumption of a value (the 80 percent cap of protection, rounding of damage) is the job of the
 * combat, jalon M7.
 */
public final class EffectEvaluator {

  private EffectEvaluator() {}

  /**
   * Evaluates one component of one enchantment at a level (EN-R13, EN-R18). The entries of the list
   * are tried in order; an entry whose {@code requirements} hold (conditions evaluated in order,
   * with {@code enchantment_level} set to {@code level} and {@code enchantment_active} true in the
   * context) transforms the value produced by the previous entry. A component the enchantment does
   * not have returns {@code start}. Random decisions ({@code remove_binomial}, {@code
   * random_chance} requirements) are reported to the observer of the context.
   *
   * @param effects the {@code effects} of the enchantment
   * @param component the component, such as {@code minecraft:damage_protection}
   * @param level the level of the enchantment on the item, 1 to 255
   * @param start the value before this enchantment (EN-R19a to EN-R19e)
   * @param context the context of the effect, of the matching {@code enchanted_*} type
   * @return the value after this enchantment
   */
  public static float value(
      EffectComponents effects, Identifier component, int level, float start, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Evaluates one component over the equipment of an entity (EN-R31): for each equipment slot, the
   * enchantments of the stack whose {@code slots} contain that slot, in the insertion order of the
   * {@code enchantments} table, each applied to the value produced by the previous one. The order
   * of the slots follows {@code EquipmentSlot}, which is provisional (EN open question).
   *
   * @param entity the wearer
   * @param catalog the enchantment definitions
   * @param component the component
   * @param start the value before any enchantment
   * @param context the context of the effect
   * @return the accumulated value
   */
  public static float valueOverEquipment(
      EntityView entity,
      EnchantmentCatalog catalog,
      Identifier component,
      float start,
      LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Runs the entity and location effects of one component and sends their intents (EN-R16, EN-R26
   * to EN-R28), after evaluating the level-based values. {@code all_of} is flattened in order.
   * Nothing is sent for an entry whose requirements fail.
   *
   * @param enchantment the definition
   * @param component the component, such as {@code minecraft:post_attack}
   * @param level the level on the item
   * @param context the context of the effect
   * @param sink receives the intents in order
   */
  public static void trigger(
      EnchantmentDefinition enchantment,
      Identifier component,
      int level,
      LootContext context,
      ActionSink sink) {
    Skeleton.throwNotImplemented();
  }
}
