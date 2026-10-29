package dev.shoko.minecraft.data.diag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R61, table "Codes de diagnostic" (closed list, one severity per code)
class DiagnosticCodeTest {

  private static Map<DiagnosticCode, Severity> table() {
    Map<DiagnosticCode, Severity> table = new EnumMap<>(DiagnosticCode.class);
    table.put(DiagnosticCode.JSON_SYNTAX, Severity.ERROR);
    table.put(DiagnosticCode.JSON_BOM, Severity.ERROR);
    table.put(DiagnosticCode.JSON_EMPTY, Severity.ERROR);
    table.put(DiagnosticCode.JSON_TRAILING, Severity.ERROR);
    table.put(DiagnosticCode.JSON_UTF8, Severity.ERROR);
    table.put(DiagnosticCode.JSON_TOO_DEEP, Severity.ERROR);
    table.put(DiagnosticCode.JSON_NUMBER_RANGE, Severity.ERROR);
    table.put(DiagnosticCode.JSON_DUPLICATE_KEY, Severity.WARNING);
    table.put(DiagnosticCode.CODEC_TYPE_MISMATCH, Severity.ERROR);
    table.put(DiagnosticCode.CODEC_MISSING_FIELD, Severity.ERROR);
    table.put(DiagnosticCode.CODEC_UNKNOWN_FIELD, Severity.ERROR);
    table.put(DiagnosticCode.CODEC_NUMBER_RANGE, Severity.ERROR);
    table.put(DiagnosticCode.CODEC_INVALID_IDENTIFIER, Severity.ERROR);
    table.put(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE, Severity.ERROR);
    table.put(DiagnosticCode.PACK_FLAGS_INACTIVE, Severity.ERROR);
    table.put(DiagnosticCode.PACK_INVALID, Severity.WARNING);
    table.put(DiagnosticCode.FILE_IGNORED, Severity.INFO);
    return table;
  }

  @Test
  void provisionalEveryCodeHasTheSeverityOfTheSpecTable() {
    table()
        .forEach(
            (code, severity) -> assertThat(code.severity()).as("%s", code).isEqualTo(severity));
  }

  @Test
  void provisionalTheListIsClosedAtSeventeenCodes() {
    assertThat(DiagnosticCode.values()).hasSize(17);
    assertThat(Arrays.stream(DiagnosticCode.values()).map(Enum::name))
        .containsExactlyInAnyOrderElementsOf(table().keySet().stream().map(Enum::name).toList());
  }

  @Test
  void severitiesAreErrorWarningInformation() {
    assertThat(Severity.values())
        .containsExactlyInAnyOrder(Severity.ERROR, Severity.WARNING, Severity.INFO);
  }
}
