package dev.shoko.minecraft.item.menu;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.testing.ReferenceCache;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The layouts cover exactly the menu registry of the reference data (IC-R37). Reads the local cache
 * and skips itself when it is absent.
 */
@Tag("oracle")
class MenuLayoutsOracleTest {

  @Test
  void theLayoutsCoverTheTwentyFiveEntriesOfTheMenuRegistry() {
    // spec: IC-R37 (registry menu, 25 entries, D)
    Path reference = ReferenceCache.require();
    JsonObject registries =
        (JsonObject) ReferenceCache.json(reference.resolve("generated/reports/registries.json"));
    JsonObject menu = (JsonObject) registries.get("minecraft:menu").orElseThrow();
    JsonObject entries = (JsonObject) menu.get("entries").orElseThrow();

    List<Identifier> registered =
        entries.members().keySet().stream().map(Identifier::parse).sorted().toList();

    assertThat(registered).hasSize(25);
    assertThat(MenuLayouts.menuTypes()).isEqualTo(registered);
    for (Identifier type : registered) {
      assertThat(MenuLayouts.of(type)).as(type.toString()).isPresent();
    }
  }
}
