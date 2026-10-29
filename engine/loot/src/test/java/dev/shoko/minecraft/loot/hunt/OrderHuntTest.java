package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// spec: loot-tables LT-R67 / item-enchantments EN-R38 ("candidats dans l'ordre du registre"):
// the iteration order of loaded registries feeds the draws, so it must be canonical.
@Tag("oracle")
class OrderHuntTest {

  private static <T extends Comparable<T>> void assertSorted(List<T> ids) {
    List<T> sorted = new ArrayList<>(ids);
    sorted.sort(null);
    assertThat(ids).isEqualTo(sorted);
  }

  @Test
  void enchantmentsAreLoadedInIdentifierOrder() {
    List<String> ids =
        RealData.data().enchantments().definitions().keySet().stream()
            .map(Identifier::toString)
            .toList();
    assertSorted(ids);
  }

  @Test
  void tablesAreLoadedInIdentifierOrder() {
    List<String> ids =
        RealData.data().tables().tables().keySet().stream().map(Identifier::toString).toList();
    assertSorted(ids);
  }
}
