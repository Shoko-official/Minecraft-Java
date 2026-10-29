package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.JsonPath;

/** Why a document is invalid, with the place where the reader gave up. Never leaves the package. */
final class ParseFailure extends Exception {

  private static final long serialVersionUID = 1L;

  private final transient DiagnosticCode code;
  private final transient JsonPath path;

  ParseFailure(DiagnosticCode code, JsonPath path) {
    super(code.name(), null, false, false);
    this.code = code;
    this.path = path;
  }

  DiagnosticCode code() {
    return code;
  }

  JsonPath path() {
    return path;
  }
}
