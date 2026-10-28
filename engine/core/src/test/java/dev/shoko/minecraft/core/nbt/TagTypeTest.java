package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TagTypeTest {

  // spec: core-nbt R1 (identifiers 0 to 6 and 8 to 11), R2 (7 and 12)
  static Stream<Arguments> identifiers() {
    return Stream.of(
        arguments(TagType.END, 0),
        arguments(TagType.BYTE, 1),
        arguments(TagType.SHORT, 2),
        arguments(TagType.INT, 3),
        arguments(TagType.LONG, 4),
        arguments(TagType.FLOAT, 5),
        arguments(TagType.DOUBLE, 6),
        arguments(TagType.BYTE_ARRAY, 7),
        arguments(TagType.STRING, 8),
        arguments(TagType.LIST, 9),
        arguments(TagType.COMPOUND, 10),
        arguments(TagType.INT_ARRAY, 11),
        arguments(TagType.LONG_ARRAY, 12));
  }

  @Test
  void thirteenTypesExist() {
    // spec: core-nbt R1, R2
    assertThat(TagType.values()).hasSize(13);
  }

  @ParameterizedTest(name = "{0} has identifier {1}")
  @MethodSource("identifiers")
  void typeHasItsBinaryIdentifier(TagType type, int id) {
    // spec: core-nbt R1, R2
    assertThat(type.id()).isEqualTo(id);
  }

  @ParameterizedTest(name = "identifier {1} is {0}")
  @MethodSource("identifiers")
  void identifierResolvesToItsType(TagType type, int id) {
    // spec: core-nbt R1, R2
    assertThat(TagType.byId(id)).contains(type);
  }

  @ParameterizedTest(name = "identifier {0}")
  @ValueSource(ints = {13, 14, 127, 128, 255, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void unknownIdentifierHasNoType(int id) {
    // spec: core-nbt R33 (type 13 or more is unknown)
    assertThat(TagType.byId(id)).isEmpty();
  }
}
