package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which context parameters a condition reads (LT-R14b, LT-R15), for the load-time warning and for
 * tests of the parameter sets. A named reference is followed through the loaded predicates.
 */
public final class ConditionReads {

  private ConditionReads() {}

  /**
   * Lists the parameters a condition reads, nested terms included.
   *
   * @param condition a condition
   * @param named the named predicates, to follow references
   * @return the keys read, in enum order
   */
  public static Set<ContextKey> of(LootCondition condition, NamedPredicates named) {
    Set<ContextKey> keys = EnumSet.noneOf(ContextKey.class);
    collect(condition, named, keys, new HashSet<>());
    return keys;
  }

  /**
   * Reports the conditions of a table that read a parameter its type does not provide (LT-R15). The
   * table still loads.
   *
   * @param conditions the conditions of the table, in the order of the table
   * @param type the context type of the table
   * @param named the named predicates, to follow references
   * @return one {@code UNAVAILABLE_PARAMETER} warning per condition and missing parameter
   */
  public static List<LootFinding> unavailable(
      List<LootCondition> conditions, LootContextType type, NamedPredicates named) {
    List<LootFinding> findings = new ArrayList<>();
    for (LootCondition condition : conditions) {
      for (ContextKey key : of(condition, named)) {
        if (!type.allowed().contains(key)) {
          findings.add(
              new LootFinding(
                  LootFindingCode.UNAVAILABLE_PARAMETER,
                  "$",
                  "type " + type.id() + " does not provide " + key.key()));
        }
      }
    }
    return findings;
  }

  /**
   * Walks the condition and the named predicates it references with an explicit stack: a chain of
   * thousands of references must not overflow the call stack, and a cycle is followed once.
   */
  private static void collect(
      LootCondition root, NamedPredicates named, Set<ContextKey> keys, Set<Identifier> seen) {
    Deque<LootCondition> pending = new ArrayDeque<>();
    pending.push(root);
    while (!pending.isEmpty()) {
      switch (pending.pop()) {
        case LootCondition.AllOf all -> all.terms().forEach(pending::push);
        case LootCondition.AnyOf any -> any.terms().forEach(pending::push);
        case LootCondition.Inverted inverted -> pending.push(inverted.term());
        case LootCondition.Reference reference -> {
          if (seen.add(reference.predicate())) {
            named.find(reference.predicate()).ifPresent(pending::push);
          }
        }
        case LootCondition.SurvivesExplosion survives -> keys.add(ContextKey.EXPLOSION_RADIUS);
        case LootCondition.MatchTool tool -> keys.add(ContextKey.TOOL);
        case LootCondition.TableBonus bonus -> keys.add(ContextKey.TOOL);
        case LootCondition.MatchBlock block -> keys.add(ContextKey.BLOCK_STATE);
        case LootCondition.KilledByPlayer killed -> keys.add(ContextKey.ATTACKING_PLAYER);
        case LootCondition.EntityProperties properties ->
            keys.add(properties.entity().contextKey());
        case LootCondition.LocationCheck location -> keys.add(ContextKey.ORIGIN);
        case LootCondition.RandomChanceWithEnchantedBonus bonus -> keys.add(ContextKey.ATTACKER);
        case LootCondition.EnchantmentActiveCheck active -> keys.add(ContextKey.ENCHANTMENT_ACTIVE);
        case LootCondition.DamageSourceProperties damage -> damage(damage, keys);
        case LootCondition.RandomChance chance -> {
          if (chance.chance() instanceof FloatProvider.EnchantmentLevel) {
            keys.add(ContextKey.ENCHANTMENT_LEVEL);
          }
        }
        default -> {}
      }
    }
  }

  private static void damage(LootCondition.DamageSourceProperties damage, Set<ContextKey> keys) {
    keys.add(ContextKey.DAMAGE_SOURCE);
    for (DamageSourcePredicate.Requirement requirement : damage.predicate().requirements()) {
      if (requirement instanceof DamageSourcePredicate.Requirement.SourceEntity) {
        keys.add(ContextKey.ATTACKER);
      } else if (requirement instanceof DamageSourcePredicate.Requirement.DirectEntity) {
        keys.add(ContextKey.DIRECT_ATTACKER);
      }
    }
  }
}
