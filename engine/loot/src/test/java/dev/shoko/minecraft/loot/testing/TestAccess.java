package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * A hand-made {@link RegistryAccess}: a few registries (items, blocks, entity types, damage types,
 * enchantments) and resolved tags. Tags hold their members already flattened. Immutable: every
 * {@code with} method returns a new access.
 */
public final class TestAccess implements RegistryAccess {

  private final Map<Identifier, Registry<?>> registries;
  private final Map<Identifier, Map<Identifier, List<Identifier>>> tags;

  private TestAccess(
      Map<Identifier, Registry<?>> registries,
      Map<Identifier, Map<Identifier, List<Identifier>>> tags) {
    this.registries = registries;
    this.tags = tags;
  }

  /** The made-up items, blocks, entity types and damage types, with the tags the tests cite. */
  public static TestAccess standard() {
    TestAccess access =
        new TestAccess(new HashMap<>(), new HashMap<>())
            .withItems()
            .withRegistry(
                "block",
                "stone",
                "cobblestone",
                "wheat",
                "oak_leaves",
                "decorated_pot",
                "furnace",
                "smoker",
                "blast_furnace",
                "composter")
            .withRegistry(
                "entity_type",
                "cow",
                "zombie",
                "skeleton",
                "sheep",
                "slime",
                "pillager",
                "player",
                "fishing_bobber")
            .withRegistry("damage_type", "player_attack", "arrow", "out_of_world", "thorns");
    return access
        .withTag("item", "diamond_tool_materials", "diamond")
        .withTag("item", "iron_tool_materials", "iron_ingot")
        .withTag("item", "bow_materials", "stick")
        .withTag("item", "trident_materials", "iron_ingot")
        .withTag("item", "test_gems", "diamond", "emerald", "iron_ingot")
        .withTag("item", "empty_tag")
        .withTag("item", "enchantable/sharp_weapon", "diamond_sword", "diamond_axe")
        .withTag("item", "enchantable/melee_weapon", "diamond_sword")
        .withTag("item", "enchantable/weapon", "diamond_sword", "diamond_axe")
        .withTag("item", "enchantable/armor", "diamond_chestplate")
        .withTag("item", "enchantable/equippable", "diamond_chestplate")
        .withTag("item", "enchantable/bow", "bow")
        .withTag("item", "enchantable/trident", "trident")
        .withTag("item", "enchantable/mining", "iron_pickaxe")
        .withTag("item", "enchantable/mining_loot", "iron_pickaxe")
        .withTag(
            "item",
            "enchantable/durability",
            "diamond_sword",
            "diamond_axe",
            "iron_pickaxe",
            "bow",
            "trident",
            "diamond_chestplate")
        .withTag("entity_type", "undead", "zombie", "skeleton")
        .withTag("damage_type", "is_projectile", "arrow")
        .withTag("damage_type", "bypasses_invulnerability", "out_of_world");
  }

  /** The identifiers of a registry, empty when the registry is unknown. */
  public List<Identifier> ids(String registry) {
    Registry<?> known = registries.get(Identifier.minecraft(registry));
    return known == null ? List.of() : known.ids();
  }

  /** The tags of a registry with their flattened members. */
  public Map<Identifier, List<Identifier>> tagsOf(String registry) {
    return tags.getOrDefault(Identifier.minecraft(registry), Map.of());
  }

  private TestAccess withItems() {
    Map<Identifier, Registry<?>> copy = new HashMap<>(registries);
    copy.put(ItemCatalog.REGISTRY.name(), LootItems.catalog().registry());
    return new TestAccess(copy, tags);
  }

  /** The same access with a registry of plain identifiers, given as paths of {@code minecraft}. */
  public TestAccess withRegistry(String registry, String... paths) {
    Identifier name = Identifier.minecraft(registry);
    SequencedMap<Identifier, String> values = new LinkedHashMap<>();
    for (String path : paths) {
      values.put(Identifier.minecraft(path), "minecraft:" + path);
    }
    Map<Identifier, Registry<?>> copy = new HashMap<>(registries);
    copy.put(name, Registry.of(new RegistryKey<String>(name), values));
    return new TestAccess(copy, tags);
  }

  /** The same access with the enchantment registry of these definitions. */
  public TestAccess withEnchantments(SequencedMap<Identifier, EnchantmentDefinition> definitions) {
    Map<Identifier, Registry<?>> copy = new HashMap<>(registries);
    copy.put(
        EnchantmentCatalog.REGISTRY.name(), Registry.of(EnchantmentCatalog.REGISTRY, definitions));
    return new TestAccess(copy, tags);
  }

  /** The same access with one more tag; members are paths of {@code minecraft}. */
  public TestAccess withTag(String registry, String tag, String... members) {
    Map<Identifier, Map<Identifier, List<Identifier>>> copy = new HashMap<>(tags);
    Map<Identifier, List<Identifier>> byTag =
        new HashMap<>(copy.getOrDefault(Identifier.minecraft(registry), Map.of()));
    List<Identifier> ids = new ArrayList<>();
    for (String member : members) {
      ids.add(Identifier.minecraft(member));
    }
    byTag.put(Identifier.minecraft(tag), List.copyOf(ids));
    copy.put(Identifier.minecraft(registry), byTag);
    return new TestAccess(registries, copy);
  }

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    Registry<?> known = registries.get(registry.name());
    return known != null && known.contains(id);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    return Optional.ofNullable((Registry<T>) registries.get(key.name()));
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    return Optional.ofNullable(tags.get(tag.registry().name())).map(byTag -> byTag.get(tag.id()));
  }
}
