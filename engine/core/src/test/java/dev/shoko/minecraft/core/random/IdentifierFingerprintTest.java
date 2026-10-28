package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Identifier fingerprint (spec core-random R26). MD5 itself is level D; the hashed text (full
 * {@code namespace:path}, UTF-8) and the big-endian byte order are level S (question Q5, probe P1):
 * those tests are provisional.
 */
class IdentifierFingerprintTest {

  // spec: core-random R26 (S, provisional big-endian, Q5), vectors V-X4
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "probe:a, 0x27CDD0728604F33D, 0xF0084CE9792A919D",
    "minecraft:test, 0x1A2B64F5D3BDB461, 0x2E96D0A422FE68A7"
  })
  void provisionalBigEndianFingerprintMatchesVectorVX4(String identifier, String lo, String hi) {
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(Hex.seed(lo, hi));
  }

  // spec: core-random R26 (S, provisional): the little-endian counter-hypothesis (N4le) is not used
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "probe:a, 0x3DF3048672D0CD27, 0x9D912A79E94C08F0",
    "minecraft:test, 0x61B4BDD3F5642B1A, 0xA768FE22A4D0962E"
  })
  void provisionalFingerprintIsNotTheLittleEndianReading(String identifier, String lo, String hi) {
    assertThat(IdentifierFingerprint.of(identifier)).isNotEqualTo(Hex.seed(lo, hi));
  }

  // spec: core-random R26 (S, provisional): a path without namespace is hashed as minecraft:path
  @Test
  void provisionalPathWithoutNamespaceIsHashedWithMinecraftNamespace() {
    assertThat(IdentifierFingerprint.of("test"))
        .isEqualTo(IdentifierFingerprint.of("minecraft:test"))
        .isEqualTo(Hex.seed("0x1A2B64F5D3BDB461", "0x2E96D0A422FE68A7"));
  }

  // spec: core-random R26 (S, provisional normalization), vectors V-X4: test, :test and
  // minecraft:test have the same fingerprint
  @ParameterizedTest
  @ValueSource(strings = {"test", ":test", "minecraft:test"})
  void provisionalThreeFormsOfTestShareTheMinecraftTestFingerprint(String identifier) {
    assertThat(IdentifierFingerprint.of(identifier))
        .isEqualTo(Hex.seed("0x1A2B64F5D3BDB461", "0x2E96D0A422FE68A7"));
  }

  // spec: core-random R26 (S, provisional): an empty namespace becomes minecraft, whatever the path
  @Test
  void provisionalEmptyNamespaceBecomesMinecraft() throws NoSuchAlgorithmException {
    assertThat(IdentifierFingerprint.of(":chests/simple_dungeon"))
        .isEqualTo(IdentifierFingerprint.of("minecraft:chests/simple_dungeon"))
        .isEqualTo(md5BigEndian("minecraft:chests/simple_dungeon"));
  }

  // spec: core-random R26 known limit: the identifier is not validated; upper case and a second
  // colon are hashed as written
  @ParameterizedTest
  @ValueSource(strings = {"Minecraft:Test", "a:b:c", "probe:a:"})
  void provisionalInvalidIdentifiersAreHashedAsWritten(String identifier)
      throws NoSuchAlgorithmException {
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(md5BigEndian(identifier));
  }

  // spec: core-random R26 known limit: a lone surrogate is hashed as the UTF-8 encoder of Java
  // writes it ('?')
  @Test
  void provisionalLoneSurrogateIsHashedAsAQuestionMark() throws NoSuchAlgorithmException {
    String identifier = "probe:a" + (char) 0xD800;
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(md5BigEndian("probe:a?"));
  }

  // spec: core-random R26 (S, provisional): an explicit namespace is kept as is
  @Test
  void provisionalExplicitNamespaceIsKept() {
    assertThat(IdentifierFingerprint.of("a")).isEqualTo(IdentifierFingerprint.of("minecraft:a"));
    assertThat(IdentifierFingerprint.of("probe:a")).isNotEqualTo(IdentifierFingerprint.of("a"));
  }

  // spec: core-random R26 (S, provisional): MD5 of the full text in UTF-8, bytes 0-7 big-endian
  // form the low word and bytes 8-15 the high word
  @ParameterizedTest
  @ValueSource(
      strings = {
        "minecraft:chests/simple_dungeon",
        "minecraft:entities/zombie",
        "probe:f",
        "a:b",
        "minecraft:",
        "deep/nested/path:with/slashes",
        "x:0123456789abcdefghijklmnopqrstuvwxyz_-./"
      })
  void provisionalFingerprintIsTheMd5OfTheFullTextReadBigEndian(String identifier)
      throws NoSuchAlgorithmException {
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(md5BigEndian(identifier));
  }

  // spec: core-random R26 (S, provisional): the text is encoded in UTF-8 (identifier not validated)
  @Test
  void provisionalNonAsciiTextIsEncodedInUtf8() throws NoSuchAlgorithmException {
    String identifier = "probe:caf" + Character.toString(0xE9) + Character.toString(0x1F600);
    assertThat(identifier.getBytes(StandardCharsets.UTF_8)).hasSize(15);
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(md5BigEndian(identifier));
  }

  private static Seed128 md5BigEndian(String text) throws NoSuchAlgorithmException {
    byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
    ByteBuffer buffer = ByteBuffer.wrap(digest);
    return new Seed128(buffer.getLong(), buffer.getLong());
  }

  // spec: core-random R26 (pure function)
  @Test
  void fingerprintIsDeterministic() {
    assertThat(IdentifierFingerprint.of("probe:a")).isEqualTo(IdentifierFingerprint.of("probe:a"));
  }
}
