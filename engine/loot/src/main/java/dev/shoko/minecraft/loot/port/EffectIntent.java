package dev.shoko.minecraft.loot.port;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.world.block.BlockState;
import java.util.SequencedMap;

/**
 * A side effect asked of the game by an entity or location effect (LT-R17, EN-R26, EN-R28). One
 * record per effect type of the registry {@code enchantment_entity_effect_type}, {@code all_of}
 * excepted: it is flattened into its sub-intents in order. All values are final: level-based values
 * have been evaluated.
 */
public sealed interface EffectIntent
    permits EffectIntent.DamageEntity,
        EffectIntent.ApplyMobEffect,
        EffectIntent.Ignite,
        EffectIntent.ApplyImpulse,
        EffectIntent.ApplyExhaustion,
        EffectIntent.ChangeItemDamage,
        EffectIntent.Explode,
        EffectIntent.ReplaceBlock,
        EffectIntent.ReplaceDisk,
        EffectIntent.SetBlockProperties,
        EffectIntent.SummonEntity,
        EffectIntent.PlaySound,
        EffectIntent.SpawnParticles,
        EffectIntent.RunFunction {

  /**
   * Hurts an entity.
   *
   * @param target the victim
   * @param damageType the damage type, such as {@code minecraft:thorns}
   * @param amount the damage, between the {@code min_damage} and {@code max_damage} drawn
   */
  record DamageEntity(EntityView target, Identifier damageType, float amount)
      implements EffectIntent {}

  /**
   * Applies a status effect.
   *
   * @param target the entity
   * @param effect the effect identifier
   * @param duration the duration in ticks
   * @param amplifier the amplifier
   */
  record ApplyMobEffect(EntityView target, Identifier effect, int duration, int amplifier)
      implements EffectIntent {}

  /**
   * Sets an entity on fire.
   *
   * @param target the entity
   * @param seconds the duration in seconds
   */
  record Ignite(EntityView target, float seconds) implements EffectIntent {}

  /**
   * Pushes an entity.
   *
   * @param target the entity
   * @param impulse the velocity added
   */
  record ApplyImpulse(EntityView target, Vec3 impulse) implements EffectIntent {}

  /**
   * Adds exhaustion to a player.
   *
   * @param target the player
   * @param amount the exhaustion
   */
  record ApplyExhaustion(EntityView target, float amount) implements EffectIntent {}

  /**
   * Changes the damage of the item held in a slot.
   *
   * @param holder the entity that holds the item
   * @param slot the slot of the item
   * @param amount the damage added; negative repairs
   */
  record ChangeItemDamage(EntityView holder, EquipmentSlot slot, int amount)
      implements EffectIntent {}

  /**
   * Makes an explosion.
   *
   * @param center the center
   * @param radius the radius
   * @param knockbackMultiplier the multiplier of the knockback
   */
  record Explode(Vec3 center, float radius, float knockbackMultiplier) implements EffectIntent {}

  /**
   * Replaces one block.
   *
   * @param pos the position
   * @param state the new state
   */
  record ReplaceBlock(BlockPos pos, BlockState state) implements EffectIntent {}

  /**
   * Replaces the blocks of a disk that satisfy the block predicate of the effect.
   *
   * @param center the center of the disk
   * @param state the new state
   * @param radius the radius
   * @param height the height
   */
  record ReplaceDisk(BlockPos center, BlockState state, float radius, int height)
      implements EffectIntent {}

  /**
   * Changes properties of the block at a position.
   *
   * @param pos the position
   * @param properties the property values to set, by property name
   */
  record SetBlockProperties(BlockPos pos, SequencedMap<String, String> properties)
      implements EffectIntent {}

  /**
   * Summons an entity.
   *
   * @param entity the entity type
   * @param position where
   */
  record SummonEntity(Identifier entity, Vec3 position) implements EffectIntent {}

  /**
   * Plays a sound.
   *
   * @param sound the sound identifier
   * @param position where
   * @param volume the volume
   * @param pitch the pitch, final value
   */
  record PlaySound(Identifier sound, Vec3 position, float volume, float pitch)
      implements EffectIntent {}

  /**
   * Spawns particles.
   *
   * @param particle the particle identifier
   * @param position where
   * @param speed the speed
   */
  record SpawnParticles(Identifier particle, Vec3 position, float speed) implements EffectIntent {}

  /**
   * Runs a function.
   *
   * @param function the function identifier
   */
  record RunFunction(Identifier function) implements EffectIntent {}
}
