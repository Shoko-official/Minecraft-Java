package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.RandomJson;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R4, R5, R7, R63 c (adversarial): random JSON read as pack.mcmeta.
// Whatever the shape, a typed outcome: not a pack (one PACK_INVALID warning), or a pack, broken or
// not, with only error diagnostics at paths of the file. Fixed seed.
class PackMetaReaderAttackTest {

  private static final List<String> KEYS =
      List.of(
          "pack",
          "pack",
          "description",
          "min_format",
          "max_format",
          "pack_format",
          "features",
          "enabled",
          "filter",
          "block",
          "namespace",
          "path",
          "overlays",
          "entries",
          "directory",
          "formats",
          "language",
          "zzz");
  private static final List<String> SCALARS =
      List.of(
          "121",
          "120",
          "122",
          "[121]",
          "[121,0]",
          "[121,1]",
          "[]",
          "{}",
          "null",
          "true",
          "\"x\"",
          "\"minecraft:trade_rebalance\"",
          "\"(\"",
          "\".*\"",
          "\"ov\"",
          "\"Bad Dir\"",
          "0",
          "-1",
          "2147483647",
          "1.5");

  @Test
  @Timeout(60)
  void provisionalRandomJsonNeverBreaksThePackMetaReader() {
    // spec: R4, R7
    Random random = new Random(0x9AC4L);
    RandomJson generator = new RandomJson(random, KEYS, SCALARS, 5);
    int packs = 0;
    for (int i = 0; i < 30_000; i++) {
      String text = generator.next();
      JsonNode root = Jx.parseLenient(text);
      Outcome<PackMeta> outcome = PackMetaReader.read(root);
      if (outcome.value().isEmpty()) {
        assertThat(outcome.diagnostics()).as(text).hasSize(1);
        assertThat(outcome.diagnostics().get(0).code())
            .as(text)
            .isEqualTo(DiagnosticCode.PACK_INVALID);
        continue;
      }
      packs++;
      PackMeta meta = outcome.value().get();
      PackCompatibility compatibility = meta.compatibility(PackType.DATA);
      assertThat(compatibility).as(text).isNotNull();
      if (outcome.hasErrors()) {
        assertThat(compatibility)
            .as("a pack with errors is broken: %s", text)
            .isEqualTo(PackCompatibility.BROKEN);
      }
      if (meta.formats().isEmpty()) {
        assertThat(compatibility).as(text).isEqualTo(PackCompatibility.BROKEN);
      }
      for (Diagnostic diagnostic : outcome.diagnostics()) {
        assertThat(diagnostic.path().toString()).as(text).startsWith("$");
        assertThat(diagnostic.code().severity().name()).as(text).isEqualTo("ERROR");
      }
    }
    assertThat(packs).as("random samples that are packs").isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalAnyWellFormedRangeIsClassifiedLikeThePureOrderOnFormats() {
    // spec: R5: compatibility follows the lexicographic order (major, minor) for every pair
    Random random = new Random(0x4A9EL);
    PackFormat target = PackType.DATA.targetFormat();
    for (int i = 0; i < 20_000; i++) {
      PackFormat min = new PackFormat(118 + random.nextInt(8), random.nextInt(3));
      PackFormat max = new PackFormat(118 + random.nextInt(8), random.nextInt(3));
      PackCompatibility expected =
          min.compareTo(max) > 0
              ? PackCompatibility.BROKEN
              : max.compareTo(target) < 0
                  ? PackCompatibility.TOO_OLD
                  : min.compareTo(target) > 0
                      ? PackCompatibility.TOO_NEW
                      : PackCompatibility.COMPATIBLE;
      assertThat(new PackFormatRange(min, max).compatibilityWith(target))
          .as("%s to %s", min, max)
          .isEqualTo(expected);
      String json =
          "{\"pack\":{\"min_format\":["
              + min.major()
              + ","
              + min.minor()
              + "],\"max_format\":["
              + max.major()
              + ","
              + max.minor()
              + "]}}";
      Outcome<PackMeta> outcome = PackMetaReader.read(Jx.parse(json));
      assertThat(outcome.diagnostics()).as(json).isEmpty();
      assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
          .as(json)
          .isEqualTo(expected);
    }
  }
}
