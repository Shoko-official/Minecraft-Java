package dev.shoko.minecraft.loot.port;

/**
 * The state read by the {@code type_specific/*} predicates (LT-R16, LT-R57a). An entity is of one
 * kind only: a {@code type_specific/sheep} predicate on any other kind is false.
 */
public sealed interface TypeSpecific
    permits TypeSpecific.Sheep,
        TypeSpecific.CubeMob,
        TypeSpecific.Raider,
        TypeSpecific.FishingHook,
        TypeSpecific.Player {

  /**
   * A sheep.
   *
   * @param sheared whether it has been sheared
   */
  record Sheep(boolean sheared) implements TypeSpecific {}

  /**
   * A cube mob (slime, magma cube).
   *
   * @param size its size
   */
  record CubeMob(int size) implements TypeSpecific {}

  /**
   * A raider.
   *
   * @param captain whether it is the raid captain
   */
  record Raider(boolean captain) implements TypeSpecific {}

  /**
   * A fishing hook.
   *
   * @param inOpenWater whether the hook is in open water
   */
  record FishingHook(boolean inOpenWater) implements TypeSpecific {}

  /**
   * A player. The keys of {@code type_specific/player} are not listed by the spec (LT-R57a: 3
   * usages), so no field is declared yet.
   */
  record Player() implements TypeSpecific {}
}
