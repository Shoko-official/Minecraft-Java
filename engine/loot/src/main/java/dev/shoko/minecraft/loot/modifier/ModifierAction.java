package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import java.util.List;
import java.util.SequencedMap;

/**
 * What a modifier does. One record per type with a behavior (LT-R39 to LT-R46) and {@link Deferred}
 * for the 21 types without data (LT-R47). Providers in a field are evaluated with the context at
 * application time, in field order.
 */
public sealed interface ModifierAction
    permits ModifierAction.ApplyBonus,
        ModifierAction.CopyComponents,
        ModifierAction.CopyState,
        ModifierAction.Discard,
        ModifierAction.EnchantRandomly,
        ModifierAction.EnchantWithLevels,
        ModifierAction.EnchantedCountIncrease,
        ModifierAction.ExplorationMap,
        ModifierAction.ExplosionDecay,
        ModifierAction.Filtered,
        ModifierAction.FurnaceSmelt,
        ModifierAction.LimitCount,
        ModifierAction.SetComponents,
        ModifierAction.SetCount,
        ModifierAction.SetDamage,
        ModifierAction.SetEnchantments,
        ModifierAction.SetInstrument,
        ModifierAction.SetName,
        ModifierAction.SetOminousBottleAmplifier,
        ModifierAction.SetPotion,
        ModifierAction.SetStewEffect,
        ModifierAction.Deferred {

  /** Which name a {@code set_name} modifier sets (LT-R44e). */
  enum NameTarget {
    /** {@code item_name}. */
    ITEM_NAME,
    /** {@code custom_name}. */
    CUSTOM_NAME
  }

  /** Where {@code copy_components} reads (LT-R44j). Only the source seen in the data. */
  enum ComponentSource {
    /** {@code block_entity}. */
    BLOCK_ENTITY
  }

  /**
   * {@code apply_bonus} (LT-R42): the level is read on {@code tool}.
   *
   * @param enchantment the enchantment, {@code minecraft:fortune} in the data
   * @param formula the formula
   */
  record ApplyBonus(Identifier enchantment, BonusFormula formula) implements ModifierAction {}

  /**
   * {@code copy_components} (LT-R44j).
   *
   * @param source where the components come from
   * @param include the components to copy
   * @param exclude the components not to copy, empty when the key is absent
   */
  record CopyComponents(ComponentSource source, List<Identifier> include, List<Identifier> exclude)
      implements ModifierAction {}

  /**
   * {@code copy_state} (LT-R44i): copies the listed properties of the {@code block_state} into the
   * {@code block_state} component.
   *
   * @param block the block the state must belong to
   * @param properties the property names to copy
   */
  record CopyState(Identifier block, List<String> properties) implements ModifierAction {}

  /** {@code discard}: removes the stack (LT-R46). */
  record Discard() implements ModifierAction {}

  /**
   * {@code enchant_randomly} (LT-R45, EN-R35).
   *
   * @param options the candidate enchantments
   * @param onlyCompatible whether incompatible candidates are excluded, true by default
   * @param includeAdditionalCostComponent false by default; its meaning is open
   */
  record EnchantRandomly(
      IdOrTagSet options, boolean onlyCompatible, boolean includeAdditionalCostComponent)
      implements ModifierAction {}

  /**
   * {@code enchant_with_levels} (LT-R45, EN-R36).
   *
   * @param levels the enchantment cost, drawn first
   * @param options the candidate enchantments, unrestricted when absent
   * @param includeAdditionalCostComponent false by default; its meaning is open
   */
  record EnchantWithLevels(
      IntProvider levels, IdOrTagSet options, boolean includeAdditionalCostComponent)
      implements ModifierAction {}

  /**
   * {@code enchanted_count_increase} (LT-R43): adds {@code count} per level of the enchantment on
   * the weapon of the {@code attacker}.
   *
   * @param count the amount per level
   * @param enchantment the enchantment, {@code minecraft:looting} in the data
   * @param limit the largest total count, 0 for no limit
   */
  record EnchantedCountIncrease(FloatProvider count, Identifier enchantment, int limit)
      implements ModifierAction {}

  /**
   * {@code exploration_map} (LT-R46): needs a structure locator that this module does not have
   * (open point of the skeleton).
   *
   * @param destination the structure tag
   * @param decoration the map marker type
   * @param zoom the zoom, 2 by default
   * @param searchRadius the search radius, 50 by default
   * @param skipExistingChunks true by default
   */
  record ExplorationMap(
      Identifier destination,
      Identifier decoration,
      int zoom,
      int searchRadius,
      boolean skipExistingChunks)
      implements ModifierAction {}

  /** {@code explosion_decay}: each unit is lost with probability {@code 1 / radius} (LT-R41). */
  record ExplosionDecay() implements ModifierAction {}

  /**
   * {@code filtered} (LT-R46).
   *
   * @param itemFilter the item predicate
   * @param onPass the modifiers applied when the stack matches, empty to leave it
   * @param onFail the modifiers applied otherwise, empty to leave it
   */
  record Filtered(ItemMatch itemFilter, List<ItemModifier> onPass, List<ItemModifier> onFail)
      implements ModifierAction {}

  /** {@code furnace_smelt}: replaces the item by its smelting result, same count (LT-R44k). */
  record FurnaceSmelt() implements ModifierAction {}

  /**
   * {@code limit_count} (LT-R40). An absent bound is the constant {@link Integer#MIN_VALUE} or
   * {@link Integer#MAX_VALUE}, which clamps nothing and draws nothing.
   *
   * @param min the lower bound
   * @param max the upper bound
   */
  record LimitCount(IntProvider min, IntProvider max) implements ModifierAction {}

  /**
   * {@code set_components} (LT-R44h).
   *
   * @param patch the components to set and, with a {@code !} key, to remove
   */
  record SetComponents(ComponentPatch patch) implements ModifierAction {}

  /**
   * {@code set_count} (LT-R39).
   *
   * @param count the count
   * @param add whether it is added to the existing count, false by default
   */
  record SetCount(IntProvider count, boolean add) implements ModifierAction {}

  /**
   * {@code set_damage} (LT-R44f).
   *
   * @param damage the fraction of durability left, 1.0 for a new item
   * @param add whether it is added to the existing fraction, false by default
   */
  record SetDamage(FloatProvider damage, boolean add) implements ModifierAction {}

  /**
   * {@code set_enchantments} (LT-R44g).
   *
   * @param enchantments the level of each enchantment, bounded to 255
   * @param add whether the levels are added to the existing ones, false by default
   */
  record SetEnchantments(SequencedMap<Identifier, IntProvider> enchantments, boolean add)
      implements ModifierAction {}

  /**
   * {@code set_instrument} (LT-R44d): one instrument drawn uniformly.
   *
   * @param options the candidate instruments
   */
  record SetInstrument(IdOrTagSet options) implements ModifierAction {}

  /**
   * {@code set_name} (LT-R44e).
   *
   * @param name the text component as read
   * @param target the name to set
   */
  record SetName(JsonNode name, NameTarget target) implements ModifierAction {}

  /**
   * {@code set_ominous_bottle_amplifier} (LT-R44c).
   *
   * @param amplifier the amplifier
   */
  record SetOminousBottleAmplifier(IntProvider amplifier) implements ModifierAction {}

  /**
   * {@code set_potion} (LT-R44a).
   *
   * @param potion the potion; {@code minecraft:empty} removes it
   */
  record SetPotion(Identifier potion) implements ModifierAction {}

  /**
   * {@code set_stew_effect} (LT-R44b).
   *
   * @param effects the effects in file order
   */
  record SetStewEffect(List<StewEffect> effects) implements ModifierAction {

    /**
     * One effect of a suspicious stew.
     *
     * @param effect the status effect
     * @param duration the duration provider
     */
    public record StewEffect(Identifier effect, IntProvider duration) {}
  }

  /**
   * One of the 21 types without data (LT-R47): decoded and kept, applied as the identity, with a
   * {@code NOT_IMPLEMENTED_MODIFIER} warning at load time.
   *
   * @param type the type identifier
   * @param fields the object as read, {@code condition} excluded
   */
  record Deferred(Identifier type, JsonObject fields) implements ModifierAction {}
}
