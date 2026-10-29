package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootDecoding;

/**
 * Decodes the matchers from their JSON form (LT-R34, LT-R57 to LT-R60). Every method reports a
 * wrong shape as {@code TYPE_MISMATCH} or {@code MISSING_FIELD}, a bad identifier as {@code
 * INVALID_IDENTIFIER}, and a tag that is not loaded as {@code UNRESOLVED_TAG}.
 */
public final class MatchCodecs {

  private MatchCodecs() {}

  /**
   * Decodes a range: a bare number, or an object with optional {@code min} and {@code max}.
   *
   * @param node the JSON value
   * @return the range
   */
  public static LootDecoding<NumberRange> range(JsonNode node) {
    return MatchReaders.range(node);
  }

  /**
   * Decodes an identifier, a {@code #tag} or a list of them.
   *
   * @param node the JSON value
   * @param registry the registry of the identifiers, such as {@code minecraft:block}
   * @param access the loaded tags, to check that a tag exists
   * @return the set
   */
  public static LootDecoding<IdOrTagSet> idOrTagSet(
      JsonNode node, Identifier registry, RegistryAccess access) {
    return MatchReaders.idOrTagSet(node, registry, access);
  }

  /**
   * Decodes a block predicate.
   *
   * @param node the JSON object
   * @param access the loaded tags
   * @return the predicate
   */
  public static LootDecoding<BlockMatch> block(JsonNode node, RegistryAccess access) {
    return MatchReaders.block(node, access);
  }

  /**
   * Decodes an item predicate.
   *
   * @param node the JSON object
   * @param access the loaded tags
   * @return the predicate
   */
  public static LootDecoding<ItemMatch> item(JsonNode node, RegistryAccess access) {
    return MatchReaders.item(node, access);
  }

  /**
   * Decodes an entity predicate.
   *
   * @param node the JSON object
   * @param access the loaded tags
   * @return the predicate
   */
  public static LootDecoding<EntityPredicate> entity(JsonNode node, RegistryAccess access) {
    return EntityReader.entity(node, access);
  }

  /**
   * Decodes a position predicate.
   *
   * @param node the JSON object
   * @param access the loaded tags
   * @return the predicate
   */
  public static LootDecoding<LocationPredicate> location(JsonNode node, RegistryAccess access) {
    return MatchReaders.location(node, access);
  }

  /**
   * Decodes a damage source predicate.
   *
   * @param node the JSON object
   * @param access the loaded tags
   * @return the predicate
   */
  public static LootDecoding<DamageSourcePredicate> damageSource(
      JsonNode node, RegistryAccess access) {
    return MatchReaders.damageSource(node, access);
  }
}
