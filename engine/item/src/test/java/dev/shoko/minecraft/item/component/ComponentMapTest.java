package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R6b, IC-R14, IC-R18: component maps and the application of a
// patch over a prototype.
class ComponentMapTest {

  private static final Identifier RARITY = Identifier.minecraft("rarity");
  private static final Identifier LORE = Identifier.minecraft("lore");
  private static final Identifier CONSUMABLE = Identifier.minecraft("consumable");

  private static ComponentValue text(String value) {
    return new ComponentValue(new StringTag(value));
  }

  private static ComponentValue emptyObject() {
    return new ComponentValue(CompoundTag.empty());
  }

  private static ComponentMap mapOf(Map<Identifier, ComponentValue> entries) {
    return new ComponentMap(new LinkedHashMap<>(entries));
  }

  private static ComponentPatch patchOf(Map<Identifier, ComponentChange> changes) {
    return new ComponentPatch(new LinkedHashMap<>(changes));
  }

  @Test
  void theEmptyMapHasNoComponent() {
    // spec: IC-R14
    ComponentMap empty = ComponentMap.empty();
    assertThat(empty.entries()).isEmpty();
    assertThat(empty.contains(RARITY)).isFalse();
    assertThat(empty.get(RARITY)).isEmpty();
  }

  @Test
  void aPresentComponentIsReadBackAndContained() {
    // spec: IC-R14
    ComponentMap map = mapOf(Map.of(RARITY, text("epic")));
    assertThat(map.contains(RARITY)).isTrue();
    assertThat(map.get(RARITY)).contains(text("epic"));
    assertThat(map.get(LORE)).isEmpty();
  }

  @Test
  void provisionalAnEmptyObjectIsPresentAndNotAbsent() {
    // spec: IC-R6b (provisional): {} is significant
    ComponentMap map = mapOf(Map.of(CONSUMABLE, emptyObject()));
    assertThat(map.contains(CONSUMABLE)).isTrue();
    assertThat(map.get(CONSUMABLE)).contains(emptyObject());
    assertThat(map).isNotEqualTo(ComponentMap.empty());
  }

  @Test
  void theEqualityOfMapsIgnoresTheOrderOfEntries() {
    // spec: ComponentMap (equality ignores the order)
    Map<Identifier, ComponentValue> forward = new LinkedHashMap<>();
    forward.put(RARITY, text("epic"));
    forward.put(LORE, text("x"));
    Map<Identifier, ComponentValue> backward = new LinkedHashMap<>();
    backward.put(LORE, text("x"));
    backward.put(RARITY, text("epic"));
    assertThat(mapOf(forward)).isEqualTo(mapOf(backward));
    assertThat(mapOf(forward).hashCode()).isEqualTo(mapOf(backward).hashCode());
  }

  @Test
  void provisionalApplyingTheEmptyPatchChangesNothing() {
    // spec: IC-R14
    ComponentMap map = mapOf(Map.of(RARITY, text("common")));
    assertThat(map.apply(patchOf(Map.of()))).isEqualTo(map);
  }

  @Test
  void provisionalASetAddsAComponent() {
    // spec: IC-R14
    ComponentMap map = mapOf(Map.of(RARITY, text("common")));
    ComponentMap result = map.apply(patchOf(Map.of(LORE, new ComponentChange.Set(text("a line")))));
    assertThat(result.get(LORE)).contains(text("a line"));
    assertThat(result.get(RARITY)).contains(text("common"));
  }

  @Test
  void provisionalASetReplacesAComponent() {
    // spec: IC-R14
    ComponentMap map = mapOf(Map.of(RARITY, text("common")));
    ComponentMap result = map.apply(patchOf(Map.of(RARITY, new ComponentChange.Set(text("epic")))));
    assertThat(result.get(RARITY)).contains(text("epic"));
    assertThat(result.entries()).hasSize(1);
  }

  @Test
  void provisionalARemovalDropsAComponentOfThePrototype() {
    // spec: IC-R14, IC-R27
    ComponentMap map = mapOf(Map.of(RARITY, text("common"), LORE, text("x")));
    ComponentMap result = map.apply(patchOf(Map.of(LORE, new ComponentChange.Removed())));
    assertThat(result.contains(LORE)).isFalse();
    assertThat(result.get(RARITY)).contains(text("common"));
  }

  @Test
  void provisionalARemovalOfAnAbsentComponentChangesNothing() {
    // spec: IC-R18 (a removed component and an absent one are equivalent)
    ComponentMap map = mapOf(Map.of(RARITY, text("common")));
    assertThat(map.apply(patchOf(Map.of(LORE, new ComponentChange.Removed())))).isEqualTo(map);
  }

  @Test
  void provisionalApplyingAPatchDoesNotModifyTheOriginalMap() {
    // spec: ComponentMap (immutable)
    ComponentMap map = mapOf(Map.of(RARITY, text("common")));
    ComponentMap changed =
        map.apply(patchOf(Map.of(RARITY, new ComponentChange.Set(text("epic")))));
    assertThat(changed.get(RARITY)).contains(text("epic"));
    assertThat(map.get(RARITY)).contains(text("common"));
  }

  @Test
  void aFloatIsComparedByItsBits() {
    // spec: IC-R18, core-nbt R37: -0.0 differs from 0.0
    ComponentValue zero = new ComponentValue(FloatTag.of(0.0f));
    ComponentValue negativeZero = new ComponentValue(FloatTag.of(-0.0f));
    assertThat(zero).isNotEqualTo(negativeZero);
    assertThat(mapOf(Map.of(RARITY, zero))).isNotEqualTo(mapOf(Map.of(RARITY, negativeZero)));
  }

  @Test
  void provisionalASetThenARemovalOfDifferentComponentsApplyIndependently() {
    // spec: IC-R14
    ComponentMap map = mapOf(Map.of(RARITY, text("common"), LORE, text("x")));
    Map<Identifier, ComponentChange> changes = new LinkedHashMap<>();
    changes.put(RARITY, new ComponentChange.Set(text("rare")));
    changes.put(LORE, new ComponentChange.Removed());
    ComponentMap result = map.apply(patchOf(changes));
    assertThat(result.entries()).hasSize(1);
    assertThat(result.get(RARITY)).contains(text("rare"));
  }
}
