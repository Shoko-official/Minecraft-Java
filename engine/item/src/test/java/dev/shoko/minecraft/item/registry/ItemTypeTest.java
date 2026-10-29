package dev.shoko.minecraft.item.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R1, IC-R15: air is the default entry and the empty item.
class ItemTypeTest {

  private static ItemType handMade(String path) {
    return new ItemType(Identifier.minecraft(path), 0, new ComponentMap(new LinkedHashMap<>()));
  }

  @Test
  void airIsAir() {
    // spec: IC-R1
    assertThat(handMade("air").isAir()).isTrue();
  }

  @Test
  void anotherItemIsNotAir() {
    // spec: IC-R1
    assertThat(handMade("stone").isAir()).isFalse();
    assertThat(handMade("air_block").isAir()).isFalse();
  }

  @Test
  void onlyTheDefaultEntryOfTheCatalogIsAir() {
    // spec: IC-R1, IC-R15
    for (String path : TestItems.itemPaths()) {
      assertThat(TestItems.type(path).isAir()).as(path).isEqualTo(path.equals("air"));
    }
  }

  @Test
  void aTypeIsAValueOfItsParts() {
    // spec: IC-R1 (the record keeps what it is given)
    ItemType type = handMade("stone");
    assertThat(type.id()).isEqualTo(Identifier.minecraft("stone"));
    assertThat(type.rawId()).isZero();
    assertThat(type.prototype().entries()).isEmpty();
  }
}
