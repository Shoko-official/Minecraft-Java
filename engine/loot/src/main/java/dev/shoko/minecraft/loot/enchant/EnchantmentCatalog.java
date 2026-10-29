package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The loaded enchantments and enchantment providers (EN-R1, EN-R34). Immutable.
 *
 * @param definitions the definitions in identifier order, unmodifiable; 43 in the vanilla data
 * @param providers the enchantment providers in identifier order, unmodifiable; 7 in the vanilla
 *     data
 */
public record EnchantmentCatalog(
    SequencedMap<Identifier, EnchantmentDefinition> definitions,
    SequencedMap<Identifier, EnchantmentProvider> providers) {

  /** Key of the {@code minecraft:enchantment} registry, for tags and {@code Holder}s. */
  public static final RegistryKey<EnchantmentDefinition> REGISTRY =
      new RegistryKey<>(Identifier.minecraft("enchantment"));

  /**
   * Finds a definition.
   *
   * @param id an enchantment identifier
   * @return the definition, empty when it is not loaded
   */
  public Optional<EnchantmentDefinition> find(Identifier id) {
    return Optional.ofNullable(definitions.get(id));
  }

  /**
   * Finds an enchantment provider.
   *
   * @param id a provider identifier such as {@code minecraft:mob_spawn_equipment}
   * @return the provider, empty when it is not loaded
   */
  public Optional<EnchantmentProvider> provider(Identifier id) {
    return Optional.ofNullable(providers.get(id));
  }

  /**
   * Lists the definitions of a set, tags expanded, in identifier order.
   *
   * @param set an identifier or tag set over the enchantment registry
   * @param access the loaded registries and tags
   * @return the definitions; identifiers that are not loaded are skipped
   */
  public List<EnchantmentDefinition> members(IdOrTagSet set, RegistryAccess access) {
    return definitions.values().stream()
        .filter(definition -> set.contains(definition.id(), access))
        .toList();
  }

  /**
   * Tells whether two enchantments can be on the same item (EN-R8, provisional): they are
   * incompatible when one cites the other in its exclusive set, and an enchantment is compatible
   * with itself.
   *
   * @param first an enchantment identifier
   * @param second an enchantment identifier
   * @param access the loaded registries and tags, for the exclusive set tags
   * @return {@code true} when neither excludes the other
   */
  public boolean areCompatible(Identifier first, Identifier second, RegistryAccess access) {
    return !isExcluded(first, second, access) && !isExcluded(second, first, access);
  }

  /**
   * Tells whether an enchantment is a curse: a member of the {@code curse} tag (EN-R9a), kept by
   * the grindstone.
   *
   * @param id an enchantment identifier
   * @param access the loaded registries and tags
   * @return {@code true} for a curse
   */
  public boolean isCurse(Identifier id, RegistryAccess access) {
    return new IdOrTagSet(REGISTRY.name(), List.of(), List.of(EnchantmentTags.CURSE), false)
        .contains(id, access);
  }

  private boolean isExcluded(Identifier owner, Identifier other, RegistryAccess access) {
    if (owner.equals(other)) {
      return false;
    }
    return find(owner)
        .map(definition -> definition.exclusiveSet().contains(other, access))
        .orElse(false);
  }
}
