package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.registry.ItemCatalog;

/**
 * Decodes one recipe file (RC-R1 to RC-R24).
 *
 * <p>Failures, as codes: a {@code type} outside the 22 serializers is {@code UNKNOWN_RECIPE_TYPE}
 * and a missing mandatory field {@code MISSING_FIELD}. An empty ingredient list is {@code
 * EMPTY_INGREDIENT} (RC-R9), an item absent from the registry {@code UNRESOLVED_ITEM}, a tag that
 * the access does not know {@code UNRESOLVED_TAG} (RC-R13). A pattern that is empty, taller or
 * wider than 3, or ragged is {@code PATTERN_SHAPE}, a key with no use {@code PATTERN_KEY_UNUSED}
 * and a character with no key {@code PATTERN_KEY_MISSING} (RC-R14, RC-R15). A result whose count
 * exceeds the stack size is {@code RESULT_EXCEEDS_STACK} (RC-R24). A missing {@code group} gives
 * the empty string, a missing {@code category} {@code MISC}, a missing {@code show_notification}
 * true (RC-R6 to RC-R8, provisional).
 */
public final class RecipeCodec {

  private RecipeCodec() {}

  /**
   * Creates a codec.
   *
   * @param items the loaded items, which give the prototypes and stack sizes of results
   * @return the codec
   */
  public static RecipeCodec of(ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes a recipe.
   *
   * @param id the identifier derived from the file path (RC-R1)
   * @param node the content of the file
   * @param access the loaded tags, to check that cited tags exist
   * @return the recipe, or the failure
   */
  public Decoding<Recipe> decode(Identifier id, JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }
}
