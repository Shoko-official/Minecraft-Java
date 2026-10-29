package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.port.ActionSink;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The work behind {@link EffectEvaluator}. */
final class EffectRun {

  private EffectRun() {}

  static float value(
      EffectComponents effects, Identifier component, int level, float start, LootContext context) {
    Optional<EffectComponentValue> value = effects.get(component);
    if (value.isEmpty()) {
      return start;
    }
    LootContext effectContext = context.withEnchantment(level, true);
    return switch (value.get()) {
      case EffectComponentValue.ValueEffects list -> {
        float current = start;
        for (ConditionalValueEffect entry : list.entries()) {
          if (holds(entry.requirements(), effectContext)) {
            current = apply(entry.effect(), current, level, effectContext);
          }
        }
        yield current;
      }
      case EffectComponentValue.DirectValue direct ->
          apply(direct.effect(), start, level, effectContext);
      default -> start;
    };
  }

  private static float apply(ValueEffect effect, float current, int level, LootContext context) {
    return Draws.observe(
        context, DrawReason.NUMBER_PROVIDER, "effect", () -> effect.apply(current, level, context));
  }

  static boolean holds(List<LootCondition> requirements, LootContext context) {
    return Draws.allHold(context, DrawReason.NUMBER_PROVIDER, "effect.requirements", requirements);
  }

  static float valueOverEquipment(
      EntityView entity,
      EnchantmentCatalog catalog,
      Identifier component,
      float start,
      LootContext context) {
    float current = start;
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      ItemStack stack = entity.equipment(slot);
      if (stack.isEmpty()) {
        continue;
      }
      EnchantmentLevels levels = ItemEnchantments.of(stack);
      for (Map.Entry<Identifier, Integer> enchanted : levels.levels().entrySet()) {
        Optional<EnchantmentDefinition> definition = catalog.find(enchanted.getKey());
        if (definition.isPresent() && activeIn(definition.get(), slot)) {
          int level = enchanted.getValue();
          current =
              value(
                  definition.get().effects(),
                  component,
                  level,
                  current,
                  context.withEnchantment(level, true, stack));
        }
      }
    }
    return current;
  }

  private static boolean activeIn(EnchantmentDefinition definition, EquipmentSlot slot) {
    return definition.slots().stream().anyMatch(group -> group.contains(slot));
  }

  static void trigger(
      EnchantmentDefinition enchantment,
      Identifier component,
      int level,
      LootContext context,
      ActionSink sink) {
    Optional<EffectComponentValue> value = enchantment.effects().get(component);
    if (value.isEmpty() || !(value.get() instanceof EffectComponentValue.EntityEffects list)) {
      return;
    }
    LootContext effectContext = context.withEnchantment(level, true);
    for (ConditionalEntityEffect entry : list.entries()) {
      if (holds(entry.requirements(), effectContext)) {
        EffectIntents.emit(entry, level, effectContext, sink);
      }
    }
  }
}
