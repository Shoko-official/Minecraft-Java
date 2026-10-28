package dev.shoko.minecraft.data.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Expect;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R26, R28, R30 (Registry: ordered entries and raw ids)
class RegistryTest {

  private static RegistryKey<String> key() {
    return new RegistryKey<>(Identifier.minecraft("block"));
  }

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static SequencedMap<Identifier, String> entries(String... names) {
    SequencedMap<Identifier, String> map = new LinkedHashMap<>();
    for (String name : names) {
      map.put(id(name), "value-" + name);
    }
    return map;
  }

  @Test
  void rawIdsFollowTheIterationOrderOfTheEntries() {
    // spec: R26 (protocol ids are the positions 0 to n-1)
    Registry<String> registry = Registry.of(key(), entries("air", "stone", "dirt"));
    assertThat(registry.size()).isEqualTo(3);
    assertThat(registry.ids()).containsExactly(id("air"), id("stone"), id("dirt"));
    assertThat(registry.rawIdOf(id("air"))).isEqualTo(OptionalInt.of(0));
    assertThat(registry.rawIdOf(id("stone"))).isEqualTo(OptionalInt.of(1));
    assertThat(registry.rawIdOf(id("dirt"))).isEqualTo(OptionalInt.of(2));
    assertThat(registry.rawIdOf(id("glass"))).isEqualTo(OptionalInt.empty());
  }

  @Test
  void entriesAreFoundByIdentifierAndByRawId() {
    Registry<String> registry = Registry.of(key(), entries("air", "stone"));
    assertThat(registry.contains(id("stone"))).isTrue();
    assertThat(registry.contains(id("dirt"))).isFalse();
    assertThat(registry.get(id("stone"))).hasValue("value-stone");
    assertThat(registry.get(id("dirt"))).isEmpty();
    assertThat(registry.byRawId(1).orElseThrow().value()).isEqualTo("value-stone");
    assertThat(registry.byRawId(0).orElseThrow().id()).hasValue(id("air"));
  }

  @Test
  void rawIdsOutOfRangeGiveNothing() {
    Registry<String> registry = Registry.of(key(), entries("air", "stone"));
    assertThat(registry.byRawId(-1)).isEmpty();
    assertThat(registry.byRawId(2)).isEmpty();
    assertThat(registry.byRawId(Integer.MAX_VALUE)).isEmpty();
    assertThat(registry.byRawId(Integer.MIN_VALUE)).isEmpty();
  }

  @Test
  void aHolderIsBoundToItsValue() {
    Registry<String> registry = Registry.of(key(), entries("air", "stone"));
    Optional<Holder<String>> holder = registry.holder(id("stone"));
    assertThat(holder.orElseThrow().value()).isEqualTo("value-stone");
    assertThat(holder.orElseThrow().id()).hasValue(id("stone"));
    assertThat(registry.holder(id("dirt"))).isEmpty();
  }

  @Test
  void anEmptyRegistryExists() {
    // spec: R28 (registries without any vanilla file exist, empty)
    Registry<String> registry = Registry.of(key(), entries());
    assertThat(registry.size()).isZero();
    assertThat(registry.ids()).isEmpty();
    assertThat(registry.byRawId(0)).isEmpty();
    assertThat(registry.defaultId()).isEmpty();
  }

  @Test
  void theKeyIsTheOneGiven() {
    assertThat(Registry.of(key(), entries("air")).key()).isEqualTo(key());
  }

  @Test
  void aRegistryWithoutDefaultHasNone() {
    assertThat(Registry.of(key(), entries("air")).defaultId()).isEmpty();
  }

  @Test
  void aDefaultEntryIsKept() {
    // spec: R26 (10 registries have a default entry)
    Registry<String> registry = Registry.of(key(), entries("air", "stone"), id("air"));
    assertThat(registry.defaultId()).hasValue(id("air"));
    assertThat(registry.size()).isEqualTo(2);
  }

  @Test
  void aDefaultThatIsNotAnEntryIsRefused() {
    Expect.illegalArgument(() -> Registry.of(key(), entries("air"), id("stone")));
  }

  @Test
  void laterChangesToTheGivenMapDoNotAffectTheRegistry() {
    SequencedMap<Identifier, String> map = entries("air", "stone");
    Registry<String> registry = Registry.of(key(), map);
    map.put(id("dirt"), "late");
    map.remove(id("air"));
    assertThat(registry.size()).isEqualTo(2);
    assertThat(registry.contains(id("air"))).isTrue();
    assertThat(registry.contains(id("dirt"))).isFalse();
    assertThat(registry.ids()).containsExactly(id("air"), id("stone"));
  }

  @Test
  void theIdentifierListIsUnmodifiable() {
    Registry<String> registry = Registry.of(key(), entries("air"));
    List<Identifier> ids = registry.ids();
    Expect.unmodifiable(() -> ids.add(id("stone")));
    Expect.unmodifiable(ids::clear);
  }

  @Test
  void aLargeRegistryKeepsEveryRawId() {
    SequencedMap<Identifier, String> map = new LinkedHashMap<>();
    for (int i = 0; i < 10_000; i++) {
      map.put(Identifier.of("test", "e" + i), "v" + i);
    }
    Registry<String> registry = Registry.of(key(), map);
    assertThat(registry.size()).isEqualTo(10_000);
    assertThat(registry.rawIdOf(Identifier.of("test", "e9999"))).isEqualTo(OptionalInt.of(9999));
    assertThat(registry.byRawId(1234).orElseThrow().id()).hasValue(Identifier.of("test", "e1234"));
  }
}
