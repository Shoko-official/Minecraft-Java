package dev.shoko.minecraft.data.id;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R20 to R25 (adversarial): fixed seed, a reference model written from
// the rules, and only IllegalArgumentException as a failure mode.
class IdentifierAttackTest {

  private static final String ALPHABET =
      "abcxyz019_-./:#AZ \351\337"
          + (char) 0x4E2D
          + new String(Character.toChars(0x1F600))
          + "\ud800\n\t\\*";

  private static String randomText(Random random, int maxLength) {
    int length = random.nextInt(maxLength + 1);
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < length; i++) {
      text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return text.toString();
  }

  /** The rules R20 and R22, written independently of the implementation. */
  private static Optional<String> expected(String text) {
    int colon = text.indexOf(':');
    String namespace = colon < 0 ? "minecraft" : text.substring(0, colon);
    String path = colon < 0 ? text : text.substring(colon + 1);
    if (colon == 0) {
      namespace = "minecraft";
    }
    boolean validNamespace = !namespace.equals("..") && namespace.matches("[a-z0-9_.-]+");
    boolean validPath = path.matches("[a-z0-9_.\\-/]*");
    boolean singleColon = text.indexOf(':', colon + 1) < 0 || colon < 0;
    return validNamespace && validPath && singleColon
        ? Optional.of(namespace + ":" + path)
        : Optional.empty();
  }

  @Test
  @Timeout(60)
  void provisionalRandomTextsFollowTheReferenceModel() {
    // spec: R20, R22, R25
    Random random = new Random(0x1D5EEDL);
    for (int i = 0; i < 20_000; i++) {
      String text = randomText(random, 12);
      Optional<String> model = expected(text);
      Optional<Identifier> parsed = Identifier.tryParse(text);
      assertThat(parsed.map(Identifier::toString)).as("text %s", escape(text)).isEqualTo(model);
      if (model.isEmpty()) {
        Expect.illegalArgument(() -> Identifier.parse(text));
      } else {
        assertThat(Identifier.parse(text)).isEqualTo(parsed.orElseThrow());
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalValidIdentifiersRoundTripThroughTheirFullForm() {
    // spec: R25
    Random random = new Random(0xBEEFL);
    int checked = 0;
    for (int i = 0; i < 20_000; i++) {
      Optional<Identifier> parsed = Identifier.tryParse(randomText(random, 10));
      if (parsed.isEmpty()) {
        continue;
      }
      Identifier id = parsed.get();
      checked++;
      assertThat(Identifier.parse(id.toString())).isEqualTo(id);
      assertThat(Identifier.parse(id.toString()).hashCode()).isEqualTo(id.hashCode());
      assertThat(Identifier.of(id.namespace(), id.path())).isEqualTo(id);
      assertThat(Identifier.isValidNamespace(id.namespace())).isTrue();
      assertThat(Identifier.isValidPath(id.path())).isTrue();
      assertThat(id.compareTo(Identifier.parse(id.toString()))).isZero();
    }
    assertThat(checked).as("valid samples drawn").isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalOrderingIsATotalOrderConsistentWithEquals() {
    // spec: R25, R30
    Random random = new Random(7L);
    Identifier[] ids = new Identifier[200];
    for (int i = 0; i < ids.length; i++) {
      String path = randomText(random, 6).replaceAll("[^a-z0-9_./-]", "x");
      ids[i] = Identifier.of(i % 3 == 0 ? "a" : "b", path);
    }
    for (Identifier x : ids) {
      for (Identifier y : ids) {
        assertThat(Integer.signum(x.compareTo(y))).isEqualTo(-Integer.signum(y.compareTo(x)));
        assertThat(x.compareTo(y) == 0).isEqualTo(x.equals(y));
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalHugeTextsAreHandledWithoutFailureOtherThanInvalidity() {
    // spec: R20 (no length limit stated)
    String longValid = "a".repeat(1_000_000);
    assertThat(Identifier.parse(longValid).path()).hasSize(1_000_000);
    assertThat(Identifier.tryParse("x:" + longValid + "A")).isEmpty();
    assertThat(Identifier.tryParse(":".repeat(100_000))).isEmpty();
  }

  private static String escape(String text) {
    StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      escaped.append(c < 0x20 || c > 0x7e ? String.format("\\u%04x", (int) c) : String.valueOf(c));
    }
    return escaped.toString();
  }
}
