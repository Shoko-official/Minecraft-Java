package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LevelFacts;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.eval.ContainerUnpacker;
import dev.shoko.minecraft.loot.eval.LootEvaluator;
import dev.shoko.minecraft.loot.eval.LootResult;
import dev.shoko.minecraft.loot.eval.UnpackResult;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.SequenceStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R49, LT-R54, LT-R55, LT-R67 (determinism), LT-R70 (conservation): every real table,
// with random contexts of every context type.
@Timeout(900)
@Tag("oracle")
class RealCorpusHuntTest {

  private static final String[] TOOLS = {
    "iron_pickaxe", "diamond_sword", "stone", "stick", "shears"
  };
  private static final String[] ENTITIES = {
    "player", "zombie", "cow", "sheep", "creeper", "pig", "fishing_bobber"
  };
  private static final String[] ENCHANTS = {
    "minecraft:fortune",
    "minecraft:silk_touch",
    "minecraft:looting",
    "minecraft:fire_aspect",
    "minecraft:sharpness",
    "minecraft:luck_of_the_sea"
  };

  private static LootContext context(
      LootData data, LootContextType type, Random random, long drawSeed) {
    LootContext.Builder builder = LootContext.builder(data, type, Lcg48Random.ofSeed(drawSeed));
    float[] lucks = {0f, 1f, 3f, -1f, 100f, Float.NaN, Float.POSITIVE_INFINITY};
    builder.luck(lucks[random.nextInt(lucks.length)]);
    for (ContextKey key : type.allowed()) {
      if (random.nextInt(4) == 0) {
        continue;
      }
      switch (key) {
        case ORIGIN ->
            builder.origin(
                new Vec3(
                    random.nextInt(2_000_000) - 1_000_000 + random.nextDouble(),
                    random.nextInt(400) - 64,
                    -random.nextInt(2_000_000)));
        case THIS_ENTITY -> builder.entity(EntityTarget.THIS, entity(random));
        case ATTACKER -> builder.entity(EntityTarget.ATTACKER, entity(random));
        case DIRECT_ATTACKER -> builder.entity(EntityTarget.DIRECT_ATTACKER, entity(random));
        case ATTACKING_PLAYER ->
            builder.entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"));
        case TARGET_ENTITY -> builder.entity(EntityTarget.TARGET_ENTITY, entity(random));
        case INTERACTING_ENTITY -> builder.entity(EntityTarget.INTERACTING_ENTITY, entity(random));
        case DAMAGE_SOURCE ->
            builder.damageSource(
                new DamageSourceData(
                    Identifier.minecraft(random.nextBoolean() ? "arrow" : "player_attack"),
                    random.nextBoolean()));
        case TOOL -> builder.tool(tool(random));
        case BLOCK_STATE ->
            builder.blockState(
                switch (random.nextInt(3)) {
                  case 0 -> Blocks.stone();
                  case 1 -> Blocks.wheat(random.nextInt(8));
                  default -> Blocks.pot(random.nextBoolean());
                });
        case EXPLOSION_RADIUS ->
            builder.explosionRadius(new float[] {0f, 1f, 4f, 0.25f, -1f}[random.nextInt(5)]);
        case ENCHANTMENT_LEVEL -> builder.enchantment(random.nextInt(300), random.nextBoolean());
        default -> {}
      }
    }
    builder.level(new LevelFacts(random.nextBoolean(), random.nextBoolean(), 5, random.nextInt()));
    return builder.build();
  }

  private static FakeEntity entity(Random random) {
    FakeEntity e = FakeEntity.of(ENTITIES[random.nextInt(ENTITIES.length)]);
    if (random.nextBoolean()) {
      e.wearing(EquipmentSlot.MAINHAND, tool(random));
    }
    return e;
  }

  private static ItemStack tool(Random random) {
    ItemStack stack = LootItems.stack(TOOLS[random.nextInt(TOOLS.length)]);
    Map<String, Integer> levels = new LinkedHashMap<>();
    for (String e : ENCHANTS) {
      if (random.nextInt(3) == 0) {
        levels.put(e, random.nextInt(6));
      }
    }
    return levels.isEmpty() ? stack : LootItems.enchanted(stack, levels);
  }

  private static void assertValid(String where, LootResult result) {
    for (ItemStack stack : result.stacks()) {
      assertThat(stack.isEmpty()).as(where).isFalse();
      assertThat(stack.count()).as(where + " " + stack).isBetween(1, stack.maxStackSize());
    }
  }

  @Test
  void provisionalEveryRealTableSurvivesRandomContextsOfEveryType() {
    LootData data = RealData.data();
    Random random = new Random(20261029L);
    List<LootContextType> types = LootContextTypes.all();
    int draws = 0;
    int nonEmpty = 0;
    for (Map.Entry<Identifier, LootTable> entry : data.tables().tables().entrySet()) {
      for (int i = 0; i < 31 * 8; i++) {
        LootContextType type = types.get(i % 31);
        long seed = random.nextLong();
        long ctxSeed = random.nextLong();
        String where = entry.getKey() + " in " + type.id() + " seed " + seed + " ctx " + ctxSeed;
        LootResult first;
        LootResult second;
        try {
          first =
              LootEvaluator.evaluate(
                  entry.getKey(), context(data, type, new Random(ctxSeed), seed));
          second =
              LootEvaluator.evaluate(
                  entry.getKey(), context(data, type, new Random(ctxSeed), seed));
        } catch (RuntimeException | StackOverflowError e) {
          throw new AssertionError(where, e);
        }
        assertValid(where, first);
        assertThat(second.stacks()).as(where).isEqualTo(first.stacks());
        draws++;
        if (!first.stacks().isEmpty()) {
          nonEmpty++;
        }
      }
    }
    System.out.println("HUNT draws=" + draws + " nonEmpty=" + nonEmpty);
    assertThat(draws).isEqualTo(1447 * 31 * 8);
  }

  @Test
  void provisionalUnpackingKeepsEveryStackOfTheTableOnce() {
    LootData data = RealData.data();
    Random random = new Random(77L);
    List<LootContextType> types = LootContextTypes.all();
    for (Map.Entry<Identifier, LootTable> entry : data.tables().tables().entrySet()) {
      long seed = random.nextLong() | 1L;
      long ctxSeed = random.nextLong();
      LootContextType type = types.get(random.nextInt(31));
      String where = entry.getKey() + " " + seed;
      LootResult plain =
          LootEvaluator.evaluateTopLevel(
              entry.getValue(),
              context(data, type, new Random(ctxSeed), 1L),
              seed,
              new SequenceStore(5));
      UnpackResult unpacked =
          ContainerUnpacker.unpack(
              new LootBinding.Bound(entry.getKey(), true, seed),
              Math.max(54, plain.stacks().size()),
              context(data, type, new Random(ctxSeed), 1L),
              new SequenceStore(5));
      List<String> a = new ArrayList<>(plain.stacks().stream().map(Object::toString).toList());
      List<String> b =
          new ArrayList<>(
              unpacked.slots().stream().filter(s -> !s.isEmpty()).map(Object::toString).toList());
      a.sort(null);
      b.sort(null);
      assertThat(b).as(where).isEqualTo(a);
    }
  }
}
