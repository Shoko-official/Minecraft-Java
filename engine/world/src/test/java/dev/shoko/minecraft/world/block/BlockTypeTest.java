package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlockTypeTest {

  private static BlockType block(String id) {
    return Blocks.registry().block(id).orElseThrow();
  }

  @ParameterizedTest
  @CsvSource({
    "minecraft:air,1",
    "minecraft:grass_block,2",
    "minecraft:water,16",
    "minecraft:chest,24",
    "minecraft:acacia_fence,32",
    "minecraft:note_block,1350",
    "minecraft:barrel,12"
  })
  void stateCountIsTheProductOfThePropertySizes(String id, int count) {
    // spec: BS-R3, BS-R6, BS-R15 (1 state without properties, note_block is the largest)
    assertThat(block(id).stateCount()).isEqualTo(count);
  }

  @Test
  void propertiesAreSortedByNameNotInReportOrder() {
    // spec: BS-R8 (the report lists type first for chests; identifiers sort by name)
    List<String> names =
        block(Blocks.CHEST).properties().stream().map(BlockProperty::name).toList();

    assertThat(names).containsExactly("facing", "type", "waterlogged");
  }

  @Test
  void propertyKeepsTheValueOrderOfTheReport() {
    // spec: BS-R9 (the order of values depends on the block)
    assertThat(block(Blocks.CHEST).property("facing").orElseThrow().values())
        .containsExactly("north", "south", "west", "east");
    assertThat(block(Blocks.HOPPER).property("facing").orElseThrow().values())
        .containsExactly("down", "north", "south", "west", "east");
    assertThat(block(Blocks.BARREL).property("facing").orElseThrow().values())
        .containsExactly("north", "east", "south", "west", "up", "down");
  }

  @Test
  void propertyLookupIsEmptyForAnUnknownName() {
    assertThat(block(Blocks.STONE).property("facing")).isEmpty();
    assertThat(block(Blocks.CHEST).property("axis")).isEmpty();
    assertThat(block(Blocks.STONE).properties()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "minecraft:air,0,0",
    "minecraft:stone,1,1",
    "minecraft:grass_block,2,3",
    "minecraft:bedrock,4,4",
    "minecraft:water,5,5",
    "minecraft:oak_log,21,22",
    "minecraft:chest,24,25",
    "minecraft:hopper,48,48",
    "minecraft:water_cauldron,58,58",
    "minecraft:acacia_fence,61,92",
    "minecraft:note_block,93,94",
    "minecraft:barrel,1443,1444"
  })
  void firstAndDefaultStateIdsFollowTheReport(String id, int first, int defaultState) {
    // spec: BS-R5 (first id is the sum of the counts before), BS-R10 (default is not always first)
    assertThat(block(id).firstStateId()).isEqualTo(first);
    assertThat(block(id).defaultStateId()).isEqualTo(defaultState);
  }

  @Test
  void stateIdOfTheChestIgnoresTheReportOrderOfProperties() {
    // spec: BS-R8, "Cas limites" (chest[facing=north,type=left,waterlogged=true], not 32)
    Map<String, String> values = new LinkedHashMap<>();
    values.put("type", "left");
    values.put("waterlogged", "true");
    values.put("facing", "north");

    assertThat(block(Blocks.CHEST).stateId(values)).isEqualTo(26);
  }

  @Test
  void stateIdDoesNotDependOnTheIterationOrderOfTheMap() {
    // spec: BlockType.stateId contract, BS-R6
    Map<String, String> one = new LinkedHashMap<>();
    one.put("facing", "west");
    one.put("type", "right");
    one.put("waterlogged", "false");
    Map<String, String> other = new LinkedHashMap<>();
    other.put("waterlogged", "false");
    other.put("type", "right");
    other.put("facing", "west");

    assertThat(block(Blocks.CHEST).stateId(one)).isEqualTo(block(Blocks.CHEST).stateId(other));
    assertThat(block(Blocks.CHEST).stateId(one)).isEqualTo(24 + 2 * 6 + 2 * 2 + 1);
  }

  @Test
  void firstBooleanValueIsTheFirstState() {
    // spec: BS-R9, "Cas limites" (acacia_fence with every property true is its first state)
    Map<String, String> allTrue =
        Map.of(
            "east",
            "true",
            "north",
            "true",
            "south",
            "true",
            "waterlogged",
            "true",
            "west",
            "true");

    assertThat(block(Blocks.ACACIA_FENCE).stateId(allTrue)).isEqualTo(61);
  }

  @Test
  void firstValueOfAnotherListIsStillRankZero() {
    // spec: BS-R9 (hopper facing starts at down, cauldron level at 1)
    assertThat(block(Blocks.HOPPER).stateId(Map.of("enabled", "false", "facing", "east")))
        .isEqualTo(57);
    assertThat(block(Blocks.WATER_CAULDRON).stateId(Map.of("level", "2"))).isEqualTo(59);
  }

  @Test
  void largestBlockDoesNotOverflowTheProducts() {
    // spec: BS-R3, "Cas limites" (note_block as an overflow test)
    BlockType note = block(Blocks.NOTE_BLOCK);
    Map<String, String> last =
        Map.of("instrument", "instrument_26", "note", "24", "powered", "false");
    Map<String, String> middle = Map.of("instrument", "hat", "note", "7", "powered", "true");

    assertThat(note.stateId(last)).isEqualTo(note.firstStateId() + 1349);
    assertThat(note.stateId(middle)).isEqualTo(257);
  }

  @Test
  void blockWithoutPropertiesHasOneState() {
    // spec: BS-R15
    assertThat(block(Blocks.STONE).stateId(Map.of())).isEqualTo(1);
  }

  @Test
  void stateIdRejectsMissingUnknownAndOutOfListValues() {
    // spec: BlockType.stateId contract (IllegalArgumentException)
    BlockType log = block(Blocks.OAK_LOG);
    BlockType stone = block(Blocks.STONE);

    Failures.expect(IllegalArgumentException.class, () -> log.stateId(Map.of()));
    Failures.expect(IllegalArgumentException.class, () -> log.stateId(Map.of("axis", "w")));
    Failures.expect(
        IllegalArgumentException.class, () -> log.stateId(Map.of("axis", "x", "color", "red")));
    Failures.expect(IllegalArgumentException.class, () -> log.stateId(Map.of("color", "red")));
    Failures.expect(IllegalArgumentException.class, () -> stone.stateId(Map.of("axis", "x")));
  }

  @Test
  void valueOfReadsEachPropertyOfAState() {
    // spec: BlockType.valueOf, BS-R6
    BlockType chest = block(Blocks.CHEST);

    assertThat(chest.valueOf(26, "facing")).isEqualTo("north");
    assertThat(chest.valueOf(26, "type")).isEqualTo("left");
    assertThat(chest.valueOf(26, "waterlogged")).isEqualTo("true");
    assertThat(chest.valueOf(47, "facing")).isEqualTo("east");
  }

  @Test
  void valueOfRejectsAForeignStateAndAnUnknownProperty() {
    // spec: BlockType.valueOf contract (IllegalArgumentException)
    BlockType chest = block(Blocks.CHEST);

    Failures.expect(IllegalArgumentException.class, () -> chest.valueOf(23, "facing"));
    Failures.expect(IllegalArgumentException.class, () -> chest.valueOf(48, "facing"));
    Failures.expect(IllegalArgumentException.class, () -> chest.valueOf(-1, "facing"));
    Failures.expect(IllegalArgumentException.class, () -> chest.valueOf(26, "axis"));
  }

  @Test
  void withValueChangesOnlyOneProperty() {
    // spec: BlockType.withValue, BS-R6
    BlockType chest = block(Blocks.CHEST);

    assertThat(chest.withValue(26, "type", "right")).isEqualTo(28);
    assertThat(chest.withValue(26, "waterlogged", "false")).isEqualTo(27);
    assertThat(chest.withValue(26, "facing", "east")).isEqualTo(24 + 3 * 6 + 1 * 2);
    assertThat(chest.withValue(26, "type", "left")).isEqualTo(26);
  }

  @Test
  void withValueRejectsBadArguments() {
    // spec: BlockType.withValue contract (IllegalArgumentException)
    BlockType chest = block(Blocks.CHEST);

    Failures.expect(IllegalArgumentException.class, () -> chest.withValue(26, "type", "double"));
    Failures.expect(IllegalArgumentException.class, () -> chest.withValue(26, "axis", "x"));
    Failures.expect(IllegalArgumentException.class, () -> chest.withValue(1, "type", "left"));
  }

  @Test
  void everyStateOfEveryBlockRoundTripsThroughItsValues() {
    // spec: BS-R6 (the formula is a bijection onto the ids of the block)
    for (BlockType type : Blocks.registry().blocks()) {
      for (int id = type.firstStateId(); id < type.firstStateId() + type.stateCount(); id++) {
        Map<String, String> values = new LinkedHashMap<>();
        for (BlockProperty property : type.properties()) {
          values.put(property.name(), type.valueOf(id, property.name()));
        }
        assertThat(type.stateId(values)).as(type.id() + " " + values).isEqualTo(id);
      }
    }
  }
}
