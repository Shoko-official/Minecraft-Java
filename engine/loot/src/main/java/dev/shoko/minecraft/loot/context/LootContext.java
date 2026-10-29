package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.random.DrawObserver;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.chunk.BlockEntityData;
import dev.shoko.minecraft.world.chunk.Chunk;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
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

  private static final Identifier BLOCK_TYPE = Identifier.minecraft("block");

  private final LootData data;
  private final LootContextType type;
  private final RandomSource random;
  private final Map<ContextKey, Object> values;
  private final float luck;
  private final LevelFacts level;
  private final Function<ChunkPos, Optional<Chunk>> chunks;
  private final DrawObserver observer;
  private final NodeBudget budget;

  private LootContext(
      LootData data,
      LootContextType type,
      RandomSource random,
      Map<ContextKey, Object> values,
      Extras extras) {
    this.data = data;
    this.type = type;
    this.random = random;
    this.values = values;
    this.luck = extras.luck();
    this.level = extras.level();
    this.chunks = extras.chunks();
    this.observer = extras.observer();
    this.budget = extras.budget();
  }

  /** The parts of a context that are not named parameters. */
  private record Extras(
      float luck,
      LevelFacts level,
      Function<ChunkPos, Optional<Chunk>> chunks,
      DrawObserver observer,
      NodeBudget budget) {}

  private Extras extras() {
    return new Extras(luck, level, chunks, observer, budget);
  }

  /**
   * Starts a context.
   *
   * @param data the loaded data: tables, predicates, providers, enchantments, registries
   * @param type the context type
   * @param random the source consumed by this evaluation, injected by the caller
   * @return a builder with no parameter, luck 0 and no observer
   */
  public static Builder builder(LootData data, LootContextType type, RandomSource random) {
    return new Builder(data, type, random);
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
    LootContextType block = LootContextTypes.find(BLOCK_TYPE).orElseThrow();
    return builder(data, block, random).blockState(machine).build();
  }

  /**
   * Returns the loaded data.
   *
   * @return the data
   */
  public LootData data() {
    return data;
  }

  /**
   * Returns the context type.
   *
   * @return the type
   */
  public LootContextType type() {
    return type;
  }

  /**
   * Returns the source consumed by the evaluation.
   *
   * @return the source
   */
  public RandomSource random() {
    return random;
  }

  /**
   * Returns the luck (LT-R18).
   *
   * @return the luck, 0 when the caller gave none
   */
  public float luck() {
    return luck;
  }

  /**
   * Returns the observer of the draws.
   *
   * @return the observer, {@link DrawObserver#NONE} when the caller gave none
   */
  public DrawObserver observer() {
    return observer;
  }

  /**
   * Returns the level facts.
   *
   * @return the facts, empty when the caller gave none ({@code weather_check} and {@code
   *     time_check} then fail)
   */
  public Optional<LevelFacts> level() {
    return Optional.ofNullable(level);
  }

  /**
   * Returns the chunk that holds a position, for {@code location_check} and block predicates.
   *
   * @param pos a block position; the chunk is found with floor division of the coordinates
   * @return the chunk, empty when the caller gave no chunk source or the chunk is not loaded
   */
  public Optional<Chunk> chunkAt(BlockPos pos) {
    if (chunks == null) {
      return Optional.empty();
    }
    return chunks.apply(new ChunkPos(Math.floorDiv(pos.x(), 16), Math.floorDiv(pos.z(), 16)));
  }

  /**
   * Reads {@link ContextKey#ORIGIN}.
   *
   * @return the origin, empty when absent
   */
  public Optional<Vec3> origin() {
    return read(ContextKey.ORIGIN, Vec3.class);
  }

  /**
   * Reads an entity parameter.
   *
   * @param target which entity
   * @return the entity, empty when the parameter is absent
   */
  public Optional<EntityView> entity(EntityTarget target) {
    return read(target.contextKey(), EntityView.class);
  }

  /**
   * Reads {@link ContextKey#DAMAGE_SOURCE}.
   *
   * @return the damage source, empty when absent
   */
  public Optional<DamageSourceData> damageSource() {
    return read(ContextKey.DAMAGE_SOURCE, DamageSourceData.class);
  }

  /**
   * Reads {@link ContextKey#TOOL}.
   *
   * @return the tool, empty when absent
   */
  public Optional<ItemStack> tool() {
    return read(ContextKey.TOOL, ItemStack.class);
  }

  /**
   * Reads {@link ContextKey#BLOCK_STATE}.
   *
   * @return the state, empty when absent
   */
  public Optional<BlockState> blockState() {
    return read(ContextKey.BLOCK_STATE, BlockState.class);
  }

  /**
   * Reads {@link ContextKey#BLOCK_ENTITY}.
   *
   * @return the block entity, empty when absent
   */
  public Optional<BlockEntityData> blockEntity() {
    return read(ContextKey.BLOCK_ENTITY, BlockEntityData.class);
  }

  /**
   * Reads {@link ContextKey#EXPLOSION_RADIUS}.
   *
   * @return the radius, empty when absent
   */
  public OptionalDouble explosionRadius() {
    return read(ContextKey.EXPLOSION_RADIUS, Float.class)
        .map(radius -> OptionalDouble.of(radius))
        .orElse(OptionalDouble.empty());
  }

  /**
   * Reads {@link ContextKey#ENCHANTMENT_LEVEL}.
   *
   * @return the level, empty when absent
   */
  public OptionalInt enchantmentLevel() {
    return read(ContextKey.ENCHANTMENT_LEVEL, Integer.class)
        .map(value -> OptionalInt.of(value))
        .orElse(OptionalInt.empty());
  }

  /**
   * Reads {@link ContextKey#ENCHANTMENT_ACTIVE}.
   *
   * @return whether the enchantment is active, empty when absent
   */
  public Optional<Boolean> enchantmentActive() {
    return read(ContextKey.ENCHANTMENT_ACTIVE, Boolean.class);
  }

  /**
   * Lists the required parameters of the context type that the caller did not set (LT-R14b).
   *
   * @return the missing keys, empty when the context is complete
   */
  public Set<ContextKey> missingRequiredKeys() {
    Set<ContextKey> missing = EnumSet.noneOf(ContextKey.class);
    for (ContextKey key : type.required()) {
      if (!values.containsKey(key)) {
        missing.add(key);
      }
    }
    return missing;
  }

  /**
   * Returns the same context with another source, for a draw whose source is chosen after the
   * context was built (LT-R63).
   *
   * @param random the source to consume
   * @return the context
   */
  public LootContext withRandom(RandomSource random) {
    return new LootContext(data, type, random, values, extras());
  }

  /**
   * Returns the same context with another observer of the draws, for a caller that counts them.
   *
   * @param observer receives one event per random decision
   * @return the context
   */
  public LootContext withObserver(DrawObserver observer) {
    return new LootContext(
        data, type, random, values, new Extras(luck, level, chunks, observer, budget));
  }

  /**
   * Returns the same context with the budget of the work of one root draw.
   *
   * @param budget the counters the evaluation charges
   * @return the context
   */
  public LootContext withBudget(NodeBudget budget) {
    return new LootContext(
        data, type, random, values, new Extras(luck, level, chunks, observer, budget));
  }

  /**
   * Returns the counters of the work of the draw this context belongs to (LT-R74d to LT-R74f).
   *
   * @return the budget, which counts only the chain of references outside a table draw
   */
  public NodeBudget budget() {
    return budget;
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
    Map<ContextKey, Object> copy = new EnumMap<>(ContextKey.class);
    copy.putAll(values);
    copy.put(ContextKey.ENCHANTMENT_LEVEL, level);
    copy.put(ContextKey.ENCHANTMENT_ACTIVE, active);
    copy.put(ContextKey.TOOL, tool);
    return new LootContext(data, type, random, copy, extras());
  }

  /**
   * Returns the same context with the enchantment level and active flag of one effect evaluation,
   * leaving the tool as it is.
   *
   * @param level the level of the enchantment on the item
   * @param active whether the enchantment is active in the slot
   * @return the context
   */
  public LootContext withEnchantment(int level, boolean active) {
    Map<ContextKey, Object> copy = new EnumMap<>(ContextKey.class);
    copy.putAll(values);
    copy.put(ContextKey.ENCHANTMENT_LEVEL, level);
    copy.put(ContextKey.ENCHANTMENT_ACTIVE, active);
    return new LootContext(data, type, random, copy, extras());
  }

  private <T> Optional<T> read(ContextKey key, Class<T> kind) {
    return Optional.ofNullable(values.get(key)).map(kind::cast);
  }

  /** Builds a {@link LootContext}. Not thread-safe; use once. */
  public static final class Builder {

    private final LootData data;
    private final LootContextType type;
    private final RandomSource random;
    private final Map<ContextKey, Object> values = new EnumMap<>(ContextKey.class);
    private float luck;
    private LevelFacts level;
    private Function<ChunkPos, Optional<Chunk>> chunks;
    private DrawObserver observer = DrawObserver.NONE;

    private Builder(LootData data, LootContextType type, RandomSource random) {
      this.data = data;
      this.type = type;
      this.random = random;
    }

    /**
     * Sets {@link ContextKey#ORIGIN}.
     *
     * @param origin the position
     * @return this builder
     */
    public Builder origin(Vec3 origin) {
      return put(ContextKey.ORIGIN, origin);
    }

    /**
     * Sets an entity parameter.
     *
     * @param target which parameter
     * @param entity the entity
     * @return this builder
     */
    public Builder entity(EntityTarget target, EntityView entity) {
      return put(target.contextKey(), entity);
    }

    /**
     * Sets {@link ContextKey#DAMAGE_SOURCE}.
     *
     * @param source the damage source
     * @return this builder
     */
    public Builder damageSource(DamageSourceData source) {
      return put(ContextKey.DAMAGE_SOURCE, source);
    }

    /**
     * Sets {@link ContextKey#TOOL}.
     *
     * @param tool the tool stack
     * @return this builder
     */
    public Builder tool(ItemStack tool) {
      return put(ContextKey.TOOL, tool);
    }

    /**
     * Sets {@link ContextKey#BLOCK_STATE}.
     *
     * @param state the state of the block
     * @return this builder
     */
    public Builder blockState(BlockState state) {
      return put(ContextKey.BLOCK_STATE, state);
    }

    /**
     * Sets {@link ContextKey#BLOCK_ENTITY}.
     *
     * @param blockEntity the block entity
     * @return this builder
     */
    public Builder blockEntity(BlockEntityData blockEntity) {
      return put(ContextKey.BLOCK_ENTITY, blockEntity);
    }

    /**
     * Sets {@link ContextKey#EXPLOSION_RADIUS}.
     *
     * @param radius the radius
     * @return this builder
     */
    public Builder explosionRadius(float radius) {
      return put(ContextKey.EXPLOSION_RADIUS, radius);
    }

    /**
     * Sets {@link ContextKey#ENCHANTMENT_LEVEL} and {@link ContextKey#ENCHANTMENT_ACTIVE}.
     *
     * @param level the level of the enchantment
     * @param active whether it is active
     * @return this builder
     */
    public Builder enchantment(int level, boolean active) {
      put(ContextKey.ENCHANTMENT_LEVEL, level);
      return put(ContextKey.ENCHANTMENT_ACTIVE, active);
    }

    /**
     * Sets the luck (LT-R18).
     *
     * @param luck the luck of the context
     * @return this builder
     */
    public Builder luck(float luck) {
      this.luck = luck;
      return this;
    }

    /**
     * Sets the level facts.
     *
     * @param facts the weather and the time
     * @return this builder
     */
    public Builder level(LevelFacts facts) {
      this.level = facts;
      return this;
    }

    /**
     * Sets the reader of chunks, called with the chunk position of a block position.
     *
     * @param chunks gives the loaded chunk at a position, empty when it is not loaded
     * @return this builder
     */
    public Builder chunks(Function<ChunkPos, Optional<Chunk>> chunks) {
      this.chunks = chunks;
      return this;
    }

    /**
     * Sets the observer of the draws.
     *
     * @param observer receives one event per random decision
     * @return this builder
     */
    public Builder observer(DrawObserver observer) {
      this.observer = observer;
      return this;
    }

    /**
     * Builds the context. A missing required parameter does not fail: it shows in {@link
     * LootContext#missingRequiredKeys()}.
     *
     * @return the context
     */
    public LootContext build() {
      Map<ContextKey, Object> copy = new EnumMap<>(ContextKey.class);
      copy.putAll(values);
      return new LootContext(
          data,
          type,
          random,
          copy,
          new Extras(luck, level, chunks, observer, NodeBudget.unlimited()));
    }

    private Builder put(ContextKey key, Object value) {
      values.put(key, value);
      return this;
    }
  }
}
