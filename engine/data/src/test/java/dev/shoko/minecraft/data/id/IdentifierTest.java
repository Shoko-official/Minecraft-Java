package dev.shoko.minecraft.data.id;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R20 to R25, vectors of the table "Identifiants"
class IdentifierTest {

  @Test
  void provisionalDefaultNamespaceIsMinecraft() {
    // spec: R22
    assertThat(Identifier.DEFAULT_NAMESPACE).isEqualTo("minecraft");
  }

  @ParameterizedTest
  @CsvSource({
    "minecraft:stone, minecraft, stone",
    "stone, minecraft, stone",
    "':stone', minecraft, stone",
    "minecraft/villager, minecraft, minecraft/villager",
    "foo:bar/baz.qux-1_2, foo, bar/baz.qux-1_2",
    "a.b-c_d0:p/q/r, a.b-c_d0, p/q/r",
    "foo:a/b/c/d/e, foo, a/b/c/d/e"
  })
  void provisionalParseReadsNamespaceAndPath(String text, String namespace, String path) {
    // spec: R20, R22 (no ':' or a leading ':' means the namespace minecraft)
    Identifier id = Identifier.parse(text);
    assertThat(id.namespace()).isEqualTo(namespace);
    assertThat(id.path()).isEqualTo(path);
    assertThat(Identifier.tryParse(text)).hasValue(id);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Foo:bar",
        "foo:Bar",
        "foo:bar baz",
        "foo/bar:baz",
        "a:b:c",
        "..:x",
        "STONE",
        "caf\351",
        "foo:b\351",
        "foo:a\nb",
        "foo:a\\b",
        "foo:a*b",
        "foo:a+b",
        "foo bar:x",
        "#minecraft:logs",
        "#logs"
      })
  void provisionalParseRejectsInvalidTextWithoutRewriting(String text) {
    // spec: R20 (no normalization), R23 ('#' only where a tag is expected), table "Identifiants"
    Expect.illegalArgument(() -> Identifier.parse(text));
    assertThat(Identifier.tryParse(text)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"foo:", ":", "", "minecraft:"})
  void provisionalEmptyPathIsAcceptedSyntactically(String text) {
    // spec: R22 (S): an empty path is valid text; it matches no element
    Identifier id = Identifier.parse(text);
    assertThat(id.path()).isEmpty();
    assertThat(Identifier.isValidPath("")).isTrue();
  }

  @Test
  void provisionalEmptyTextReadsAsDefaultNamespaceAndEmptyPath() {
    // spec: R22 (S)
    assertThat(Identifier.parse("").toString()).isEqualTo("minecraft:");
    assertThat(Identifier.parse(":").toString()).isEqualTo("minecraft:");
    assertThat(Identifier.parse("foo:").toString()).isEqualTo("foo:");
  }

  @Test
  void provisionalToStringAlwaysHasTheNamespace() {
    // spec: R22, R25
    assertThat(Identifier.parse("stone").toString()).isEqualTo("minecraft:stone");
    assertThat(Identifier.parse(":stone").toString()).isEqualTo("minecraft:stone");
    assertThat(Identifier.parse("foo:bar/baz").toString()).isEqualTo("foo:bar/baz");
  }

  @Test
  void provisionalOfBuildsFromTwoParts() {
    // spec: R20
    Identifier id = Identifier.of("sonde", "a/b");
    assertThat(id.namespace()).isEqualTo("sonde");
    assertThat(id.path()).isEqualTo("a/b");
    assertThat(id.toString()).isEqualTo("sonde:a/b");
  }

  @Test
  void provisionalMinecraftBuildsInTheDefaultNamespace() {
    // spec: R22
    Identifier id = Identifier.minecraft("worldgen/biome");
    assertThat(id.namespace()).isEqualTo("minecraft");
    assertThat(id.path()).isEqualTo("worldgen/biome");
  }

  @Test
  void provisionalOfAndMinecraftRejectInvalidParts() {
    // spec: R20 (the parts must already be valid)
    Expect.illegalArgument(() -> Identifier.of("Foo", "bar"));
    Expect.illegalArgument(() -> Identifier.of("foo", "Bar"));
    Expect.illegalArgument(() -> Identifier.of("..", "x"));
    Expect.illegalArgument(() -> Identifier.of("a/b", "x"));
    Expect.illegalArgument(() -> Identifier.of("a b", "x"));
    Expect.illegalArgument(() -> Identifier.minecraft("Stone"));
    Expect.illegalArgument(() -> Identifier.minecraft("a b"));
  }

  @Test
  void provisionalNamespaceAlphabetDoesNotAllowSlash() {
    // spec: R20 (the path alphabet adds '/', the namespace one does not)
    assertThat(Identifier.isValidNamespace("a/b")).isFalse();
    assertThat(Identifier.isValidPath("a/b")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"minecraft", "a", "a.b", "a-b", "a_b", "a0", "0", "-", "_", "x.y-z_0"})
  void provisionalValidNamespaces(String namespace) {
    // spec: R20
    assertThat(Identifier.isValidNamespace(namespace)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"..", "A", "a b", "a/b", "a:b", "\351", "a#", "a\n"})
  void provisionalInvalidNamespaces(String namespace) {
    // spec: R20 (the namespace ".." is invalid)
    assertThat(Identifier.isValidNamespace(namespace)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"stone", "a/b/c", "a.b-c_d", "0/9", "a//b", "/x", "x/"})
  void provisionalValidPaths(String path) {
    // spec: R20 (alphabet only; no rule on empty segments)
    assertThat(Identifier.isValidPath(path)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"A", "a b", "a:b", "\351", "a#b", "a\\b", "a\tb", "a\u0000b"})
  void provisionalInvalidPaths(String path) {
    // spec: R20
    assertThat(Identifier.isValidPath(path)).isFalse();
  }

  @Test
  void provisionalEqualityComparesFullStringsAfterReading() {
    // spec: R25
    Identifier a = Identifier.parse("stone");
    Identifier b = Identifier.parse("minecraft:stone");
    Identifier c = Identifier.parse(":stone");
    assertThat(a).isEqualTo(b).isEqualTo(c).isEqualTo(Identifier.of("minecraft", "stone"));
    assertThat(a.hashCode()).isEqualTo(b.hashCode()).isEqualTo(c.hashCode());
    assertThat(a).isNotEqualTo(Identifier.parse("foo:stone"));
    assertThat(a).isNotEqualTo(Identifier.parse("minecraft:stones"));
    assertThat(a).isNotEqualTo("minecraft:stone").isNotEqualTo(null);
  }

  @Test
  void provisionalSlashBelongsToThePathNotToTheNamespace() {
    // spec: R22 ('minecraft/villager' has no ':', so it is a path in the default namespace)
    assertThat(Identifier.parse("minecraft/villager"))
        .isNotEqualTo(Identifier.parse("minecraft:villager"))
        .isEqualTo(Identifier.of("minecraft", "minecraft/villager"));
  }

  @Test
  void provisionalOrdersByNamespaceThenPathByCodePoint() {
    // spec: R30 (provisional order of elements); Identifier.compareTo
    List<Identifier> ids = new ArrayList<>();
    for (String text : List.of("b:a", "a:z", "a:a_b", "a:a/b", "a:a.b", "a:a-b", "a:a0", "a:a")) {
      ids.add(Identifier.parse(text));
    }
    Collections.sort(ids);
    assertThat(ids.stream().map(Identifier::toString))
        .containsExactly("a:a", "a:a-b", "a:a.b", "a:a/b", "a:a0", "a:a_b", "a:z", "b:a");
  }

  @Test
  void provisionalCompareToIsConsistentWithEquals() {
    // spec: R25, R30
    Identifier a = Identifier.parse("minecraft:stone");
    Identifier b = Identifier.parse("stone");
    assertThat(a.compareTo(b)).isZero();
    assertThat(a.compareTo(Identifier.parse("minecraft:stonf"))).isNegative();
    assertThat(Identifier.parse("minecraft:stonf").compareTo(a)).isPositive();
    assertThat(Identifier.parse("a:z").compareTo(Identifier.parse("b:a"))).isNegative();
  }
}
