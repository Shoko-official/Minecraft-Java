package dev.shoko.minecraft.tools.diff;

import java.util.List;

/**
 * Renders a comparison in the report format of docs/VERIFICATION.md. Labels are French like the
 * rest of the documentation; non-ASCII letters are written as escapes to keep the source ASCII.
 *
 * @param subject what was compared, with its inputs (subsystem, seed, zone)
 * @param reference identity of the reference state (version, oracle cache)
 * @param stage generation stage at fault, or a statement that none applies
 * @param hypotheses supposed causes, each labelled as a hypothesis; empty when there is none
 */
public record DivergenceReport(
    String subject, String reference, String stage, List<String> hypotheses) {

  private static final String E_ACUTE = "\u00e9";
  private static final String E_ACUTE_UPPER = "\u00c9";
  private static final String E_GRAVE = "\u00e8";
  private static final String E_GRAVE_UPPER = "\u00c8";
  private static final String HYPOTHESIS = "HYPOTH" + E_GRAVE_UPPER + "SE";

  public DivergenceReport {
    hypotheses = List.copyOf(hypotheses);
    for (String line : List.of(subject, reference, stage)) {
      if (line.isBlank() || line.contains("\n")) {
        throw new IllegalArgumentException("report fields are non-blank single lines: " + line);
      }
    }
  }

  public String render(Comparison comparison) {
    StringBuilder text = new StringBuilder();
    line(text, "Sujet      : ", subject);
    line(text, "R" + E_ACUTE + "f" + E_ACUTE + "rence  : ", reference);
    line(text, "Verdict    : ", verdict(comparison));
    line(
        text,
        "Premier " + E_ACUTE + "cart : ",
        comparison.first().map(DivergenceReport::describe).orElse("aucun"));
    line(text, E_ACUTE_UPPER + "tape      : ", stage);
    line(text, "Hypoth" + E_GRAVE + "ses : ", renderHypotheses());
    return text.toString();
  }

  private static String verdict(Comparison comparison) {
    String expected = grouped(comparison.expectedSize());
    if (comparison.verdict() == Comparison.Verdict.IDENTICAL) {
      return "IDENTICAL (" + expected + " entr" + E_ACUTE + "es)";
    }
    String text =
        "DIVERGENT ("
            + grouped(comparison.count())
            + " "
            + E_ACUTE
            + "cart"
            + (comparison.count() > 1 ? "s" : "")
            + " sur "
            + expected
            + " entr"
            + E_ACUTE
            + "es attendues";
    if (comparison.actualSize() != comparison.expectedSize()) {
      text += ", " + grouped(comparison.actualSize()) + " obtenues";
    }
    return text + ")";
  }

  /** One line locating a divergence: key, then expected and actual values. */
  static String describe(Divergence divergence) {
    if (divergence.kind() == Divergence.Kind.ORDER) {
      return "ordre en position "
          + divergence.expectedIndex()
          + " attendu "
          + escape(divergence.expected())
          + " obtenu "
          + escape(divergence.actual());
    }
    return escape(divergence.key())
        + " attendu "
        + escape(divergence.expected())
        + " obtenu "
        + escape(divergence.actual());
  }

  private String renderHypotheses() {
    if (hypotheses.isEmpty()) {
      return "aucune";
    }
    return String.join(
        " ; ", hypotheses.stream().map(h -> HYPOTHESIS + " : " + escape(h)).toList());
  }

  /** A count with thousands separated by spaces, as in the documented example "1 048 576". */
  static String grouped(int count) {
    String digits = Integer.toString(count);
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        text.append(' ');
      }
      text.append(digits.charAt(i));
    }
    return text.toString();
  }

  /** Keeps every value on its line. */
  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(value).append('\n');
  }
}
