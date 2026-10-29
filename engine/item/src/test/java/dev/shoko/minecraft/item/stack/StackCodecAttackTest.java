package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R24 to IC-R29 (adversarial): fixed seeds, mutated NBT, JSON
// and command text. Only Decoding results are accepted (no exception), a failure carries an
// ERROR finding, a success none, and what is accepted writes and reads back.
class StackCodecAttackTest {

  private static final int ROUNDS = 1500;

  private static final List<String> IDS =
      List.of(
          "minecraft:stone",
          "minecraft:potion",
          "minecraft:stone_pickaxe",
          "minecraft:inconnu",
          "Bad Id",
          "",
          "minecraft:",
          "minecraft:air");

  private static final List<String> COMPONENT_KEYS =
      List.of(
          "minecraft:rarity",
          "!minecraft:rarity",
          "minecraft:damage",
          "minecraft:enchantments",
          "minecraft:inconnu",
          "!minecraft:inconnu",
          "minecraft:food",
          "minecraft:potion_contents");

  private static StackCodec codec() {
    return TestItems.stackCodec();
  }

  private static List<ItemStack> pool() {
    return List.of(
        TestItems.stack("stone", 3),
        StackSupport.withRarity("stone", 64, "epic"),
        TestItems.stack("ender_pearl", 16),
        TestItems.stack("stone_pickaxe", 1, "damage", "17"),
        TestItems.stack("bread", 2, "food", "{'nutrition':1,'saturation':-0.0}"),
        TestItems.stack("potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"),
        TestItems.stack("stone", 5, "enchantments", "{'minecraft:sharpness':3}"),
        TestItems.stack("stone", 7).withoutComponent(StackSupport.rarityId()));
  }

  private static boolean isError(Finding finding) {
    return finding.code().severity() == Severity.ERROR;
  }

  /** The invariants of every decoding, returning the stack when it was accepted. */
  private static ItemStack checked(Decoding<ItemStack> decoding, String input) {
    if (decoding instanceof Decoding.Failure<ItemStack> failure) {
      assertThat(failure.findings()).as(input).anyMatch(StackCodecAttackTest::isError);
      return null;
    }
    Decoding.Success<ItemStack> success = (Decoding.Success<ItemStack>) decoding;
    assertThat(success.findings()).as(input).noneMatch(StackCodecAttackTest::isError);
    assertThat(success.value()).as(input).isNotNull();
    return success.value();
  }

  private static boolean inBounds(ItemStack stack) {
    return !stack.isEmpty() && stack.count() <= stack.maxStackSize();
  }

  private static NbtTag randomTag(Random random) {
    return switch (random.nextInt(10)) {
      case 0 -> new ByteTag((byte) (random.nextInt(8) - 2));
      case 1 -> new ShortTag((short) (random.nextInt(80) - 5));
      case 2 -> new IntTag(random.nextInt(75) - 5);
      case 3 -> new LongTag(random.nextInt(75) - 5);
      case 4 -> FloatTag.of(random.nextFloat());
      case 5 -> DoubleTag.of(random.nextDouble());
      case 6 -> new StringTag(IDS.get(random.nextInt(IDS.size())));
      case 7 -> ListTag.empty();
      case 8 -> CompoundTag.empty();
      default -> IntArrayTag.of(1, 2);
    };
  }

  private static CompoundTag mutate(Random random, CompoundTag base) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>(base.entries());
    int edits = 1 + random.nextInt(3);
    for (int i = 0; i < edits; i++) {
      List<String> keys = new ArrayList<>(entries.keySet());
      switch (random.nextInt(6)) {
        case 0 -> {
          if (!keys.isEmpty()) {
            entries.remove(keys.get(random.nextInt(keys.size())));
          }
        }
        case 1 -> {
          if (!keys.isEmpty()) {
            entries.put(keys.get(random.nextInt(keys.size())), randomTag(random));
          }
        }
        case 2 ->
            entries.put(
                List.of("id", "count", "Slot", "components", "x").get(random.nextInt(5)),
                randomTag(random));
        case 3 -> entries.put("id", new StringTag(IDS.get(random.nextInt(IDS.size()))));
        case 4 -> entries.put("components", mutateComponents(random, entries.get("components")));
        default -> entries.put("count", new IntTag(random.nextInt(130) - 10));
      }
    }
    return CompoundTag.of(entries);
  }

  private static NbtTag mutateComponents(Random random, NbtTag existing) {
    SequencedMap<String, NbtTag> components = new LinkedHashMap<>();
    if (existing instanceof CompoundTag compound) {
      components.putAll(compound.entries());
    }
    components.put(COMPONENT_KEYS.get(random.nextInt(COMPONENT_KEYS.size())), randomTag(random));
    return CompoundTag.of(components);
  }

  @Test
  @Timeout(60)
  void provisionalMutatedNbtGivesATypedOutcomeAndAcceptedStacksRoundTrip() {
    // spec: IC-R24 to IC-R27
    Random random = new Random(0x57ACC0DEL);
    StackCodec codec = codec();
    List<ItemStack> pool = pool();
    for (int i = 0; i < ROUNDS; i++) {
      CompoundTag input = mutate(random, codec.toNbt(pool.get(random.nextInt(pool.size()))));
      ItemStack accepted = checked(codec.fromNbt(input), "round " + i + " " + input.entries());
      if (accepted != null && inBounds(accepted)) {
        ItemStack back = checked(codec.fromNbt(codec.toNbt(accepted)), "reread " + i);
        assertThat(back).as("round %d", i).isEqualTo(accepted);
      }
    }
  }

  private static String randomJsonValue(Random random, int depth) {
    return switch (random.nextInt(depth > 2 ? 6 : 8)) {
      case 0 -> "\"" + IDS.get(random.nextInt(IDS.size())) + "\"";
      case 1 -> Integer.toString(random.nextInt(130) - 10);
      case 2 -> Double.toString(random.nextInt(100) / 8.0);
      case 3 -> "true";
      case 4 -> "null";
      case 5 -> "\"epic\"";
      case 6 -> "[" + randomJsonValue(random, depth + 1) + "]";
      default -> randomJsonObject(random, depth + 1);
    };
  }

  private static String randomJsonObject(Random random, int depth) {
    StringBuilder text = new StringBuilder("{");
    int members = random.nextInt(4);
    for (int i = 0; i < members; i++) {
      if (i > 0) {
        text.append(',');
      }
      String key =
          random.nextBoolean()
              ? List.of("id", "count", "components", "x").get(random.nextInt(4))
              : COMPONENT_KEYS.get(random.nextInt(COMPONENT_KEYS.size()));
      text.append('"').append(key).append("\":").append(randomJsonValue(random, depth));
    }
    return text.append('}').toString();
  }

  private static String stackLikeJson(Random random) {
    String id = "\"" + IDS.get(random.nextInt(IDS.size())) + "\"";
    String count = random.nextInt(4) == 0 ? "" : ",\"count\":" + (random.nextInt(70) - 3);
    String components =
        random.nextBoolean() ? "" : ",\"components\":" + randomJsonObject(random, 1);
    return "{\"id\":" + id + count + components + "}";
  }

  @Test
  @Timeout(60)
  void provisionalRandomJsonGivesATypedOutcomeAndAcceptedStacksRoundTrip() {
    // spec: IC-R28
    Random random = new Random(0x15011L);
    StackCodec codec = codec();
    for (int i = 0; i < ROUNDS; i++) {
      String text = random.nextInt(5) == 0 ? randomJsonObject(random, 0) : stackLikeJson(random);
      ItemStack accepted = checked(codec.fromJson(Json.parseStrict(text)), text);
      if (accepted != null && inBounds(accepted)) {
        String written = codec.toJson(accepted);
        ItemStack back = checked(codec.fromJson(Json.parseStrict(written)), written);
        assertThat(back).as(text).isEqualTo(accepted);
      }
    }
  }

  private static String mutateText(Random random, String seed) {
    String alphabet = "[]{}=,!\"' :#abz019_./-";
    StringBuilder text = new StringBuilder(seed);
    int edits = 1 + random.nextInt(3);
    for (int i = 0; i < edits; i++) {
      char c = alphabet.charAt(random.nextInt(alphabet.length()));
      int at = text.isEmpty() ? 0 : random.nextInt(text.length());
      switch (random.nextInt(3)) {
        case 0 -> text.insert(at, c);
        case 1 -> {
          if (!text.isEmpty()) {
            text.deleteCharAt(at);
          }
        }
        default -> {
          if (!text.isEmpty()) {
            text.setCharAt(at, c);
          }
        }
      }
    }
    return text.toString();
  }

  @Test
  @Timeout(60)
  void provisionalMutatedCommandTextGivesATypedOutcomeAndAcceptedStacksRoundTrip() {
    // spec: IC-R29
    Random random = new Random(0x7E87L);
    StackCodec codec = codec();
    List<String> seeds =
        List.of(
            "minecraft:stone",
            "minecraft:stone[minecraft:rarity=\"epic\"]",
            "minecraft:stone[minecraft:rarity=\"epic\",!minecraft:lore]",
            "minecraft:stone_pickaxe[minecraft:damage=5]",
            "minecraft:stone[!minecraft:rarity]");
    for (int i = 0; i < ROUNDS; i++) {
      String text = mutateText(random, seeds.get(random.nextInt(seeds.size())));
      ItemStack accepted = checked(codec.fromCommandText(text), text);
      if (accepted != null && !accepted.isEmpty()) {
        String written = codec.toCommandText(accepted);
        ItemStack back = checked(codec.fromCommandText(written), written);
        assertThat(back).as("%s -> %s", text, written).isNotNull();
        assertThat(back.sameContent(accepted)).as("%s -> %s", text, written).isTrue();
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalEveryEncodedFormReadsBackToTheSameStack() {
    // spec: IC-R24 to IC-R29
    Random random = new Random(0xE4C0DEDL);
    StackCodec codec = codec();
    String[] rarities = {"common", "uncommon", "rare", "epic"};
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack stack =
          TestItems.stack("stone_pickaxe", 1, "damage", Integer.toString(random.nextInt(131)))
              .withComponent(
                  StackSupport.rarityId(), StackSupport.text(rarities[random.nextInt(4)]));
      if (random.nextBoolean()) {
        stack = stack.withoutComponent(TestItems.id("lore"));
      }
      ItemStack viaNbt = checked(codec.fromNbt(codec.toNbt(stack)), "nbt " + stack);
      ItemStack viaJson =
          checked(codec.fromJson(Json.parseStrict(codec.toJson(stack))), "json " + stack);
      ItemStack viaText =
          checked(codec.fromCommandText(codec.toCommandText(stack)), "text " + stack);
      assertThat(viaNbt).isEqualTo(stack);
      assertThat(viaJson).isEqualTo(stack);
      assertThat(viaText.sameContent(stack)).as("text of %s", stack).isTrue();
    }
  }
}
