package dev.shoko.minecraft.item.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import java.util.List;

/** Assertions on {@link Decoding} results, by finding code and never by message. */
public final class Findings {

  private Findings() {}

  /** The value of a success; fails the test with the findings otherwise. */
  public static <T> T value(Decoding<T> decoding) {
    assertThat(decoding)
        .as("findings: %s", decoding.findings())
        .isInstanceOf(Decoding.Success.class);
    return ((Decoding.Success<T>) decoding).value();
  }

  public static <T> void assertFailure(Decoding<T> decoding, FindingCode... codes) {
    assertThat(decoding)
        .as("findings: %s", decoding.findings())
        .isInstanceOf(Decoding.Failure.class);
    assertThat(codesOf(decoding)).contains(codes);
  }

  public static <T> void assertAnyFailure(Decoding<T> decoding) {
    assertThat(decoding)
        .as("expected a failure, findings: %s", decoding.findings())
        .isInstanceOf(Decoding.Failure.class);
    assertThat(decoding.findings()).isNotEmpty();
  }

  /** A success that carries exactly these warning codes. */
  public static <T> T assertWarnings(Decoding<T> decoding, FindingCode... codes) {
    T value = value(decoding);
    assertThat(codesOf(decoding)).containsExactly(codes);
    return value;
  }

  public static List<FindingCode> codesOf(Decoding<?> decoding) {
    return decoding.findings().stream().map(Finding::code).toList();
  }
}
