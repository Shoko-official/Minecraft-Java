package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.loot.assembly.LootLoadFinding;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// spec: LT-R15 / LT-R14b: loading the 1447 real tables must not warn that a table reads a
// parameter its own type does not provide: such a warning is a hole in the provisional sets.
@Tag("oracle")
class LoadFindingsHuntTest {

  @Test
  void provisionalTheRealTablesReadOnlyParametersTheirTypeProvides() {
    List<LootLoadFinding> findings = RealData.result().findings();
    Map<String, Integer> byMessage = new TreeMap<>();
    for (LootLoadFinding f : findings) {
      if (f.registry().path().equals("loot_table")) {
        byMessage.merge(f.finding().code() + " " + f.finding().message(), 1, Integer::sum);
      }
    }
    System.out.println("HUNT loot_table findings: " + byMessage);
    assertThat(byMessage).isEmpty();
  }
}
