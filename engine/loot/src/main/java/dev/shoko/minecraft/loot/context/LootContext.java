package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.random.DrawObserver;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.chunk.BlockEntityData;
import dev.shoko.minecraft.world.chunk.Chunk;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

/**
 * Everything one evaluation reads (LT-R11): the loaded data, the context type, the named
 * parameters, the luck, the level facts, the chunks and the random source. Built by the caller
 * (block broken, entity killed, chest opened, command) and passed unchanged to nested tables
 * (LT-R23).
 *
 * <p>The random source is mutable and shared by everything evaluated with this context: the order
 * of consumption is LT-R66. An absent parameter is not an error here; each reader decides (LT-R15).
 * The context is otherwise immutable.
 */
public final class LootContext {

  private LootContext() {}

  /**
   * Starts a context.
   *
   * @param data the loaded data: tables, predicates, providers, enchantments, registries
   * @param type the context type
   * @param random the source consumed by this evaluation, injected by the caller
   * @return a builder with no parameter, luck 0 and no observer
   */
  public static Builder builder(LootData data, LootContextType type, RandomSource random) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Builds the reduced block context that {@code loot} evaluates number providers and block
   * predicates with, for the machines of {@code item} (fuel, composting, brewing: LT-R50, ADR 0004
   * amendment 1, decision 2). Only {@link ContextKey#BLOCK_STATE} is set.
   *
   * @param data the loaded data
   * @param machine the state of the block of the machine
   * @param random the source consumed by the providers
   * @return the context, of type {@code minecraft:block}
   */
  public static LootContext forBlock(LootData data, BlockState machine, RandomSource random) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the loaded data.
   *
   * @return the data
   */
  public LootData data() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the context type.
   *
   * @return the type
   */
  public LootContextType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the source consumed by the evaluation.
   *
   * @return the source
   */
  public RandomSource random() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the luck (LT-R18).
   *
   * @return the luck, 0 when the caller gave none
   */
  public float luck() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the observer of the draws.
   *
   * @return the observer, {@link DrawObserver#NONE} when the caller gave none
   */
  public DrawObserver observer() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the level facts.
   *
   * @return the facts, empty when the caller gave none ({@code weather_check} and {@code
   *     time_check} then fail)
   */
  public Optional<LevelFacts> level() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the chunk that holds a position, for {@code location_check} and block predicates.
   *
   * @param pos a block position; the chunk is found with floor division of the coordinates
   * @return the chunk, empty when the caller gave no chunk source or the chunk is not loaded
   */
  public Optional<Chunk> chunkAt(BlockPos pos) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#ORIGIN}.
   *
   * @return the origin, empty when absent
   */
  public Optional<Vec3> origin() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an entity parameter.
   *
   * @param target which entity
   * @return the entity, empty when the parameter is absent
   */
  public Optional<EntityView> entity(EntityTarget target) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#DAMAGE_SOURCE}.
   *
   * @return the damage source, empty when absent
   */
  public Optional<DamageSourceData> damageSource() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#TOOL}.
   *
   * @return the tool, empty when absent
   */
  public Optional<ItemStack> tool() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#BLOCK_STATE}.
   *
   * @return the state, empty when absent
   */
  public Optional<BlockState> blockState() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#BLOCK_ENTITY}.
   *
   * @return the block entity, empty when absent
   */
  public Optional<BlockEntityData> blockEntity() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#EXPLOSION_RADIUS}.
   *
   * @return the radius, empty when absent
   */
  public OptionalDouble explosionRadius() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#ENCHANTMENT_LEVEL}.
   *
   * @return the level, empty when absent
   */
  public OptionalInt enchantmentLevel() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads {@link ContextKey#ENCHANTMENT_ACTIVE}.
   *
   * @return whether the enchantment is active, empty when absent
   */
  public Optional<Boolean> enchantmentActive() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Lists the required parameters of the context type that the caller did not set (LT-R14b).
   *
   * @return the missing keys, empty when the context is complete
   */
  public Set<ContextKey> missingRequiredKeys() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the same context with another source, for a draw whose source is chosen after the
   * context was built (LT-R63).
   *
   * @param random the source to consume
   * @return the context
   */
  public LootContext withRandom(RandomSource random) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the same context with the enchantment parameters of one effect evaluation (EN-R15).
   *
   * @param level the level of the enchantment on the item
   * @param active whether the enchantment is active in the slot
   * @param tool the stack that carries the enchantment, as {@link ContextKey#TOOL}
   * @return the context
   */
  public LootContext withEnchantment(int level, boolean active, ItemStack tool) {
    return Skeleton.throwNotImplemented();
  }

  /** Builds a {@link LootContext}. Not thread-safe; use once. */
  public static final class Builder {

    private Builder() {}

    /**
     * Sets {@link ContextKey#ORIGIN}.
     *
     * @param origin the position
     * @return this builder
     */
    public Builder origin(Vec3 origin) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets an entity parameter.
     *
     * @param target which parameter
     * @param entity the entity
     * @return this builder
     */
    public Builder entity(EntityTarget target, EntityView entity) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#DAMAGE_SOURCE}.
     *
     * @param source the damage source
     * @return this builder
     */
    public Builder damageSource(DamageSourceData source) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#TOOL}.
     *
     * @param tool the tool stack
     * @return this builder
     */
    public Builder tool(ItemStack tool) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#BLOCK_STATE}.
     *
     * @param state the state of the block
     * @return this builder
     */
    public Builder blockState(BlockState state) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#BLOCK_ENTITY}.
     *
     * @param blockEntity the block entity
     * @return this builder
     */
    public Builder blockEntity(BlockEntityData blockEntity) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#EXPLOSION_RADIUS}.
     *
     * @param radius the radius
     * @return this builder
     */
    public Builder explosionRadius(float radius) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets {@link ContextKey#ENCHANTMENT_LEVEL} and {@link ContextKey#ENCHANTMENT_ACTIVE}.
     *
     * @param level the level of the enchantment
     * @param active whether it is active
     * @return this builder
     */
    public Builder enchantment(int level, boolean active) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets the luck (LT-R18).
     *
     * @param luck the luck of the context
     * @return this builder
     */
    public Builder luck(float luck) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets the level facts.
     *
     * @param facts the weather and the time
     * @return this builder
     */
    public Builder level(LevelFacts facts) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets the reader of chunks, called with the chunk position of a block position.
     *
     * @param chunks gives the loaded chunk at a position, empty when it is not loaded
     * @return this builder
     */
    public Builder chunks(Function<ChunkPos, Optional<Chunk>> chunks) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Sets the observer of the draws.
     *
     * @param observer receives one event per random decision
     * @return this builder
     */
    public Builder observer(DrawObserver observer) {
      return Skeleton.throwNotImplemented();
    }

    /**
     * Builds the context. A missing required parameter does not fail: it shows in {@link
     * LootContext#missingRequiredKeys()}.
     *
     * @return the context
     */
    public LootContext build() {
      return Skeleton.throwNotImplemented();
    }
  }
}
