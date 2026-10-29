package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R7 (names of the registry entries): the constants name the
// components that a rule of the module reads.
class WellKnownComponentsTest {

  private static Map<String, Identifier> constants() {
    Map<String, Identifier> constants = new LinkedHashMap<>();
    constants.put("max_stack_size", WellKnownComponents.MAX_STACK_SIZE);
    constants.put("max_damage", WellKnownComponents.MAX_DAMAGE);
    constants.put("damage", WellKnownComponents.DAMAGE);
    constants.put("unbreakable", WellKnownComponents.UNBREAKABLE);
    constants.put("enchantments", WellKnownComponents.ENCHANTMENTS);
    constants.put("stored_enchantments", WellKnownComponents.STORED_ENCHANTMENTS);
    constants.put("use_remainder", WellKnownComponents.USE_REMAINDER);
    constants.put("equippable", WellKnownComponents.EQUIPPABLE);
    constants.put("cooking_fuel", WellKnownComponents.COOKING_FUEL);
    constants.put("compostable", WellKnownComponents.COMPOSTABLE);
    constants.put("brewing_fuel", WellKnownComponents.BREWING_FUEL);
    constants.put("potion_contents", WellKnownComponents.POTION_CONTENTS);
    constants.put("trim", WellKnownComponents.TRIM);
    constants.put("container", WellKnownComponents.CONTAINER);
    return constants;
  }

  @Test
  void everyConstantIsTheMinecraftIdentifierOfItsComponent() {
    // spec: IC-R7
    constants()
        .forEach(
            (path, constant) ->
                assertThat(constant).as(path).isEqualTo(Identifier.minecraft(path)));
  }

  @Test
  void theConstantsAreDistinct() {
    // spec: IC-R7
    assertThat(constants().values()).doesNotHaveDuplicates();
    assertThat(constants()).hasSize(14);
  }
}
