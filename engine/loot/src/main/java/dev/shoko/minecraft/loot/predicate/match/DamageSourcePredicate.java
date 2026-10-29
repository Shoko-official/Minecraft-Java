package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.EntityView;
import java.util.List;
import java.util.Optional;

/**
 * The damage source predicate of {@code damage_source_properties} (LT-R60). All requirements must
 * hold.
 *
 * @param requirements the keys present, in file order, unmodifiable
 */
public record DamageSourcePredicate(List<Requirement> requirements) {

  private static final Identifier DAMAGE_TYPE = Identifier.minecraft("damage_type");

  /** One key of a damage source predicate. */
  public sealed interface Requirement
      permits Requirement.Tags,
          Requirement.SourceEntity,
          Requirement.DirectEntity,
          Requirement.IsDirect {

    /**
     * The tags of the damage type.
     *
     * @param tags each tag with the membership it must have
     */
    record Tags(List<TagExpectation> tags) implements Requirement {}

    /**
     * The entity that caused the damage, the {@code attacker} parameter.
     *
     * @param entity the entity predicate
     */
    record SourceEntity(EntityPredicate entity) implements Requirement {}

    /**
     * The entity that struck, the {@code direct_attacker} parameter.
     *
     * @param entity the entity predicate
     */
    record DirectEntity(EntityPredicate entity) implements Requirement {}

    /**
     * Whether the damage is direct.
     *
     * @param expected the expected answer
     */
    record IsDirect(boolean expected) implements Requirement {}
  }

  /**
   * A tag and whether the damage type must be in it.
   *
   * @param tag the damage type tag, without the {@code #}
   * @param expected {@code true} when the type must be a member
   */
  public record TagExpectation(Identifier tag, boolean expected) {}

  /**
   * Tells whether a damage source satisfies the predicate.
   *
   * @param context the context, for the entities and the registries
   * @param source the damage source
   * @return {@code true} when every requirement holds
   */
  public boolean matches(LootContext context, DamageSourceData source) {
    for (Requirement requirement : requirements) {
      if (!holds(requirement, context, source)) {
        return false;
      }
    }
    return true;
  }

  private static boolean holds(
      Requirement requirement, LootContext context, DamageSourceData source) {
    RegistryAccess access = context.data().registries();
    return switch (requirement) {
      case Requirement.Tags tags -> tagsHold(tags, source, access);
      case Requirement.IsDirect direct -> source.direct() == direct.expected();
      case Requirement.SourceEntity entity ->
          entityHolds(entity.entity(), context.entity(EntityTarget.ATTACKER), context);
      case Requirement.DirectEntity entity ->
          entityHolds(entity.entity(), context.entity(EntityTarget.DIRECT_ATTACKER), context);
    };
  }

  private static boolean entityHolds(
      EntityPredicate predicate, Optional<EntityView> entity, LootContext context) {
    return entity.isPresent() && predicate.matches(entity.get(), context);
  }

  private static boolean tagsHold(
      Requirement.Tags tags, DamageSourceData source, RegistryAccess access) {
    for (TagExpectation tag : tags.tags()) {
      TagKey<Object> key = new TagKey<>(new RegistryKey<>(DAMAGE_TYPE), tag.tag());
      boolean member =
          access.tagMembers(key).map(list -> list.contains(source.damageType())).orElse(false);
      if (member != tag.expected()) {
        return false;
      }
    }
    return true;
  }
}
