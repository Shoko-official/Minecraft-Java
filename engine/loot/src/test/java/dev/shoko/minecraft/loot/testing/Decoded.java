package dev.shoko.minecraft.loot.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import java.util.List;

/** Assertions on {@link LootDecoding} results, by finding code and never by message. */
public final class Decoded {

  private Decoded() {}

  /** The value of a success; fails the test with the findings otherwise. */
  public static <T> T value(LootDecoding<T> decoding) {
    assertThat(decoding)
        .as("findings: %s", decoding.findings())
        .isInstanceOf(LootDecoding.Success.class);
    if (decoding instanceof LootDecoding.Success<T> success) {
      return success.value();
    }
    throw new AssertionError("not a success");
  }

  /** Asserts a failure that carries at least these codes. */
  public static <T> void failsWith(LootDecoding<T> decoding, LootFindingCode... codes) {
    assertThat(decoding)
        .as("expected a failure, findings: %s", decoding.findings())
        .isInstanceOf(LootDecoding.Failure.class);
    assertThat(codes(decoding)).contains(codes);
  }

  /** Asserts a failure, whatever the cause. */
  public static <T> void fails(LootDecoding<T> decoding) {
    assertThat(decoding)
        .as("expected a failure, findings: %s", decoding.findings())
        .isInstanceOf(LootDecoding.Failure.class);
    assertThat(decoding.findings()).isNotEmpty();
  }

  /** A success that carries exactly these warning codes. */
  public static <T> T warnings(LootDecoding<T> decoding, LootFindingCode... codes) {
    T value = value(decoding);
    assertThat(codes(decoding)).containsExactly(codes);
    return value;
  }

  public static List<LootFindingCode> codes(LootDecoding<?> decoding) {
    return decoding.findings().stream().map(LootFinding::code).toList();
  }

  /**
   * Checks the contract of {@link LootDecoding}: a success has no finding of severity ERROR, a
   * failure has at least one, and every finding has a code and a JSON path rooted at {@code $}.
   */
  public static void assertWellFormed(LootDecoding<?> decoding) {
    boolean hasError =
        decoding.findings().stream().anyMatch(f -> f.code().severity() == Severity.ERROR);
    if (decoding instanceof LootDecoding.Success<?>) {
      assertThat(hasError).as("a success carries no ERROR: %s", decoding.findings()).isFalse();
    } else {
      assertThat(hasError).as("a failure carries an ERROR: %s", decoding.findings()).isTrue();
    }
    for (LootFinding finding : decoding.findings()) {
      assertThat(finding.code()).isNotNull();
      assertThat(finding.path()).isNotNull().startsWith("$");
    }
  }
}
