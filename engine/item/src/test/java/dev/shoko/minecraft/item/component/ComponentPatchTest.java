package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R14, IC-R17, IC-R18: the modifications a stack carries over
// the prototype of its item, and their normal form.
class ComponentPatchTest {

  private static final Identifier RARITY = Identifier.minecraft("rarity");
  private static final Identifier LORE = Identifier.minecraft("lore");
  private static final Identifier ITEM_MODEL = Identifier.minecraft("item_model");

  private static ComponentValue text(String value) {
    return new ComponentValue(new StringTag(value));
  }

  private static ComponentMap prototype() {
    SequencedMap<Identifier, ComponentValue> entries = new LinkedHashMap<>();
    entries.put(RARITY, text("common"));
    entries.put(ITEM_MODEL, text("minecraft:stone"));
    return new ComponentMap(entries);
  }

  @Test
  void theEmptyPatchChangesNothing() {
    // spec: IC-R14
    assertThat(ComponentPatch.empty().isEmpty()).isTrue();
    assertThat(ComponentPatch.empty().changes()).isEmpty();
  }

  @Test
  void aSetIsRecordedAsASetChange() {
    // spec: IC-R14
    ComponentPatch patch = ComponentPatch.empty().withSet(RARITY, text("epic"));
    assertThat(patch.isEmpty()).isFalse();
    assertThat(patch.changes()).containsOnlyKeys(RARITY);
    assertThat(patch.changes().get(RARITY)).isEqualTo(new ComponentChange.Set(text("epic")));
  }

  @Test
  void aRemovalIsRecordedAsARemovedChange() {
    // spec: IC-R14, IC-R27
    ComponentPatch patch = ComponentPatch.empty().withRemoved(LORE);
    assertThat(patch.isEmpty()).isFalse();
    assertThat(patch.changes().get(LORE)).isEqualTo(new ComponentChange.Removed());
  }

  @Test
  void aLaterChangeReplacesAnEarlierOneOfTheSameComponent() {
    // spec: ComponentPatch.withSet, withRemoved
    ComponentPatch setThenRemoved =
        ComponentPatch.empty().withSet(RARITY, text("epic")).withRemoved(RARITY);
    assertThat(setThenRemoved.changes()).hasSize(1);
    assertThat(setThenRemoved.changes().get(RARITY)).isEqualTo(new ComponentChange.Removed());
    ComponentPatch removedThenSet = setThenRemoved.withSet(RARITY, text("rare"));
    assertThat(removedThenSet.changes()).hasSize(1);
    assertThat(removedThenSet.changes().get(RARITY))
        .isEqualTo(new ComponentChange.Set(text("rare")));
  }

  @Test
  void changesKeepTheirInsertionOrder() {
    // spec: ComponentPatch (insertion order)
    ComponentPatch patch =
        ComponentPatch.empty()
            .withSet(ITEM_MODEL, text("minecraft:dirt"))
            .withRemoved(LORE)
            .withSet(RARITY, text("epic"));
    assertThat(patch.changes().keySet()).containsExactly(ITEM_MODEL, LORE, RARITY);
  }

  @Test
  void aPatchIsImmutable() {
    // spec: ComponentPatch (immutable)
    ComponentPatch base = ComponentPatch.empty();
    ComponentPatch changed = base.withSet(RARITY, text("epic"));
    assertThat(base.isEmpty()).isTrue();
    assertThat(changed.isEmpty()).isFalse();
  }

  @Test
  void provisionalASetEqualToThePrototypeIsDroppedByNormalization() {
    // spec: IC-R17 (provisional)
    ComponentPatch patch = ComponentPatch.empty().withSet(RARITY, text("common"));
    assertThat(patch.normalizedAgainst(prototype()).isEmpty()).isTrue();
  }

  @Test
  void provisionalASetDifferentFromThePrototypeIsKept() {
    // spec: IC-R17 (provisional)
    ComponentPatch patch = ComponentPatch.empty().withSet(RARITY, text("epic"));
    assertThat(patch.normalizedAgainst(prototype())).isEqualTo(patch);
  }

  @Test
  void provisionalASetOfAComponentTheItemLacksIsKept() {
    // spec: IC-R17, IC-R14 (provisional)
    ComponentPatch patch = ComponentPatch.empty().withSet(LORE, text("a line"));
    assertThat(patch.normalizedAgainst(prototype())).isEqualTo(patch);
  }

  @Test
  void provisionalARemovalOfAComponentTheItemLacksIsDropped() {
    // spec: IC-R18 (a removed component and an absent one are equivalent), IC-R17
    ComponentPatch patch = ComponentPatch.empty().withRemoved(LORE);
    assertThat(patch.normalizedAgainst(prototype()).isEmpty()).isTrue();
  }

  @Test
  void provisionalARemovalOfAComponentOfThePrototypeIsKept() {
    // spec: IC-R14, IC-R27 (provisional)
    ComponentPatch patch = ComponentPatch.empty().withRemoved(RARITY);
    assertThat(patch.normalizedAgainst(prototype())).isEqualTo(patch);
  }

  @Test
  void provisionalNormalizationKeepsOnlyTheUsefulChangesInOrder() {
    // spec: IC-R17 (provisional)
    ComponentPatch patch =
        ComponentPatch.empty()
            .withSet(ITEM_MODEL, text("minecraft:dirt"))
            .withSet(RARITY, text("common"))
            .withRemoved(LORE);
    ComponentPatch normalized = patch.normalizedAgainst(prototype());
    assertThat(normalized.changes().keySet()).containsExactly(ITEM_MODEL);
    assertThat(normalized.changes().get(ITEM_MODEL))
        .isEqualTo(new ComponentChange.Set(text("minecraft:dirt")));
  }

  @Test
  void provisionalTwoPatchesWithTheSameEffectNormalizeToEqualPatches() {
    // spec: IC-R17, IC-R18 (provisional): same effective components, same normal form
    ComponentPatch redundant =
        ComponentPatch.empty().withSet(RARITY, text("common")).withRemoved(LORE);
    assertThat(redundant.normalizedAgainst(prototype()))
        .isEqualTo(ComponentPatch.empty().normalizedAgainst(prototype()));
  }

  @Test
  void provisionalNormalizationDoesNotChangeTheEffectiveComponents() {
    // spec: IC-R17 (provisional)
    ComponentPatch patch =
        ComponentPatch.empty()
            .withSet(RARITY, text("common"))
            .withSet(ITEM_MODEL, text("minecraft:dirt"))
            .withRemoved(LORE);
    assertThat(prototype().apply(patch.normalizedAgainst(prototype())))
        .isEqualTo(prototype().apply(patch));
  }

  @Test
  void provisionalNormalizationComparesFloatsByBits() {
    // spec: IC-R17 (provisional): "bit for bit"; -0.0 is not 0.0
    Identifier key = Identifier.minecraft("repair_cost");
    SequencedMap<Identifier, ComponentValue> entries = new LinkedHashMap<>();
    entries.put(key, new ComponentValue(FloatTag.of(0.0f)));
    ComponentMap floatPrototype = new ComponentMap(entries);
    ComponentPatch same =
        ComponentPatch.empty().withSet(key, new ComponentValue(FloatTag.of(0.0f)));
    ComponentPatch negative =
        ComponentPatch.empty().withSet(key, new ComponentValue(FloatTag.of(-0.0f)));
    assertThat(same.normalizedAgainst(floatPrototype).isEmpty()).isTrue();
    assertThat(negative.normalizedAgainst(floatPrototype)).isEqualTo(negative);
  }
}
