package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.predicate.match.NumberRange;
import java.util.Arrays;
import java.util.List;

/** Short constructors for {@link IdOrTagSet} and {@link NumberRange}. */
public final class Sets {

  private static final Identifier ITEM = Identifier.minecraft("item");
  private static final Identifier BLOCK = Identifier.minecraft("block");
  private static final Identifier ENTITY_TYPE = Identifier.minecraft("entity_type");
  private static final Identifier ENCHANTMENT = Identifier.minecraft("enchantment");

  private Sets() {}

  private static List<Identifier> ids(String... paths) {
    return Arrays.stream(paths).map(Identifier::minecraft).toList();
  }

  public static IdOrTagSet items(String... paths) {
    return new IdOrTagSet(ITEM, ids(paths), List.of(), false);
  }

  public static IdOrTagSet itemTag(String tag) {
    return new IdOrTagSet(ITEM, List.of(), ids(tag), false);
  }

  public static IdOrTagSet anyItem() {
    return new IdOrTagSet(ITEM, List.of(), List.of(), true);
  }

  public static IdOrTagSet blocks(String... paths) {
    return new IdOrTagSet(BLOCK, ids(paths), List.of(), false);
  }

  public static IdOrTagSet entityTypes(String... paths) {
    return new IdOrTagSet(ENTITY_TYPE, ids(paths), List.of(), false);
  }

  public static IdOrTagSet entityTypeTag(String tag) {
    return new IdOrTagSet(ENTITY_TYPE, List.of(), ids(tag), false);
  }

  public static IdOrTagSet enchantments(String... paths) {
    return new IdOrTagSet(ENCHANTMENT, ids(paths), List.of(), false);
  }

  public static IdOrTagSet enchantmentTag(String tag) {
    return new IdOrTagSet(ENCHANTMENT, List.of(), ids(tag), false);
  }

  public static IdOrTagSet anyEnchantment() {
    return new IdOrTagSet(ENCHANTMENT, List.of(), List.of(), true);
  }

  /** The range that accepts every number. */
  public static NumberRange anyNumber() {
    return new NumberRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public static NumberRange between(double min, double max) {
    return new NumberRange(min, max);
  }

  public static NumberRange atLeast(double min) {
    return new NumberRange(min, Double.POSITIVE_INFINITY);
  }
}
