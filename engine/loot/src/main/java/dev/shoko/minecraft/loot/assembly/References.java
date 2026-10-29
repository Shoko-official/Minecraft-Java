package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the references to named elements (predicates, number providers, tables) that a decoded
 * element makes, for the checks of LT-R61b, LT-R76 and LT-R77. A reference is followed through
 * every composite: conditions, providers, modifiers, pools, entries and inline tables.
 */
public final class References {

  /** The kind of element a reference points at. */
  public enum Kind {
    /** A named predicate. */
    PREDICATE,
    /** A named integer provider. */
    INT_PROVIDER,
    /** A named float provider. */
    FLOAT_PROVIDER,
    /** A loot table. */
    TABLE
  }

  /**
   * One reference.
   *
   * @param kind what it points at
   * @param id the identifier it names
   */
  public record Ref(Kind kind, Identifier id) {}

  private References() {}

  /**
   * Lists the references of a condition.
   *
   * @param condition a condition
   * @return the references in the order of the tree
   */
  public static List<Ref> of(LootCondition condition) {
    List<Ref> out = new ArrayList<>();
    scan(condition, out);
    return out;
  }

  /**
   * Lists the references of an integer provider.
   *
   * @param provider a provider
   * @return the references in the order of the tree
   */
  public static List<Ref> of(IntProvider provider) {
    List<Ref> out = new ArrayList<>();
    scan(provider, out);
    return out;
  }

  /**
   * Lists the references of a float provider.
   *
   * @param provider a provider
   * @return the references in the order of the tree
   */
  public static List<Ref> of(FloatProvider provider) {
    List<Ref> out = new ArrayList<>();
    scan(provider, out);
    return out;
  }

  /**
   * Lists the references of a table, inline tables included.
   *
   * @param table a table
   * @return the references in the order of the table
   */
  public static List<Ref> of(LootTable table) {
    List<Ref> out = new ArrayList<>();
    scan(table, out);
    return out;
  }

  private static void scan(LootCondition condition, List<Ref> out) {
    switch (condition) {
      case LootCondition.Reference reference ->
          out.add(new Ref(Kind.PREDICATE, reference.predicate()));
      case LootCondition.AllOf all -> all.terms().forEach(term -> scan(term, out));
      case LootCondition.AnyOf any -> any.terms().forEach(term -> scan(term, out));
      case LootCondition.Inverted inverted -> scan(inverted.term(), out);
      case LootCondition.RandomChance chance -> scan(chance.chance(), out);
      case LootCondition.IntValueCheck check -> scan(check.value(), out);
      case LootCondition.FloatValueCheck check -> scan(check.value(), out);
      default -> {}
    }
  }

  private static void scan(IntProvider provider, List<Ref> out) {
    switch (provider) {
      case IntProvider.Reference reference -> out.add(new Ref(Kind.INT_PROVIDER, reference.id()));
      case IntProvider.Uniform uniform -> {
        scan(uniform.min(), out);
        scan(uniform.max(), out);
      }
      case IntProvider.Binomial binomial -> {
        scan(binomial.n(), out);
        scan(binomial.p(), out);
      }
      case IntProvider.WeightedList list -> list.distribution().forEach(e -> scan(e.data(), out));
      case IntProvider.Conditional conditional -> {
        scan(conditional.condition(), out);
        scan(conditional.onTrue(), out);
        scan(conditional.onFalse(), out);
      }
      case IntProvider.NumberDispatcher dispatcher -> {
        for (IntProvider.NumberDispatcher.Case branch : dispatcher.cases()) {
          scan(branch.condition(), out);
          scan(branch.value(), out);
        }
        scan(dispatcher.fallback(), out);
      }
      case IntProvider.FromFloat fromFloat -> scan(fromFloat.source(), out);
      case IntProvider.Arithmetic arithmetic -> arithmetic.operands().forEach(o -> scan(o, out));
      default -> {}
    }
  }

  private static void scan(FloatProvider provider, List<Ref> out) {
    switch (provider) {
      case FloatProvider.Reference reference ->
          out.add(new Ref(Kind.FLOAT_PROVIDER, reference.id()));
      case FloatProvider.Uniform uniform -> {
        scan(uniform.min(), out);
        scan(uniform.max(), out);
      }
      case FloatProvider.WeightedList list -> list.distribution().forEach(e -> scan(e.data(), out));
      case FloatProvider.Conditional conditional -> {
        scan(conditional.condition(), out);
        scan(conditional.onTrue(), out);
        scan(conditional.onFalse(), out);
      }
      case FloatProvider.NumberDispatcher dispatcher -> {
        for (FloatProvider.NumberDispatcher.Case branch : dispatcher.cases()) {
          scan(branch.condition(), out);
          scan(branch.value(), out);
        }
        scan(dispatcher.fallback(), out);
      }
      case FloatProvider.FromInt fromInt -> scan(fromInt.source(), out);
      case FloatProvider.Arithmetic arithmetic -> arithmetic.operands().forEach(o -> scan(o, out));
      default -> {}
    }
  }

  private static void scan(LootTable table, List<Ref> out) {
    for (LootPool pool : table.pools()) {
      pool.conditions().forEach(c -> scan(c, out));
      scan(pool.rolls(), out);
      scan(pool.bonusRolls(), out);
      pool.entries().forEach(entry -> scan(entry, out));
      pool.modifiers().forEach(m -> scan(m, out));
    }
    table.modifiers().forEach(m -> scan(m, out));
  }

  private static void scan(LootEntry entry, List<Ref> out) {
    entry.conditions().forEach(c -> scan(c, out));
    entry.modifiers().forEach(m -> scan(m, out));
    switch (entry.kind()) {
      case LootEntry.Kind.LootTableRef ref -> {
        switch (ref.table()) {
          case LootEntry.TableRef.Named named -> out.add(new Ref(Kind.TABLE, named.id()));
          case LootEntry.TableRef.Inline inline -> scan(inline.table(), out);
        }
      }
      case LootEntry.Kind.Alternatives group -> group.children().forEach(c -> scan(c, out));
      case LootEntry.Kind.Sequence group -> group.children().forEach(c -> scan(c, out));
      case LootEntry.Kind.Group group -> group.children().forEach(c -> scan(c, out));
      default -> {}
    }
  }

  private static void scan(ItemModifier modifier, List<Ref> out) {
    modifier.conditions().forEach(c -> scan(c, out));
    switch (modifier.action()) {
      case ModifierAction.SetCount set -> scan(set.count(), out);
      case ModifierAction.LimitCount limit -> {
        scan(limit.min(), out);
        scan(limit.max(), out);
      }
      case ModifierAction.EnchantedCountIncrease increase -> scan(increase.count(), out);
      case ModifierAction.SetDamage damage -> scan(damage.damage(), out);
      case ModifierAction.SetEnchantments set ->
          set.enchantments().values().forEach(p -> scan(p, out));
      case ModifierAction.SetOminousBottleAmplifier amplifier -> scan(amplifier.amplifier(), out);
      case ModifierAction.SetStewEffect stew ->
          stew.effects().forEach(e -> scan(e.duration(), out));
      case ModifierAction.EnchantWithLevels levels -> scan(levels.levels(), out);
      case ModifierAction.Filtered filtered -> {
        filtered.onPass().forEach(m -> scan(m, out));
        filtered.onFail().forEach(m -> scan(m, out));
      }
      default -> {}
    }
  }
}
