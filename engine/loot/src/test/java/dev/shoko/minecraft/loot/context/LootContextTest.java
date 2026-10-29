package dev.shoko.minecraft.loot.context;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.random.DrawObserver;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.chunk.Chunk;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: LT-R11, LT-R12, LT-R15, LT-R18, ADR 0004 amendment 1 (decision 2): the context of a draw
class LootContextTest {

  private static final RandomSource RANDOM = Lcg48Random.ofSeed(5);

  private static LootContextType typeRequiring(ContextKey... required) {
    return new LootContextType(
        Identifier.minecraft("test"),
        99,
        Set.of(required),
        Set.copyOf(java.util.EnumSet.allOf(ContextKey.class)));
  }

  @Test
  void aContextWithNothingSetReadsAsAbsent() {
    // spec: LT-R11, LT-R15 (an absent parameter is not an error here)
    LootContext context = Ctx.context(RANDOM);
    assertThat(context.origin()).isEmpty();
    for (EntityTarget target : EntityTarget.values()) {
      assertThat(context.entity(target)).as(target.key()).isEmpty();
    }
    assertThat(context.damageSource()).isEmpty();
    assertThat(context.tool()).isEmpty();
    assertThat(context.blockState()).isEmpty();
    assertThat(context.blockEntity()).isEmpty();
    assertThat(context.explosionRadius()).isEmpty();
    assertThat(context.enchantmentLevel()).isEmpty();
    assertThat(context.enchantmentActive()).isEmpty();
    assertThat(context.level()).isEmpty();
  }

  @Test
  void theLuckDefaultsToZeroAndTheObserverToNone() {
    // spec: LT-R18 (luck 0 by default), design contract of the observer
    LootContext context = Ctx.context(RANDOM);
    assertThat(context.luck()).isZero();
    assertThat(context.observer()).isSameAs(DrawObserver.NONE);
  }

  @Test
  void theContextKeepsTheDataTheTypeAndTheSource() {
    // spec: LT-R11
    LootData data = Ctx.data();
    LootContext context = LootContext.builder(data, Ctx.ANY, RANDOM).build();
    assertThat(context.data()).isSameAs(data);
    assertThat(context.type()).isSameAs(Ctx.ANY);
    assertThat(context.random()).isSameAs(RANDOM);
  }

  @Test
  void everyParameterSetByTheBuilderIsReadBack() {
    // spec: LT-R12
    FakeEntity victim = FakeEntity.of("cow");
    FakeEntity attacker = FakeEntity.of("player");
    ItemStack tool = LootItems.stack("iron_pickaxe");
    BlockState state = Blocks.wheat(7);
    DamageSourceData source = new DamageSourceData(Identifier.minecraft("player_attack"), true);
    DrawLog log = new DrawLog();
    LootContext context =
        Ctx.builder(RANDOM)
            .origin(new Vec3(1.5, 64, -2.5))
            .entity(EntityTarget.THIS, victim)
            .entity(EntityTarget.ATTACKER, attacker)
            .damageSource(source)
            .tool(tool)
            .blockState(state)
            .explosionRadius(4f)
            .enchantment(3, true)
            .luck(1.5f)
            .observer(log)
            .build();
    assertThat(context.origin()).contains(new Vec3(1.5, 64, -2.5));
    assertThat(context.entity(EntityTarget.THIS)).containsSame(victim);
    assertThat(context.entity(EntityTarget.ATTACKER)).containsSame(attacker);
    assertThat(context.entity(EntityTarget.DIRECT_ATTACKER)).isEmpty();
    assertThat(context.damageSource()).contains(source);
    assertThat(context.tool()).contains(tool);
    assertThat(context.blockState()).contains(state);
    assertThat(context.explosionRadius().getAsDouble()).isEqualTo(4.0);
    assertThat(context.enchantmentLevel().getAsInt()).isEqualTo(3);
    assertThat(context.enchantmentActive()).contains(true);
    assertThat(context.luck()).isEqualTo(1.5f);
    assertThat(context.observer()).isSameAs(log);
  }

  @Test
  void eachEntityTargetIsASeparateParameter() {
    // spec: LT-R12 (this, attacker, direct_attacker, attacking_player...)
    java.util.Map<EntityTarget, EntityView> entities = new java.util.EnumMap<>(EntityTarget.class);
    LootContext.Builder builder = Ctx.builder(RANDOM);
    for (EntityTarget target : EntityTarget.values()) {
      entities.put(target, FakeEntity.of("zombie"));
      builder.entity(target, entities.get(target));
    }
    LootContext context = builder.build();
    for (EntityTarget target : EntityTarget.values()) {
      assertThat(context.entity(target)).as(target.key()).containsSame(entities.get(target));
    }
  }

  @Test
  void missingRequiredKeysListsWhatTheCallerDidNotSet() {
    // spec: LT-R14b (a missing required parameter does not fail the build)
    LootContextType type = typeRequiring(ContextKey.ORIGIN, ContextKey.TOOL);
    LootContext none = LootContext.builder(Ctx.data(), type, RANDOM).build();
    assertThat(none.missingRequiredKeys())
        .containsExactlyInAnyOrder(ContextKey.ORIGIN, ContextKey.TOOL);
    LootContext some =
        LootContext.builder(Ctx.data(), type, RANDOM).origin(new Vec3(0, 0, 0)).build();
    assertThat(some.missingRequiredKeys()).containsExactly(ContextKey.TOOL);
    LootContext all =
        LootContext.builder(Ctx.data(), type, RANDOM)
            .origin(new Vec3(0, 0, 0))
            .tool(LootItems.stack("iron_pickaxe"))
            .build();
    assertThat(all.missingRequiredKeys()).isEmpty();
  }

  @Test
  void withRandomKeepsEveryParameterAndChangesTheSource() {
    // spec: LT-R63 (the source is chosen after the context was built)
    ItemStack tool = LootItems.stack("iron_pickaxe");
    LootContext context = Ctx.builder(RANDOM).tool(tool).luck(2f).build();
    RandomSource other = Lcg48Random.ofSeed(99);
    LootContext moved = context.withRandom(other);
    assertThat(moved.random()).isSameAs(other);
    assertThat(moved.tool()).contains(tool);
    assertThat(moved.luck()).isEqualTo(2f);
    assertThat(context.random()).isSameAs(RANDOM);
  }

  @Test
  void withEnchantmentSetsTheLevelTheActiveFlagAndTheTool() {
    // spec: EN-R15 (parameters of one effect evaluation)
    ItemStack tool = LootItems.stack("diamond_sword");
    LootContext context = Ctx.context(RANDOM);
    LootContext effect = context.withEnchantment(4, true, tool);
    assertThat(effect.enchantmentLevel().getAsInt()).isEqualTo(4);
    assertThat(effect.enchantmentActive()).contains(true);
    assertThat(effect.tool()).contains(tool);
    assertThat(context.enchantmentLevel()).isEmpty();
    assertThat(context.tool()).isEmpty();
  }

  @Test
  void forBlockSetsOnlyTheBlockState() {
    // spec: ADR 0004 amendment 1 decision 2, LT-R50 (reduced block context of a machine)
    BlockState furnace = Blocks.plain("furnace", 7, 70);
    LootContext context = LootContext.forBlock(Ctx.data(), furnace, RANDOM);
    assertThat(context.type().id()).isEqualTo(Identifier.minecraft("block"));
    assertThat(context.blockState()).contains(furnace);
    assertThat(context.random()).isSameAs(RANDOM);
    assertThat(context.origin()).isEmpty();
    assertThat(context.tool()).isEmpty();
    assertThat(context.entity(EntityTarget.THIS)).isEmpty();
    assertThat(context.explosionRadius()).isEmpty();
  }

  @Test
  void chunkAtAsksTheReaderWithTheFloorDivisionOfTheCoordinates() {
    // spec: LT-R59 (negative coordinates use floor division)
    List<ChunkPos> asked = new ArrayList<>();
    LootContext context =
        Ctx.builder(RANDOM)
            .chunks(
                pos -> {
                  asked.add(pos);
                  return Optional.<Chunk>empty();
                })
            .build();
    assertThat(context.chunkAt(new BlockPos(-1, 64, -17))).isEmpty();
    assertThat(context.chunkAt(new BlockPos(15, 0, 16))).isEmpty();
    assertThat(context.chunkAt(new BlockPos(16, 0, -16))).isEmpty();
    assertThat(asked)
        .containsExactly(new ChunkPos(-1, -2), new ChunkPos(0, 1), new ChunkPos(1, -1));
  }

  @Test
  void chunkAtIsEmptyWhenTheCallerGaveNoChunkSource() {
    // spec: LootContext contract (chunkAt)
    assertThat(Ctx.context(RANDOM).chunkAt(new BlockPos(0, 0, 0))).isEmpty();
  }
}
