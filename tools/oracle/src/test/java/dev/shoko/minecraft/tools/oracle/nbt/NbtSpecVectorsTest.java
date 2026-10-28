package dev.shoko.minecraft.tools.oracle.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Replays the binary vectors V01 to V24 and E01 to E09 of docs/specs/core-nbt-vectors.md, bytes
 * copied from the spec, with the verdict the spec expects. Every vector is read as it is, root of
 * any type included; V20 is a network root without a name (R20).
 */
class NbtSpecVectorsTest {

  private static final String FAIL = "<fail>";
  private static final String ABSENT = "<absent>";
  private static final String ACCEPTED = "<accepted>";

  private static Arguments named(String id, String hex, String expected) {
    return Arguments.of(id, hex, true, expected);
  }

  static Stream<Arguments> vectors() {
    return Stream.of(
        named("V01", "00", ABSENT),
        named("V02", "01 00 01 62 ff", "b=-1b"),
        named("V03", "02 00 00 ff fe", "=-2s"),
        named("V04", "03 00 00 01 02 03 04", "=16909060"),
        named("V05", "04 00 00 80 00 00 00 00 00 00 00", "=-9223372036854775808L"),
        named("V06", "05 00 00 3f c0 00 00", "=f:3fc00000"),
        named("V07", "06 00 00 80 00 00 00 00 00 00 00", "=d:8000000000000000"),
        named("V08", "07 00 00 00 00 00 02 01 ff", "=B[1,-1]"),
        named("V09", "08 00 00 00 02 68 69", "=\"hi\""),
        named("V10", "09 00 00 00 00 00 00 00", "=[]"),
        named("V11", "09 00 00 03 00 00 00 02 00 00 00 07 00 00 00 08", "=[7,8]"),
        named("V12", "0a 00 00 01 00 01 61 01 00", "={a=1b}"),
        named("V13", "0b 00 00 00 00 00 02 00 00 00 01 00 00 00 02", "=I[1,2]"),
        named("V14", "0c 00 00 00 00 00 01 00 00 00 00 00 00 00 01", "=L[1]"),
        named("V15", "08 00 00 00 02 c0 80", "=\"\\u0000\""),
        named("V16", "08 00 00 00 02 c3 a9", "=\"\\u00e9\""),
        named("V17", "08 00 00 00 03 e2 82 ac", "=\"\\u20ac\""),
        named("V18", "08 00 00 00 06 ed a0 bd ed b8 80", "=\"\\ud83d\\ude00\""),
        named("V19", "0a 00 00 00", "={}"),
        Arguments.of("V20", "0a 00", false, "={}"),
        named("V21", "0a 00 00 01 00 01 7a 01 01 00 01 61 02 00", "={z=1b,a=2b}"),
        named("V22", "05 00 00 7f c0 00 00", "=f:7fc00000"),
        named("V23", "06 00 00 7f f0 00 00 00 00 00 00", "=d:7ff0000000000000"),
        named("V24", "09 00 00 01 00 00 00 00", "=[]"),
        named("E01", "03 00 00 01 02 03", FAIL),
        named("E02", "0d 00 00", FAIL),
        named("E03", "09 00 00 00 00 00 00 01", FAIL),
        named("E04", "07 00 00 ff ff ff ff", FAIL),
        named("E05", "09 00 00 01 ff ff ff ff", "=[]"),
        named("E06", "08 00 00 00 01 00", FAIL),
        named("E07", "08 00 00 00 04 f0 9f 98 80", FAIL),
        named("E08", "0a 00 00 01 00 01 61 01", FAIL),
        named("E09a", nestedLists(512), ACCEPTED),
        named("E09b", nestedLists(513), FAIL));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("vectors")
  void readsTheSpecVectorWithTheExpectedVerdict(
      String id, String hex, boolean namedRoot, String expected) {
    byte[] bytes = HexFormat.of().parseHex(hex.replace(" ", ""));
    NbtReader reader = new NbtReader();

    if (expected.equals(FAIL)) {
      assertThatThrownBy(() -> reader.readAnyRoot(new ByteArrayInputStream(bytes), namedRoot))
          .as(id)
          .isInstanceOf(IOException.class)
          .hasMessageStartingWith("Malformed NBT at ");
      return;
    }
    Optional<NbtReader.NamedTag> root;
    try {
      root = reader.readAnyRoot(new ByteArrayInputStream(bytes), namedRoot);
    } catch (IOException e) {
      throw new AssertionError(id + " should be accepted: " + e.getMessage(), e);
    }
    String actual = root.map(tag -> tag.name() + "=" + render(tag.value())).orElse(ABSENT);
    if (expected.equals(ACCEPTED)) {
      assertThat(actual).as(id).isNotEqualTo(ABSENT);
    } else {
      assertThat(actual).as(id).isEqualTo(expected);
    }
  }

  @Test
  void depthVectorsHaveTheSizeAndDigestOfTheSpec() throws NoSuchAlgorithmException {
    byte[] accepted = HexFormat.of().parseHex(nestedLists(512));
    byte[] refused = HexFormat.of().parseHex(nestedLists(513));

    assertThat(accepted).hasSize(2563);
    assertThat(sha256(accepted))
        .isEqualTo("c577920edeaead13ad505ca09a1859049301d88e58ecb8a53c7e10cae672d02e");
    assertThat(refused).hasSize(2568);
    assertThat(sha256(refused))
        .isEqualTo("c6ca4e050c4cdab8eb7f9cd9924855aba1f9de7fd2539de59a8e34b3b45c72cd");
  }

  /** E09: {@code 09 00 00}, then (n - 1) times {@code 09 00 00 00 01}, then an empty list. */
  private static String nestedLists(int containers) {
    return "090000" + "0900000001".repeat(containers - 1) + "0000000000";
  }

  private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  /** Type-tagged text of a decoded value; floats by their exact bit pattern (R35). */
  private static String render(Object value) {
    return switch (value) {
      case Byte b -> b + "b";
      case Short s -> s + "s";
      case Integer i -> i.toString();
      case Long l -> l + "L";
      case Float f -> "f:" + Integer.toHexString(Float.floatToRawIntBits(f));
      case Double d -> "d:" + Long.toHexString(Double.doubleToRawLongBits(d));
      case String s -> quote(s);
      case byte[] a -> "B" + join(IntStream.range(0, a.length).mapToObj(i -> "" + a[i]));
      case int[] a -> "I" + join(Arrays.stream(a).mapToObj(Integer::toString));
      case long[] a -> "L" + join(Arrays.stream(a).mapToObj(Long::toString));
      case List<?> list -> join(list.stream().map(NbtSpecVectorsTest::render));
      case Map<?, ?> map ->
          map.entrySet().stream()
              .map(e -> e.getKey() + "=" + render(e.getValue()))
              .collect(Collectors.joining(",", "{", "}"));
      default -> throw new IllegalArgumentException("unexpected value " + value);
    };
  }

  private static String join(Stream<String> values) {
    return values.collect(Collectors.joining(",", "[", "]"));
  }

  private static String quote(String text) {
    StringBuilder quoted = new StringBuilder("\"");
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= 0x20 && c < 0x7f && c != '"' && c != '\\') {
        quoted.append(c);
      } else {
        quoted.append(String.format("\\u%04x", (int) c));
      }
    }
    return quoted.append('"').toString();
  }
}
