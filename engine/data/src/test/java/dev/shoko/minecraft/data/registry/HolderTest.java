package dev.shoko.minecraft.data.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.FakeAccess;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading K1, K4 (references resolved on first use, even in a loop)
class HolderTest {

  /** A value that refers to another one through a holder, to build loops. */
  private record Node(String name, Holder<Node> next) {}

  private static RegistryKey<String> key() {
    return new RegistryKey<>(Identifier.minecraft("block"));
  }

  private static RegistryKey<Node> nodeKey() {
    return new RegistryKey<>(Identifier.minecraft("node"));
  }

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static Registry<String> registry() {
    SequencedMap<Identifier, String> map = new LinkedHashMap<>();
    map.put(id("air"), "AIR");
    map.put(id("stone"), "STONE");
    return Registry.of(key(), map);
  }

  @Test
  void aDirectHolderWrapsAValueWithoutIdentifier() {
    Holder<String> holder = Holder.direct("v");
    assertThat(holder.value()).isEqualTo("v");
    assertThat(holder.id()).isEmpty();
  }

  @Test
  void directHoldersAreEqualWhenTheirValuesAre() {
    assertThat(Holder.direct("v")).isEqualTo(Holder.direct("v"));
    assertThat(Holder.direct("v").hashCode()).isEqualTo(Holder.direct("v").hashCode());
    assertThat(Holder.direct("v")).isNotEqualTo(Holder.direct("w"));
  }

  @Test
  void aReferenceResolvesToTheRegistryValue() {
    FakeAccess access = FakeAccess.create().typedRegistry(registry());
    Holder<String> holder = Holder.reference(key(), id("stone"), access);
    assertThat(holder.value()).isEqualTo("STONE");
    assertThat(holder.id()).hasValue(id("stone"));
  }

  @Test
  void nothingIsLookedUpBeforeTheValueIsRequested() {
    // spec: Holder Javadoc ("resolved on first use")
    FakeAccess access = FakeAccess.create().typedRegistry(registry());
    Holder<String> holder = Holder.reference(key(), id("stone"), access);
    assertThat(access.lookups()).isZero();
    assertThat(holder.id()).hasValue(id("stone"));
    assertThat(access.lookups()).isZero();
    assertThat(holder.value()).isEqualTo("STONE");
    assertThat(access.lookups()).isPositive();
  }

  @Test
  void aReferenceToAnAbsentEntryFailsWhenResolved() {
    // spec: Holder.value Javadoc (IllegalStateException)
    FakeAccess access = FakeAccess.create().typedRegistry(registry());
    Holder<String> holder = Holder.reference(key(), id("glass"), access);
    Expect.illegalState(holder::value);
    assertThat(holder.id()).hasValue(id("glass"));
  }

  @Test
  void aReferenceToAnUnbuiltRegistryFailsWhenResolved() {
    // spec: RegistryAccess.registry ("empty when its owning module has not built it")
    Holder<String> holder = Holder.reference(key(), id("stone"), FakeAccess.create());
    Expect.illegalState(holder::value);
  }

  @Test
  void aReferenceMayBeBuiltBeforeTheRegistryExists() {
    // spec: K4 (any order of decoding)
    FakeAccess access = FakeAccess.create();
    Holder<String> holder = Holder.reference(key(), id("stone"), access);
    access.typedRegistry(registry());
    assertThat(holder.value()).isEqualTo("STONE");
  }

  @Test
  void referencesAreEqualWhenRegistryAndIdentifierAre() {
    // spec: Holder.equals Javadoc
    FakeAccess one = FakeAccess.create();
    FakeAccess two = FakeAccess.create().typedRegistry(registry());
    Holder<String> a = Holder.reference(key(), id("stone"), one);
    Holder<String> b = Holder.reference(key(), id("minecraft:stone"), two);
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(Holder.reference(key(), id("air"), one));
    assertThat(a).isNotEqualTo(Holder.reference(new RegistryKey<>(id("item")), id("stone"), one));
  }

  @Test
  void aReferenceIsNeverEqualToAValueInPlace() {
    Holder<String> reference = Holder.reference(key(), id("stone"), FakeAccess.create());
    assertThat(reference).isNotEqualTo(Holder.direct("STONE"));
    assertThat(Holder.direct("STONE")).isNotEqualTo(reference);
  }

  @Test
  void entriesMayReferToEachOtherInALoop() {
    // spec: Holder Javadoc ("even in a loop"), K4
    FakeAccess access = FakeAccess.create();
    Holder<Node> toB = Holder.reference(nodeKey(), id("b"), access);
    Holder<Node> toA = Holder.reference(nodeKey(), id("a"), access);
    SequencedMap<Identifier, Node> map = new LinkedHashMap<>();
    map.put(id("a"), new Node("A", toB));
    map.put(id("b"), new Node("B", toA));
    access.typedRegistry(Registry.of(nodeKey(), map));
    Node a = toA.value();
    assertThat(a.name()).isEqualTo("A");
    assertThat(a.next().value().name()).isEqualTo("B");
    assertThat(a.next().value().next().value()).isSameAs(a);
  }

  @Test
  void anEntryMayReferToItself() {
    FakeAccess access = FakeAccess.create();
    Holder<Node> self = Holder.reference(nodeKey(), id("a"), access);
    SequencedMap<Identifier, Node> map = new LinkedHashMap<>();
    map.put(id("a"), new Node("A", self));
    access.typedRegistry(Registry.of(nodeKey(), map));
    assertThat(self.value().next().value()).isSameAs(self.value());
  }
}
