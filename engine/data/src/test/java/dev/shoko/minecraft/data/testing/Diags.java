package dev.shoko.minecraft.data.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.diag.Severity;
import java.util.List;

/**
 * Assertions and projections on diagnostics. Tests check code, severity, file and JSON path, never
 * the message (R61 b).
 */
public final class Diags {

  private Diags() {}

  /** Projects diagnostics to {@code CODE@path} strings, in order, using the R61 text of a path. */
  public static List<String> summary(List<Diagnostic> diagnostics) {
    return diagnostics.stream().map(d -> d.code() + "@" + d.path()).toList();
  }

  public static List<String> summary(Outcome<?> outcome) {
    return summary(outcome.diagnostics());
  }

  public static List<DiagnosticCode> codes(Outcome<?> outcome) {
    return outcome.diagnostics().stream().map(Diagnostic::code).toList();
  }

  /** Asserts one diagnostic, an error with this code, and no value. */
  public static void assertSingleError(Outcome<?> outcome, DiagnosticCode code) {
    assertThat(outcome.value()).as("value of an invalid input").isEmpty();
    assertThat(outcome.diagnostics()).as("diagnostics").hasSize(1);
    Diagnostic diagnostic = outcome.diagnostics().get(0);
    assertThat(diagnostic.code()).isEqualTo(code);
    assertThat(diagnostic.code().severity()).isEqualTo(Severity.ERROR);
    assertThat(outcome.hasErrors()).isTrue();
  }

  /** Same as {@link #assertSingleError(Outcome, DiagnosticCode)} and checks the JSON path. */
  public static void assertSingleError(Outcome<?> outcome, DiagnosticCode code, String path) {
    assertSingleError(outcome, code);
    assertThat(outcome.diagnostics().get(0).path().toString()).isEqualTo(path);
  }

  /** Asserts the diagnostics, as {@code CODE@path} strings, in this exact order. */
  public static void assertSummary(Outcome<?> outcome, String... expected) {
    assertThat(summary(outcome)).containsExactly(expected);
  }
}
