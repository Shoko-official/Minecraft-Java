package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentCatalog;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.loot.modifier.BonusFormula;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.IntProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/** Encodes item modifiers to a JSON tree (inverse of the modifier decoder). */
final class ModifierJson {

  private static final ComponentCodec COMPONENTS =
      ComponentCodec.of(ComponentCatalog.of(List.of()));

  private ModifierJson() {}

  /** The value of a modifier key: one object for one modifier, a list for several. */
  static Object modifiers(List<ItemModifier> modifiers) {
    if (modifiers.size() == 1) {
      return modifier(modifiers.getFirst());
    }
    List<Object> list = new ArrayList<>();
    modifiers.forEach(m -> list.add(modifier(m)));
    return list;
  }

  static Object modifier(ItemModifier modifier) {
    SequencedMap<String, Object> object = JsonOut.object();
    object.put("type", "minecraft:" + typeName(modifier.action()));
    if (!modifier.conditions().isEmpty()) {
      object.put("condition", ConditionJson.conditions(modifier.conditions()));
    }
    fields(modifier.action(), object);
    return object;
  }

  private static String typeName(ModifierAction action) {
    return switch (action) {
      case ModifierAction.ApplyBonus a -> "apply_bonus";
      case ModifierAction.CopyComponents a -> "copy_components";
      case ModifierAction.CopyState a -> "copy_state";
      case ModifierAction.Discard a -> "discard";
      case ModifierAction.EnchantRandomly a -> "enchant_randomly";
      case ModifierAction.EnchantWithLevels a -> "enchant_with_levels";
      case ModifierAction.EnchantedCountIncrease a -> "enchanted_count_increase";
      case ModifierAction.ExplorationMap a -> "exploration_map";
      case ModifierAction.ExplosionDecay a -> "explosion_decay";
      case ModifierAction.Filtered a -> "filtered";
      case ModifierAction.FurnaceSmelt a -> "furnace_smelt";
      case ModifierAction.LimitCount a -> "limit_count";
      case ModifierAction.SetComponents a -> "set_components";
      case ModifierAction.SetCount a -> "set_count";
      case ModifierAction.SetDamage a -> "set_damage";
      case ModifierAction.SetEnchantments a -> "set_enchantments";
      case ModifierAction.SetInstrument a -> "set_instrument";
      case ModifierAction.SetName a -> "set_name";
      case ModifierAction.SetOminousBottleAmplifier a -> "set_ominous_bottle_amplifier";
      case ModifierAction.SetPotion a -> "set_potion";
      case ModifierAction.SetStewEffect a -> "set_stew_effect";
      case ModifierAction.Deferred a -> a.type().path();
    };
  }

  private static void fields(ModifierAction action, SequencedMap<String, Object> out) {
    switch (action) {
      case ModifierAction.ApplyBonus bonus -> {
        out.put("enchantment", bonus.enchantment().toString());
        formula(bonus.formula(), out);
      }
      case ModifierAction.CopyComponents copy -> {
        out.put("source", "block_entity");
        out.put("include", ids(copy.include()));
        if (!copy.exclude().isEmpty()) {
          out.put("exclude", ids(copy.exclude()));
        }
      }
      case ModifierAction.CopyState copy -> {
        out.put("block", copy.block().toString());
        out.put("properties", List.copyOf(copy.properties()));
      }
      case ModifierAction.EnchantRandomly random -> {
        if (!random.options().unrestricted()) {
          out.put("options", MatchJson.set(random.options()));
        }
        if (!random.onlyCompatible()) {
          out.put("only_compatible", false);
        }
        if (random.includeAdditionalCostComponent()) {
          out.put("include_additional_cost_component", true);
        }
      }
      case ModifierAction.EnchantWithLevels levels -> {
        out.put("levels", ProviderJson.intProvider(levels.levels()));
        if (!levels.options().unrestricted()) {
          out.put("options", MatchJson.set(levels.options()));
        }
        if (levels.includeAdditionalCostComponent()) {
          out.put("include_additional_cost_component", true);
        }
      }
      case ModifierAction.EnchantedCountIncrease increase -> {
        out.put("count", ProviderJson.floatProvider(increase.count()));
        out.put("enchantment", increase.enchantment().toString());
        if (increase.limit() > 0) {
          out.put("limit", JsonOut.number(increase.limit()));
        }
      }
      case ModifierAction.ExplorationMap map -> exploration(map, out);
      case ModifierAction.Filtered filtered -> {
        out.put("item_filter", MatchJson.item(filtered.itemFilter()));
        if (!filtered.onPass().isEmpty()) {
          out.put("on_pass", modifiers(filtered.onPass()));
        }
        if (!filtered.onFail().isEmpty()) {
          out.put("on_fail", modifiers(filtered.onFail()));
        }
      }
      case ModifierAction.Discard discard -> {}
      default -> moreFields(action, out);
    }
  }

  private static void moreFields(ModifierAction action, SequencedMap<String, Object> out) {
    switch (action) {
      case ModifierAction.LimitCount limit -> out.put("limit", limit(limit));
      case ModifierAction.SetComponents set -> out.put("components", components(set.patch()));
      case ModifierAction.SetCount count -> {
        out.put("count", ProviderJson.intProvider(count.count()));
        if (count.add()) {
          out.put("add", true);
        }
      }
      case ModifierAction.SetDamage damage -> {
        out.put("damage", ProviderJson.floatProvider(damage.damage()));
        if (damage.add()) {
          out.put("add", true);
        }
      }
      case ModifierAction.SetEnchantments enchantments -> setEnchantments(enchantments, out);
      case ModifierAction.SetInstrument instrument ->
          out.put("options", MatchJson.set(instrument.options()));
      case ModifierAction.SetName name -> {
        out.put("name", JsonOut.tree(name.name()));
        out.put(
            "target",
            name.target() == ModifierAction.NameTarget.ITEM_NAME ? "item_name" : "custom_name");
      }
      case ModifierAction.SetOminousBottleAmplifier amplifier ->
          out.put("amplifier", ProviderJson.intProvider(amplifier.amplifier()));
      case ModifierAction.SetPotion potion -> out.put("id", potion.potion().toString());
      case ModifierAction.SetStewEffect stew -> out.put("effects", stew(stew));
      case ModifierAction.Deferred deferred ->
          deferred.fields().members().forEach((key, value) -> out.put(key, JsonOut.tree(value)));
      default -> {}
    }
  }

  private static List<Object> ids(List<Identifier> ids) {
    return ids.stream().map(id -> (Object) id.toString()).toList();
  }

  private static void formula(BonusFormula formula, SequencedMap<String, Object> out) {
    switch (formula) {
      case BonusFormula.OreDrops ore -> out.put("formula", "minecraft:ore_drops");
      case BonusFormula.UniformBonusCount uniform -> {
        out.put("formula", "minecraft:uniform_bonus_count");
        SequencedMap<String, Object> parameters = JsonOut.object();
        parameters.put("bonusMultiplier", JsonOut.number(uniform.bonusMultiplier()));
        out.put("parameters", parameters);
      }
      case BonusFormula.BinomialWithBonusCount binomial -> {
        out.put("formula", "minecraft:binomial_with_bonus_count");
        SequencedMap<String, Object> parameters = JsonOut.object();
        parameters.put("extra", JsonOut.number(binomial.extra()));
        parameters.put("probability", JsonOut.number(binomial.probability()));
        out.put("parameters", parameters);
      }
    }
  }

  private static void exploration(
      ModifierAction.ExplorationMap map, SequencedMap<String, Object> out) {
    out.put("destination", "#" + map.destination());
    if (!map.decoration().equals(Identifier.minecraft("mansion"))) {
      out.put("decoration", map.decoration().toString());
    }
    if (map.zoom() != 2) {
      out.put("zoom", JsonOut.number(map.zoom()));
    }
    if (map.searchRadius() != 50) {
      out.put("search_radius", JsonOut.number(map.searchRadius()));
    }
    if (!map.skipExistingChunks()) {
      out.put("skip_existing_chunks", false);
    }
  }

  private static Object limit(ModifierAction.LimitCount limit) {
    SequencedMap<String, Object> bounds = JsonOut.object();
    if (!isBound(limit.min(), Integer.MIN_VALUE)) {
      bounds.put("min", ProviderJson.intProvider(limit.min()));
    }
    if (!isBound(limit.max(), Integer.MAX_VALUE)) {
      bounds.put("max", ProviderJson.intProvider(limit.max()));
    }
    return bounds;
  }

  private static boolean isBound(IntProvider provider, int open) {
    return provider instanceof IntProvider.Constant constant && constant.value() == open;
  }

  private static Object components(ComponentPatch patch) {
    SequencedMap<String, Object> object = JsonOut.object();
    for (Map.Entry<Identifier, ComponentChange> change : patch.changes().entrySet()) {
      switch (change.getValue()) {
        case ComponentChange.Set set ->
            object.put(
                change.getKey().toString(),
                new JsonOut.Literal(COMPONENTS.valueToJson(change.getKey(), set.value())));
        case ComponentChange.Removed removed -> object.put("!" + change.getKey(), JsonOut.object());
      }
    }
    return object;
  }

  private static void setEnchantments(
      ModifierAction.SetEnchantments action, SequencedMap<String, Object> out) {
    SequencedMap<String, Object> levels = JsonOut.object();
    action
        .enchantments()
        .forEach((id, level) -> levels.put(id.toString(), ProviderJson.intProvider(level)));
    out.put("enchantments", levels);
    if (action.add()) {
      out.put("add", true);
    }
  }

  private static Object stew(ModifierAction.SetStewEffect stew) {
    List<Object> effects = new ArrayList<>();
    for (ModifierAction.SetStewEffect.StewEffect effect : stew.effects()) {
      SequencedMap<String, Object> entry = JsonOut.object();
      entry.put("type", effect.effect().toString());
      entry.put("duration", ProviderJson.intProvider(effect.duration()));
      effects.add(entry);
    }
    return effects;
  }
}
