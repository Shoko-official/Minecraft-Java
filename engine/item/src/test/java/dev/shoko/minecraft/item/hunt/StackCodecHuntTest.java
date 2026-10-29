package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class StackCodecHuntTest {

  private static final ComponentCodec CODEC = TestItems.componentCodec();
  private static final StackCodec STACKS = TestItems.stackCodec();

  @Test
  void nbtCountIsBoundedLikeJsonCount() {
    CompoundTag tag = Nbt.compound("{id:\"minecraft:stone\",count:100000}");
    Decoding<ItemStack> decoded = STACKS.fromNbt(tag);
    ItemStack stack =
        decoded instanceof Decoding.Success<ItemStack> s ? s.value() : ItemStack.empty();
    assertThat(stack.count()).isLessThanOrEqualTo(ItemStack.MAX_COUNT);
  }

  @Test
  void nanPayloadOfATypedFloatSurvivesNbtDecoding() {
    FloatTag signaling = new FloatTag(0x7f800001);
    LinkedHashMap<String, dev.shoko.minecraft.core.nbt.NbtTag> entries = new LinkedHashMap<>();
    entries.put("nutrition", new IntTag(1));
    entries.put("saturation", signaling);
    ComponentValue decoded =
        Findings.value(CODEC.valueFromNbt(TestItems.id("food"), CompoundTag.of(entries)));
    assertThat(((CompoundTag) decoded.tag()).get("saturation").orElseThrow()).isEqualTo(signaling);
  }

  @Test
  void wearNeverMakesDamageNegative() {
    ItemStack worn = TestItems.stack("stone_pickaxe", 1).wornBy(-5);
    assertThat(worn.damage()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void duplicateIdentifierKeysAfterNormalizationAreNotSilentlyMerged() {
    Decoding<ComponentValue> decoded =
        CODEC.valueFromJson(
            Identifier.minecraft("enchantments"),
            Json.parseStrict("{\"sharpness\":1,\"minecraft:sharpness\":5}"));
    assertThat(decoded).isInstanceOf(Decoding.Failure.class);
  }

  @Test
  void theCodecRefusesWhatTheEnchantmentViewRefuses() {
    ComponentValue value =
        Findings.value(
            CODEC.valueFromJson(
                TestItems.id("enchantments"), Json.parseStrict("{\"minecraft:sharpness\":256}")));
    assertThat(dev.shoko.minecraft.item.component.EnchantmentLevels.from(value))
        .as("the typed view refuses a level above 255")
        .isInstanceOf(Decoding.Failure.class);
    assertThat(
            CODEC.valueFromJson(
                TestItems.id("enchantments"), Json.parseStrict("{\"minecraft:sharpness\":256}")))
        .as("the decoder must refuse it too (IC-R22)")
        .isInstanceOf(Decoding.Failure.class);
  }

  @Test
  void aNegativeDamageIsRefusedAtDecoding() {
    assertThat(CODEC.valueFromJson(TestItems.id("damage"), Json.parseStrict("-1")))
        .as("damage lies in 0..max_damage (IC-R21)")
        .isInstanceOf(Decoding.Failure.class);
  }
}
