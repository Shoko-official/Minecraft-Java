package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

class BlockStateTest {

  private static BlockStateRegistry registry() {
    return Blocks.registry();
  }

  @Test
  void stateKnowsItsBlockAndIdentifier() {
    // spec: BS-R5 (a state is a block and a global identifier)
    BlockState state = registry().state(26);

    assertThat(state.id()).isEqualTo(26);
    assertThat(state.block().id()).isEqualTo(Blocks.CHEST);
  }

  @Test
  void valueReadsAProperty() {
    // spec: BS-R6
    BlockState state = registry().state(26);

    assertThat(state.value("facing")).isEqualTo("north");
    assertThat(state.value("type")).isEqualTo("left");
    assertThat(state.value("waterlogged")).isEqualTo("true");
  }

  @Test
  void valueOfAnUnknownPropertyIsRejected() {
    // spec: BlockState.value contract (IllegalArgumentException)
    BlockState stone = registry().state(1);

    Failures.expect(IllegalArgumentException.class, () -> stone.value("axis"));
    Failures.expect(IllegalArgumentException.class, () -> registry().state(26).value("axis"));
  }

  @Test
  void withReturnsTheStateThatDiffersByOneProperty() {
    // spec: BlockState.with, BS-R6
    BlockState state = registry().state(26);

    assertThat(state.with("type", "right")).isEqualTo(registry().state(28));
    assertThat(state.with("type", "right").id()).isEqualTo(28);
    assertThat(state.with("type", "left")).isEqualTo(state);
  }

  @Test
  void withRejectsAnUnknownPropertyOrValue() {
    // spec: BlockState.with contract (IllegalArgumentException)
    BlockState state = registry().state(26);

    Failures.expect(IllegalArgumentException.class, () -> state.with("axis", "x"));
    Failures.expect(IllegalArgumentException.class, () -> state.with("type", "double"));
  }

  @Test
  void valuesAreListedInPropertyNameOrder() {
    // spec: BlockState.values contract (property name order), BS-R6, BS-R8
    SequencedMap<String, String> values = registry().state(26).values();

    assertThat(List.copyOf(values.keySet())).containsExactly("facing", "type", "waterlogged");
    assertThat(List.copyOf(values.values())).containsExactly("north", "left", "true");
  }

  @Test
  void blockWithoutPropertiesHasNoValues() {
    // spec: BS-R15
    assertThat(registry().state(1).values()).isEmpty();
    assertThat(registry().air().values()).isEmpty();
  }

  @Test
  void valuesMapCannotBeModified() {
    // spec: BlockState.values contract (unmodifiable)
    SequencedMap<String, String> values = registry().state(26).values();

    Failures.expect(UnsupportedOperationException.class, () -> values.put("facing", "south"));
  }

  @Test
  void statesAreEqualWhenTheirIdentifiersAreEqual() {
    // spec: BlockState (two states are equal when they have the same identifier)
    assertThat(registry().state(26)).isEqualTo(registry().state(26));
    assertThat(registry().state(26)).hasSameHashCodeAs(registry().state(26));
    assertThat(registry().state(26)).isNotEqualTo(registry().state(27));
  }
}
