package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCosts;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.EnchantmentItems;
import dev.shoko.minecraft.loot.enchant.SlotGroup;
import dev.shoko.minecraft.loot.enchant.effect.ConditionalValueEffect;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponentValue;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.enchant.effect.ValueEffect;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/** Enchantment definitions built from records, to give a test full control of the effects. */
public final class TestEnchantments {

  private TestEnchantments() {}

  public static EnchantmentDefinition definition(String name, EffectComponents effects) {
    return new EnchantmentDefinition(
        Identifier.minecraft(name),
        Json.parse("{'translate':'enchantment.test'}"),
        2,
        new EnchantmentCosts(
            2, 4, new LevelBasedValue.Linear(10f, 20f), new LevelBasedValue.Linear(60f, 20f)),
        List.of(SlotGroup.MAINHAND),
        new EnchantmentItems(
            Sets.itemTag("enchantable/weapon"), Sets.itemTag("enchantable/weapon"), false),
        Sets.enchantments(),
        effects);
  }

  public static LevelBasedValue binaryDag(int levels) {
    LevelBasedValue value = new LevelBasedValue.Constant(1f);
    for (int level = 0; level < levels; level++) {
      value = new LevelBasedValue.Fraction(value, value);
    }
    return value;
  }

  public static EffectComponents componentEffects(Identifier component, LevelBasedValue value) {
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    map.put(
        component,
        new EffectComponentValue.ValueEffects(
            List.of(new ConditionalValueEffect(new ValueEffect.Add(value), List.of()))));
    return new EffectComponents(map);
  }

  public static LootData dataWith(EnchantmentDefinition... definitions) {
    LinkedHashMap<Identifier, EnchantmentDefinition> ordered = new LinkedHashMap<>();
    for (EnchantmentDefinition definition : definitions) {
      ordered.put(definition.id(), definition);
    }
    LootData base = Ctx.data();
    return new LootData(
        base.tables(),
        base.predicates(),
        base.numbers(),
        new EnchantmentCatalog(ordered, new LinkedHashMap<>()),
        base.items(),
        base.recipes(),
        base.registries());
  }
}
