package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R30: the order of identifiers is the lexicographic order of the full
// text "namespace:path" by code point. '-' (45) < '.' (46) < '0' (48) < ':' (58), so the namespace
// "a" sorts after the namespaces that extend it with one of these characters.
class IdentifierOrderHuntTest {

  private static final List<String> EXPECTED = List.of("a-b:c", "a.b:z", "a0:x", "a:b");

  private static List<String> texts(Iterable<Identifier> ids) {
    List<String> texts = new ArrayList<>();
    ids.forEach(id -> texts.add(id.toString()));
    return texts;
  }

  private static String[] files(String folder, String extension, String content) {
    return new String[] {
      "data/a/" + folder + "/b" + extension, content,
      "data/a-b/" + folder + "/c" + extension, content,
      "data/a0/" + folder + "/x" + extension, content,
      "data/a.b/" + folder + "/z" + extension, content
    };
  }

  @Test
  void compareToOrdersByTheFullTextOfTheIdentifier() {
    List<Identifier> ids = new ArrayList<>();
    EXPECTED.forEach(text -> ids.add(Identifier.parse(text)));
    Collections.shuffle(ids, new Random(30));
    Collections.sort(ids);
    assertThat(texts(ids)).containsExactlyElementsOf(EXPECTED);
    assertThat(Identifier.parse("a:b")).isGreaterThan(Identifier.parse("a0:x"));
    assertThat(Identifier.parse("a0:x")).isGreaterThan(Identifier.parse("a.b:z"));
    assertThat(Identifier.parse("a.b:z")).isGreaterThan(Identifier.parse("a-b:c"));
  }

  @Test
  void elementsOfARegistryIterateInTheFullTextOrder() {
    LoadedData data =
        MiniData.load(Packs.implicit("vanilla", files("loot_table", ".json", "{}")))
            .value()
            .orElseThrow();
    assertThat(MiniData.elementIds(data, "minecraft:loot_table"))
        .containsExactlyElementsOf(EXPECTED);
  }

  @Test
  void resolvedTagsIterateInTheFullTextOrder() {
    LoadedData data =
        MiniData.load(
                Packs.implicit("vanilla", files("tags/loot_table", ".json", "{\"values\":[]}")))
            .value()
            .orElseThrow();
    TagResolution tags = data.tags(MiniData.registry("minecraft:loot_table")).orElseThrow();
    assertThat(texts(tags.tags())).containsExactlyElementsOf(EXPECTED);
  }

  @Test
  void functionsIterateInTheFullTextOrder() {
    LoadedData data =
        MiniData.load(Packs.implicit("vanilla", files("function", ".mcfunction", "say hi")))
            .value()
            .orElseThrow();
    assertThat(texts(data.functions().keySet())).containsExactlyElementsOf(EXPECTED);
  }
}
