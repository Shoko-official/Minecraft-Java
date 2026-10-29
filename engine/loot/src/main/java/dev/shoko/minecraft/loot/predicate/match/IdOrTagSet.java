package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An identifier, a {@code #tag} or a list of both, as written for {@code blocks}, {@code items},
 * {@code biomes}, {@code entity_type} and {@code options} (LT-R34, LT-R45, LT-R58, LT-R59). Tags
 * stay unresolved: they are looked up through the registries at evaluation time, so a datapack
 * reload is seen.
 *
 * @param registry the registry the identifiers belong to, such as {@code minecraft:block}
 * @param ids the explicit identifiers in file order, unmodifiable
 * @param tags the tag identifiers, without the {@code #}, in file order, unmodifiable
 * @param unrestricted {@code true} when the key was absent: everything of the registry matches,
 *     {@code ids} and {@code tags} are empty
 */
public record IdOrTagSet(
    Identifier registry, List<Identifier> ids, List<Identifier> tags, boolean unrestricted) {

  /**
   * Tells whether an identifier is in the set.
   *
   * @param id an element of the registry
   * @param access the loaded registries and tags
   * @return {@code true} when unrestricted, listed, or a member of one of the tags
   */
  public boolean contains(Identifier id, RegistryAccess access) {
    if (unrestricted || ids.contains(id)) {
      return true;
    }
    for (Identifier tag : tags) {
      if (tagMembers(tag, access).contains(id)) {
        return true;
      }
    }
    return false;
  }

  private List<Identifier> tagMembers(Identifier tag, RegistryAccess access) {
    TagKey<Object> key = new TagKey<>(new RegistryKey<>(registry), tag);
    return access.tagMembers(key).orElse(List.of());
  }

  /**
   * Lists the members in registry order, tags expanded and duplicates removed (LT-R22, EN-R35).
   *
   * @param access the loaded registries and tags
   * @return the members; for an unrestricted set, every element of the registry
   */
  public List<Identifier> members(RegistryAccess access) {
    Optional<Registry<Object>> known = access.registry(new RegistryKey<>(registry));
    if (known.isPresent()) {
      return known.get().ids().stream().filter(id -> contains(id, access)).toList();
    }
    Set<Identifier> members = new LinkedHashSet<>(ids);
    for (Identifier tag : tags) {
      members.addAll(tagMembers(tag, access));
    }
    return List.copyOf(members);
  }
}
