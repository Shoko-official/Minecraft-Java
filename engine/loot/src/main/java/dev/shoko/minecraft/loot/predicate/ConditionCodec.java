package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootDecoding;
import java.util.List;

/**
 * Decodes conditions from their JSON form (LT-R3, LT-R4a). The discriminant is {@code type}; a
 * string is a reference to a named predicate; {@code terms} accepts objects and references. The
 * forms of earlier versions ({@code condition} as discriminant) are not decoded (LT-R3).
 */
public final class ConditionCodec {

  private ConditionCodec() {}

  /**
   * Decodes one condition.
   *
   * @param node an object or a string
   * @param access the loaded registries and tags
   * @return the condition; {@code UNKNOWN_TYPE} for a type outside the 20, {@code MISSING_FIELD} or
   *     {@code TYPE_MISMATCH} for a wrong shape. A reference is not checked here
   */
  public static LootDecoding<LootCondition> decode(JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the value of a {@code condition} key. A single condition is a list of one; a list is
   * kept in order and means {@code all_of} (LT-R4b, provisional).
   *
   * @param node an object, a string or an array
   * @param access the loaded registries and tags
   * @return the conditions in file order
   */
  public static LootDecoding<List<LootCondition>> decodeList(JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the {@code predicate} registry.
   *
   * @param elements the elements found by the loader
   * @param access the loaded registries and tags
   * @return the named predicates; one that fails is absent and listed in the findings, in
   *     identifier order
   */
  public static LootDecoding<NamedPredicates> decodeNamed(
      ElementRegistryData elements, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }
}
