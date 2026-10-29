package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.codec.RecipeCodec;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.stream.Stream;

/**
 * The fake items of the shared fixtures plus a few more (books, arrows, fireworks, bundles), with
 * the component types they need, so that special recipes can be exercised without the reference
 * data.
 */
final class HuntFixtures {

  private static ItemCatalog cached;

  private HuntFixtures() {}

  private static List<Identifier> componentTypes() {
    return Stream.concat(
            TestItems.COMPONENT_IDS.stream(),
            Stream.of("bundle_contents", "charged_projectiles", "written_book_content", "map_id")
                .map(Identifier::minecraft))
        .toList();
  }

  static synchronized ItemCatalog catalog() {
    if (cached == null) {
      TestItems.Source source = TestItems.source().withComponentTypes(componentTypes());
      for (String path :
          List.of("written_book", "arrow", "firework_star", "firework_rocket", "red_dye")) {
        source = source.plusItem(TestItems.entry(path, path.equals("written_book") ? 16 : 64));
      }
      source = source.plusItem(TestItems.entry("writable_book", 1));
      source = source.plusItem(TestItems.entry("bundle", 1, Map.of("bundle_contents", "[]")));
      source = source.plusItem(TestItems.entry("crossbow", 1, Map.of("charged_projectiles", "[]")));
      cached = Findings.value(ItemCatalog.load(source));
    }
    return cached;
  }

  static ItemType type(String path) {
    return catalog().find(Identifier.minecraft(path)).orElseThrow();
  }

  static ItemStack stack(String path, int count) {
    return ItemStack.of(type(path), count);
  }

  static ItemStack stack(String path) {
    return stack(path, 1);
  }

  /** Item tags of the fixtures, over the extended catalog. */
  static RegistryAccess access() {
    Map<Identifier, List<Identifier>> tags = new HashMap<>();
    tags.put(Identifier.minecraft("planks"), ids("oak_planks", "birch_planks"));
    tags.put(Identifier.minecraft("book_cloning_target"), ids("writable_book"));
    tags.put(Identifier.minecraft("trim_materials"), ids("emerald", "iron_ingot"));
    tags.put(
        Identifier.minecraft("trimmable_armor"),
        ids("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"));
    return new Access(catalog(), tags);
  }

  private static List<Identifier> ids(String... paths) {
    return Stream.of(paths).map(Identifier::minecraft).toList();
  }

  static Optional<Recipe> decode(String path, String relaxedJson) {
    var decoding =
        RecipeCodec.of(catalog())
            .decode(Identifier.minecraft(path), Json.parse(relaxedJson), access());
    return Optional.of(Findings.value(decoding));
  }

  /** A set of recipes from alternating paths and JSON texts; all must load. */
  static RecipeSet recipes(String... pathsAndJson) {
    SequencedMap<Identifier, Recipe> recipes = new LinkedHashMap<>();
    for (int i = 0; i < pathsAndJson.length; i += 2) {
      Recipe recipe = decode(pathsAndJson[i], pathsAndJson[i + 1]).orElseThrow();
      recipes.put(recipe.id(), recipe);
    }
    return RecipeSet.of(recipes);
  }

  /** A grid from cell texts of {@code Cells}: "-" empty, "path" or "path:count". */
  static CraftingGrid grid(int width, int height, ItemStack... stacks) {
    List<ItemStack> cells = new ArrayList<>(List.of(stacks));
    while (cells.size() < width * height) {
      cells.add(ItemStack.empty());
    }
    assertThat(cells).hasSize(width * height);
    return new CraftingGrid(width, height, cells);
  }

  private record Access(ItemCatalog items, Map<Identifier, List<Identifier>> itemTags)
      implements RegistryAccess {

    @Override
    public boolean contains(RegistryKey<?> registry, Identifier id) {
      return registry.equals(ItemCatalog.REGISTRY) && items.find(id).isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
      return key.equals(ItemCatalog.REGISTRY)
          ? Optional.of((Registry<T>) items.registry())
          : Optional.empty();
    }

    @Override
    public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
      return tag.registry().equals(ItemCatalog.REGISTRY)
          ? Optional.ofNullable(itemTags.get(tag.id()))
          : Optional.empty();
    }
  }
}
