package dev.shoko.minecraft.tools.diff;

import static dev.shoko.minecraft.tools.diff.WorldFixture.entry;
import static dev.shoko.minecraft.tools.diff.WorldFixture.indexOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DivergenceReportTest {

  private static final DivergenceReport REPORT =
      new DivergenceReport(
          "worldgen/surface seed=12345 zone=(2..3,-1..-1)",
          "26.3 oracle, cache 2026-10-28",
          "surface",
          List.of());

  /** Expands {e} {E} {g} {G} into French accented letters, keeping this source ASCII. */
  private static String fr(String text) {
    return text.replace("{e}", "\u00e9")
        .replace("{E}", "\u00c9")
        .replace("{g}", "\u00e8")
        .replace("{G}", "\u00c8");
  }

  @Test
  void rendersTheDocumentedFormatForADivergence() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String key = "chunk(3,-1)/section(4)/block(49,70,-12)";
    actual.set(indexOf(actual, key), entry(key, "minecraft:dirt"));
    Comparison comparison =
        StateComparator.compare(WorldFixture.reference(), WorldFixture.state(actual));

    assertThat(REPORT.render(comparison))
        .isEqualTo(
            fr(
                """
            Sujet      : worldgen/surface seed=12345 zone=(2..3,-1..-1)
            R{e}f{e}rence  : 26.3 oracle, cache 2026-10-28
            Verdict    : DIVERGENT (1 {e}cart sur 44 entr{e}es attendues)
            Premier {e}cart : chunk(3,-1)/section(4)/block(49,70,-12) attendu minecraft:air \
            obtenu minecraft:dirt
            {E}tape      : surface
            Hypoth{g}ses : aucune
            """));
  }

  @Test
  void rendersAnIdenticalVerdict() {
    Comparison comparison =
        StateComparator.compare(WorldFixture.reference(), WorldFixture.reference());

    String text = REPORT.render(comparison);

    assertThat(text).contains(fr("Verdict    : IDENTICAL (44 entr{e}es)\n"));
    assertThat(text).contains(fr("Premier {e}cart : aucun\n"));
  }

  @Test
  void statesDifferentSizesAndLabelsHypotheses() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    actual.removeIf(e -> StateKey.isUnder(e.key(), "chunk(3,-1)"));
    Comparison comparison =
        StateComparator.compare(WorldFixture.reference(), WorldFixture.state(actual));
    DivergenceReport report =
        new DivergenceReport(
            "subject", "reference", "inconnue", List.of("chunk save skipped", "line\nbreak"));

    String text = report.render(comparison);

    assertThat(text)
        .contains(fr("DIVERGENT (22 {e}carts sur 44 entr{e}es attendues, 22 obtenues)"))
        .contains("attendu minecraft:stone obtenu (absent)")
        .contains(
            fr("Hypoth{g}ses : HYPOTH{G}SE : chunk save skipped ; HYPOTH{G}SE : line\\nbreak"));
    assertThat(text.lines()).hasSize(6);
  }

  @Test
  void describesAnOrderDivergenceByPosition() {
    Divergence order = new Divergence(Divergence.Kind.ORDER, "a/b", "a/b", "a/c", 7, 7);

    assertThat(DivergenceReport.describe(order))
        .isEqualTo("ordre en position 7 attendu a/b obtenu a/c");
  }

  @Test
  void groupsThousandsAsInTheDocumentation() {
    assertThat(DivergenceReport.grouped(0)).isEqualTo("0");
    assertThat(DivergenceReport.grouped(999)).isEqualTo("999");
    assertThat(DivergenceReport.grouped(1000)).isEqualTo("1 000");
    assertThat(DivergenceReport.grouped(1_048_576)).isEqualTo("1 048 576");
  }

  @Test
  void refusesFieldsThatWouldBreakTheFormat() {
    assertThatThrownBy(() -> new DivergenceReport("a\nb", "r", "s", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DivergenceReport("a", " ", "s", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
