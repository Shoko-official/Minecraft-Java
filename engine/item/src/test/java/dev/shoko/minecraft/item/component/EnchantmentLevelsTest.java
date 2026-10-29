package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Nbt;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R22: enchantments and stored_enchantments are a dictionary of
// enchantment identifier to level, from 1 to 255 (the range is provisional).
class EnchantmentLevelsTest {

  private static final Identifier SHARPNESS = Identifier.minecraft("sharpness");
  private static final Identifier UNBREAKING = Identifier.minecraft("unbreaking");

  private static ComponentValue compound(String snbt) {
    return new ComponentValue(Nbt.compound(snbt));
  }

  private static EnchantmentLevels levels(Object... idsAndLevels) {
    Map<Identifier, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < idsAndLevels.length; i += 2) {
      map.put((Identifier) idsAndLevels[i], (Integer) idsAndLevels[i + 1]);
    }
    return new EnchantmentLevels(new LinkedHashMap<>(map));
  }

  @Test
  void theFormOfTheModifiersIsADictionaryOfLevels() {
    // spec: IC-R22 (D form {"minecraft:sharpness": 1})
    EnchantmentLevels read =
        Findings.value(EnchantmentLevels.from(compound("{\"minecraft:sharpness\":1}")));
    assertThat(read.levels()).containsExactly(Map.entry(SHARPNESS, 1));
    assertThat(read.levelOf(SHARPNESS)).isEqualTo(1);
  }

  @Test
  void theOrderOfTheEntriesIsKept() {
    // spec: IC-R22 (insertion order)
    EnchantmentLevels read =
        Findings.value(
            EnchantmentLevels.from(
                compound("{\"minecraft:unbreaking\":3,\"minecraft:sharpness\":5}")));
    assertThat(read.levels().keySet()).containsExactly(UNBREAKING, SHARPNESS);
  }

  @Test
  void anAbsentEnchantmentHasLevelZero() {
    // spec: EnchantmentLevels.levelOf
    EnchantmentLevels read =
        Findings.value(EnchantmentLevels.from(compound("{\"minecraft:sharpness\":2}")));
    assertThat(read.levelOf(UNBREAKING)).isZero();
  }

  @Test
  void anEmptyDictionaryHasNoLevel() {
    // spec: IC-R22, IC-R4 (enchantments {})
    EnchantmentLevels read = Findings.value(EnchantmentLevels.from(compound("{}")));
    assertThat(read.levels()).isEmpty();
  }

  @Test
  void provisionalTheBoundsOfTheRangeAreAccepted() {
    // spec: IC-R22 (provisional): levels 1 to 255
    EnchantmentLevels read =
        Findings.value(
            EnchantmentLevels.from(
                compound("{\"minecraft:sharpness\":1,\"minecraft:unbreaking\":255}")));
    assertThat(read.levelOf(SHARPNESS)).isEqualTo(1);
    assertThat(read.levelOf(UNBREAKING)).isEqualTo(255);
  }

  @Test
  void provisionalALevelBelowOneIsDroppedWithoutAFinding() {
    // spec: IC-R22 (provisional): "un niveau inferieur a 1 retire l'entree"
    var decoding =
        EnchantmentLevels.from(
            compound(
                "{\"minecraft:sharpness\":0,\"minecraft:unbreaking\":-3,\"minecraft:fire_aspect\":2}"));
    EnchantmentLevels read = Findings.value(decoding);
    assertThat(decoding.findings()).isEmpty();
    assertThat(read.levels().keySet()).containsExactly(Identifier.minecraft("fire_aspect"));
  }

  @Test
  void provisionalALevelAbove255IsARangeFailure() {
    // spec: IC-R22 (provisional), EnchantmentLevels.from
    Findings.assertFailure(
        EnchantmentLevels.from(compound("{\"minecraft:sharpness\":256}")), FindingCode.VALUE_RANGE);
  }

  @Test
  void provisionalAValueThatIsNotACompoundIsRefused() {
    // spec: IC-R22 (provisional): "a compound of identifiers to integers"
    Findings.assertFailure(
        EnchantmentLevels.from(new ComponentValue(new StringTag("sharpness"))),
        FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalALevelThatIsNotAnIntegerIsRefused() {
    // spec: IC-R22 (provisional)
    Findings.assertFailure(
        EnchantmentLevels.from(compound("{\"minecraft:sharpness\":\"high\"}")),
        FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAKeyThatIsNotAnIdentifierIsRefused() {
    // spec: IC-R22 (provisional)
    Findings.assertFailure(
        EnchantmentLevels.from(compound("{\"Not An Id\":1}")), FindingCode.INVALID_IDENTIFIER);
  }

  @Test
  void settingAnEnchantmentAddsOrReplacesIt() {
    // spec: IC-R22
    EnchantmentLevels start = levels(SHARPNESS, 2);
    assertThat(start.with(UNBREAKING, 3).levels())
        .containsExactly(Map.entry(SHARPNESS, 2), Map.entry(UNBREAKING, 3));
    assertThat(start.with(SHARPNESS, 5).levels()).containsExactly(Map.entry(SHARPNESS, 5));
    assertThat(start.levels()).containsExactly(Map.entry(SHARPNESS, 2));
  }

  @Test
  void provisionalSettingALevelBelowOneRemovesTheEntry() {
    // spec: IC-R22 (provisional)
    EnchantmentLevels start = levels(SHARPNESS, 2, UNBREAKING, 3);
    assertThat(start.with(SHARPNESS, 0).levels()).containsExactly(Map.entry(UNBREAKING, 3));
    assertThat(start.with(UNBREAKING, -1).levels()).containsExactly(Map.entry(SHARPNESS, 2));
    assertThat(start.with(Identifier.minecraft("mending"), 0).levels())
        .containsExactly(Map.entry(SHARPNESS, 2), Map.entry(UNBREAKING, 3));
  }

  @Test
  void theValueIsACompoundInTheOrderOfTheLevels() {
    // spec: IC-R22, EnchantmentLevels.toValue
    ComponentValue value = levels(UNBREAKING, 3, SHARPNESS, 5).toValue();
    assertThat(value.tag()).isInstanceOf(CompoundTag.class);
    assertThat(((CompoundTag) value.tag()).entries().keySet())
        .containsExactly("minecraft:unbreaking", "minecraft:sharpness");
  }

  @Test
  void theValueReadsBackToTheSameLevels() {
    // spec: IC-R22
    EnchantmentLevels original = levels(UNBREAKING, 3, SHARPNESS, 255);
    assertThat(Findings.value(EnchantmentLevels.from(original.toValue()))).isEqualTo(original);
  }

  @Test
  void noEnchantmentGivesTheEmptyCompound() {
    // spec: IC-R4 (enchantments {}), IC-R6b
    assertThat(levels().toValue()).isEqualTo(new ComponentValue(CompoundTag.empty()));
  }
}
