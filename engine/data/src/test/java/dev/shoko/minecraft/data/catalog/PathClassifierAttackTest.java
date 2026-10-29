package dev.shoko.minecraft.data.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.MiniData;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R8 to R16, R20, R24 (adversarial): random paths classified against
// invariants that follow from the rules. Fixed seed.
class PathClassifierAttackTest {

  private static final String[] SEGMENTS = {
    "data",
    "minecraft",
    "sonde",
    "Sonde",
    "loot_table",
    "tags",
    "block",
    "function",
    "structure",
    "worldgen",
    "biome",
    "villager_trade",
    "dimension",
    "a",
    "b.json",
    "c.nbt",
    "d.mcfunction",
    "e",
    "..",
    ".",
    "",
    "x y",
    "caf\351",
    ".mcassetsroot",
    "pack.mcmeta",
    "item",
    "my-pack.v2",
    "x.json.json",
    "UPPER",
    "a_b"
  };

  private static String randomPath(Random random) {
    int count = 1 + random.nextInt(7);
    StringBuilder path = new StringBuilder();
    for (int i = 0; i < count; i++) {
      path.append(i == 0 ? "" : "/").append(SEGMENTS[random.nextInt(SEGMENTS.length)]);
    }
    return path.toString();
  }

  private static String folderOf(Identifier registry) {
    return registry.path();
  }

  @Test
  @Timeout(60)
  void provisionalEveryPathIsClassifiedWithoutFailureAndConsistently() {
    // spec: R8, R9, R10, R24: the identifier of a classified file rebuilds the path
    PathClassifier classifier = PathClassifier.of(MiniData.catalog());
    Random random = new Random(0xFA7AL);
    int attributed = 0;
    for (int i = 0; i < 40_000; i++) {
      String path = i % 3 == 0 ? "data/minecraft/" + randomPath(random) : randomPath(random);
      FileAttribution first = classifier.classify(path);
      assertThat(classifier.classify(path)).as(path).isEqualTo(first);
      switch (first) {
        case FileAttribution.Element element -> {
          attributed++;
          assertThat(path)
              .isEqualTo(
                  "data/"
                      + element.id().namespace()
                      + "/"
                      + folderOf(element.registry())
                      + "/"
                      + element.id().path()
                      + ".json");
        }
        case FileAttribution.Tag tag -> {
          attributed++;
          assertThat(path)
              .isEqualTo(
                  "data/"
                      + tag.id().namespace()
                      + "/tags/"
                      + folderOf(tag.registry())
                      + "/"
                      + tag.id().path()
                      + ".json");
        }
        case FileAttribution.Content content -> {
          attributed++;
          String extension = content.registry().path().equals("function") ? ".mcfunction" : ".nbt";
          assertThat(path)
              .isEqualTo(
                  "data/"
                      + content.id().namespace()
                      + "/"
                      + folderOf(content.registry())
                      + "/"
                      + content.id().path()
                      + extension);
        }
        case FileAttribution.Ignored ignored -> assertThat(ignored.message()).isNotNull();
        case FileAttribution.Metadata metadata -> assertThat(metadata).isNotNull();
        case FileAttribution.Skipped skipped -> assertThat(path).startsWith("data/");
      }
    }
    assertThat(attributed).as("random paths that name a real file").isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalGeneratedValidFilesAreAlwaysAttributedToTheirRegistry() {
    // spec: R8, R9, R11: valid namespaces, folders and paths always classify as the element
    PathClassifier classifier = PathClassifier.of(MiniData.catalog());
    Random random = new Random(0x600DL);
    String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789_-.";
    for (int i = 0; i < 5_000; i++) {
      StringBuilder namespace = new StringBuilder("n");
      StringBuilder path = new StringBuilder("p");
      for (int k = random.nextInt(6); k > 0; k--) {
        namespace.append(alphabet.charAt(random.nextInt(alphabet.length())));
      }
      String pathAlphabet = "abcdefghijklmnopqrstuvwxyz0123456789_-";
      for (int k = random.nextInt(8); k > 0; k--) {
        char next =
            random.nextInt(5) == 0
                ? '/'
                : pathAlphabet.charAt(random.nextInt(pathAlphabet.length()));
        if (next != '/' || path.charAt(path.length() - 1) != '/') {
          path.append(next);
        }
      }
      path.append('z');
      String text = path.toString();
      FileAttribution attribution =
          classifier.classify("data/" + namespace + "/loot_table/" + text + ".json");
      assertThat(attribution)
          .isEqualTo(
              new FileAttribution.Element(
                  Identifier.parse("minecraft:loot_table"),
                  Identifier.of(namespace.toString(), text)));
    }
  }

  @Test
  @Timeout(60)
  void provisionalVeryLongPathsAreHandled() {
    // spec: R21 (the longest data path is 139 characters); far beyond here
    PathClassifier classifier = PathClassifier.of(MiniData.catalog());
    String deep = "a/".repeat(50_000) + "z";
    assertThat(classifier.classify("data/x/loot_table/" + deep + ".json"))
        .isEqualTo(
            new FileAttribution.Element(
                Identifier.parse("minecraft:loot_table"), Identifier.of("x", deep)));
    assertThat(classifier.classify("data/" + "x".repeat(100_000) + "/loot_table/a.json"))
        .isInstanceOf(FileAttribution.Element.class);
    assertThat(classifier.classify("/".repeat(100_000))).isNotNull();
  }
}
