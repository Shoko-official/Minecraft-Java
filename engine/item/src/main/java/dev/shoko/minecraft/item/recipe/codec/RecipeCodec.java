package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.StackCodec;

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
 * true (RC-R6 to RC-R8, provisional). A member the serializer does not know is ignored.
 */
public final class RecipeCodec {

  private static final String MINECRAFT = Identifier.DEFAULT_NAMESPACE;

  private final ItemCatalog items;
  private final StackCodec stacks;

  private RecipeCodec(ItemCatalog items) {
    this.items = items;
    this.stacks = StackCodec.of(items);
  }

  /**
   * Creates a codec.
   *
   * @param items the loaded items, which give the prototypes and stack sizes of results
   * @return the codec
   */
  public static RecipeCodec of(ItemCatalog items) {
    return new RecipeCodec(items);
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
    RecipeReader reader = new RecipeReader(items, stacks, new ItemAccess(access, items));
    return Decoding.attempt(() -> read(reader, id, RecipeReader.object(node, "$")));
  }

  private static Recipe read(RecipeReader reader, Identifier id, JsonObject root) {
    JsonNode typeNode = RecipeReader.required(root, "type", "$");
    Identifier type = RecipeReader.identifier(typeNode, "$.type");
    if (!type.namespace().equals(MINECRAFT)) {
      throw unknown(type);
    }
    return switch (type.path()) {
      case "crafting_shaped" -> new CraftingReaders(reader).shaped(id, root);
      case "crafting_shapeless" -> new CraftingReaders(reader).shapeless(id, root);
      case "crafting_transmute" -> new CraftingReaders(reader).transmute(id, root);
      case "smelting" -> machines(reader).cooking(id, RecipeType.SMELTING, root);
      case "blasting" -> machines(reader).cooking(id, RecipeType.BLASTING, root);
      case "smoking" -> machines(reader).cooking(id, RecipeType.SMOKING, root);
      case "campfire_cooking" -> machines(reader).cooking(id, RecipeType.CAMPFIRE_COOKING, root);
      case "stonecutting" -> machines(reader).stonecutting(id, root);
      case "smithing_transform" -> machines(reader).smithingTransform(id, root);
      case "smithing_trim" -> machines(reader).smithingTrim(id, root);
      case "brewing" -> machines(reader).brewing(id, root);
      default -> special(reader, id, type, root);
    };
  }

  private static MachineReaders machines(RecipeReader reader) {
    return new MachineReaders(reader);
  }

  private static Recipe special(
      RecipeReader reader, Identifier id, Identifier type, JsonObject root) {
    SpecialKind kind =
        switch (type.path()) {
          case "crafting_special_bannerduplicate" -> SpecialKind.BANNER_DUPLICATE;
          case "crafting_special_bookcloning" -> SpecialKind.BOOK_CLONING;
          case "crafting_special_firework_rocket" -> SpecialKind.FIREWORK_ROCKET;
          case "crafting_special_firework_star" -> SpecialKind.FIREWORK_STAR;
          case "crafting_special_firework_star_fade" -> SpecialKind.FIREWORK_STAR_FADE;
          case "crafting_special_mapextending" -> SpecialKind.MAP_EXTENDING;
          case "crafting_special_repairitem" -> SpecialKind.REPAIR_ITEM;
          case "crafting_special_shielddecoration" -> SpecialKind.SHIELD_DECORATION;
          case "crafting_decorated_pot" -> SpecialKind.DECORATED_POT;
          case "crafting_dye" -> SpecialKind.DYE;
          case "crafting_imbue" -> SpecialKind.IMBUE;
          default -> throw unknown(type);
        };
    return new SpecialReader(reader).read(id, kind, root);
  }

  private static FindingException unknown(Identifier type) {
    return new FindingException(
        FindingCode.UNKNOWN_RECIPE_TYPE, "$.type", "no recipe serializer " + type);
  }
}
