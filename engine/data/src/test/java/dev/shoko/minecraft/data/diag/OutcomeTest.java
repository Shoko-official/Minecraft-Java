package dev.shoko.minecraft.data.diag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R54, R61 (Outcome: a value, if any, and the diagnostics found)
class OutcomeTest {

  private static final FileOrigin FILE = new FileOrigin("p", "data/a.json");

  private static Diagnostic diagnostic(DiagnosticCode code) {
    return new Diagnostic(code, FILE, JsonPath.root(), "message");
  }

  @Test
  void successHasValueAndNoDiagnostic() {
    Outcome<String> outcome = Outcome.success("v");
    assertThat(outcome.value()).hasValue("v");
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.hasErrors()).isFalse();
  }

  @Test
  void failureHasNoValue() {
    Outcome<String> outcome = Outcome.failure(List.of(diagnostic(DiagnosticCode.JSON_SYNTAX)));
    assertThat(outcome.value()).isEmpty();
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void diagnosticsKeepTheirOrder() {
    List<Diagnostic> list =
        List.of(
            diagnostic(DiagnosticCode.CODEC_TYPE_MISMATCH),
            diagnostic(DiagnosticCode.FILE_IGNORED),
            diagnostic(DiagnosticCode.JSON_DUPLICATE_KEY),
            diagnostic(DiagnosticCode.CODEC_MISSING_FIELD));
    assertThat(Outcome.success(1, list).diagnostics()).containsExactlyElementsOf(list);
    assertThat(Outcome.<Integer>failure(list).diagnostics()).containsExactlyElementsOf(list);
  }

  @Test
  void provisionalAValueMayComeWithErrors() {
    // a failed tag is dropped, the rest stays (R36); a broken pack is still present (R5)
    Outcome<String> outcome =
        Outcome.success("v", List.of(diagnostic(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE)));
    assertThat(outcome.value()).hasValue("v");
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void hasErrorsFollowsTheSeverityOfTheCodeNotTheCodeName() {
    for (DiagnosticCode code : DiagnosticCode.values()) {
      Outcome<String> outcome = Outcome.success("v", List.of(diagnostic(code)));
      assertThat(outcome.hasErrors())
          .as("code %s", code)
          .isEqualTo(code.severity() == Severity.ERROR);
    }
  }

  @Test
  void warningsAndInformationAreNotErrors() {
    Outcome<String> outcome =
        Outcome.success(
            "v",
            List.of(
                diagnostic(DiagnosticCode.JSON_DUPLICATE_KEY),
                diagnostic(DiagnosticCode.PACK_INVALID),
                diagnostic(DiagnosticCode.FILE_IGNORED)));
    assertThat(outcome.hasErrors()).isFalse();
  }

  @Test
  void oneErrorAmongWarningsIsEnough() {
    Outcome<String> outcome =
        Outcome.success(
            "v",
            List.of(
                diagnostic(DiagnosticCode.JSON_DUPLICATE_KEY),
                diagnostic(DiagnosticCode.PACK_FLAGS_INACTIVE),
                diagnostic(DiagnosticCode.FILE_IGNORED)));
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void laterChangesToTheGivenListDoNotAffectTheOutcome() {
    List<Diagnostic> list = new ArrayList<>(List.of(diagnostic(DiagnosticCode.JSON_SYNTAX)));
    Outcome<String> failure = Outcome.failure(list);
    Outcome<String> success = Outcome.success("v", list);
    list.add(diagnostic(DiagnosticCode.JSON_EMPTY));
    list.clear();
    assertThat(failure.diagnostics()).hasSize(1);
    assertThat(success.diagnostics()).hasSize(1);
  }

  @Test
  void diagnosticsAreUnmodifiable() {
    Outcome<String> outcome = Outcome.success("v", List.of(diagnostic(DiagnosticCode.JSON_SYNTAX)));
    List<Diagnostic> exposed = outcome.diagnostics();
    Expect.unmodifiable(() -> exposed.add(diagnostic(DiagnosticCode.JSON_EMPTY)));
    Expect.unmodifiable(exposed::clear);
  }

  @Test
  void diagnosticIsAPlainRecordOfFourComponents() {
    Diagnostic d = new Diagnostic(DiagnosticCode.JSON_SYNTAX, FILE, JsonPath.root(), "m");
    assertThat(d.code()).isEqualTo(DiagnosticCode.JSON_SYNTAX);
    assertThat(d.origin()).isEqualTo(FILE);
    assertThat(d.path()).isEqualTo(JsonPath.root());
    assertThat(d.message()).isEqualTo("m");
  }
}
