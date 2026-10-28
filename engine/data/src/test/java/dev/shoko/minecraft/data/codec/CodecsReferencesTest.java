package dev.shoko.minecraft.data.codec;

import static dev.shoko.minecraft.data.testing.Cx.blocks;
import static dev.shoko.minecraft.data.testing.Cx.decode;
import static dev.shoko.minecraft.data.testing.Cx.tagT;
import static dev.shoko.minecraft.data.testing.Cx.universe;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.FakeAccess;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R64, K3, vectors VC18 to VC24 (references and sets of elements)
class CodecsReferencesTest {

  private static List<String> ids(HolderSet<String> set) {
    return set.holders().stream().map(h -> h.id().orElseThrow().toString()).toList();
  }

  private static HolderSet<String> set(String json, FakeAccess access) {
    Outcome<HolderSet<String>> outcome = decode(Codecs.holderSet(blocks()), json, access);
    assertThat(outcome.diagnostics()).as(json).isEmpty();
    return outcome.value().orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"minecraft:a\"", "\"a\"", "\":a\""})
  void provisionalAPlainStringIsAnInlineSetOfOneElement(String json) {
    // spec: VC18, R64 a
    HolderSet<String> set = set(json, universe());
    assertThat(set.tagKey()).isEmpty();
    assertThat(ids(set)).containsExactly("minecraft:a");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"#minecraft:t\"", "\"#t\"", "\"#:t\""})
  void provisionalAHashStringIsAReferenceToATagOfTheSameRegistry(String json) {
    // spec: VC19, R64 b
    HolderSet<String> set = set(json, universe());
    assertThat(set.tagKey()).hasValue(tagT());
    assertThat(ids(set)).containsExactly("minecraft:a", "minecraft:b");
  }

  @Test
  void provisionalAnArrayIsAnInlineListKeptInOrder() {
    // spec: VC20, R64 c
    assertThat(ids(set("[\"a\",\"b\"]", universe()))).containsExactly("minecraft:a", "minecraft:b");
    assertThat(ids(set("[\"b\",\"a\"]", universe()))).containsExactly("minecraft:b", "minecraft:a");
    assertThat(set("[\"a\",\"b\"]", universe()).tagKey()).isEmpty();
  }

  @Test
  void provisionalAnEmptyArrayIsAnEmptySet() {
    // spec: VC20, R64 c
    HolderSet<String> set = set("[]", universe());
    assertThat(set.holders()).isEmpty();
    assertThat(set.tagKey()).isEmpty();
  }

  @Test
  void provisionalARepeatedElementIsKeptOnceAtItsFirstPlaceWithoutDiagnostic() {
    // spec: VC20, R64 c
    assertThat(ids(set("[\"a\",\"a\",\"b\"]", universe())))
        .containsExactly("minecraft:a", "minecraft:b");
    assertThat(ids(set("[\"b\",\"a\",\"b\",\"minecraft:a\"]", universe())))
        .containsExactly("minecraft:b", "minecraft:a");
  }

  @Test
  void provisionalATagReferenceInsideAnArrayIsAnInvalidIdentifier() {
    // spec: VC21, R64 c
    Outcome<HolderSet<String>> outcome =
        decode(Codecs.holderSet(blocks()), "[\"a\",\"#t\"]", universe());
    Diags.assertSummary(outcome, "CODEC_INVALID_IDENTIFIER@$[1]");
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalAnUnknownElementIsUnresolvedAtItsPlace() {
    // spec: VC22, R64
    Diags.assertSummary(
        decode(Codecs.holderSet(blocks()), "[\"a\",\"x\"]", universe()),
        "CODEC_UNRESOLVED_REFERENCE@$[1]");
    Diags.assertSummary(
        decode(Codecs.holderSet(blocks()), "\"x\"", universe()), "CODEC_UNRESOLVED_REFERENCE@$");
  }

  @Test
  void provisionalAnUnknownTagIsUnresolved() {
    // spec: VC22, R64 b
    Diags.assertSummary(
        decode(Codecs.holderSet(blocks()), "\"#nope\"", universe()),
        "CODEC_UNRESOLVED_REFERENCE@$");
  }

  @Test
  void provisionalAFailedTagCountsAsAbsent() {
    // spec: R64 ("un tag en echec compte comme absent"); a failed tag has no members in the access
    FakeAccess access = FakeAccess.create().entries(blocks(), "a", "b");
    Diags.assertSummary(
        decode(Codecs.holderSet(blocks()), "\"#t\"", access), "CODEC_UNRESOLVED_REFERENCE@$");
  }

  @Test
  void provisionalAnEmptyTagIsAFoundTag() {
    // spec: R38, R64
    FakeAccess access = FakeAccess.create().tag(blocks(), "t");
    HolderSet<String> set = set("\"#t\"", access);
    assertThat(set.tagKey()).hasValue(tagT());
    assertThat(set.holders()).isEmpty();
  }

  @Test
  void provisionalEveryFaultyEntryOfAnArrayIsReported() {
    // spec: R62 g, R64
    Outcome<HolderSet<String>> outcome =
        decode(Codecs.holderSet(blocks()), "[\"#t\",\"x\",\"a\",1,\"Bad:Id\"]", universe());
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(
        outcome,
        "CODEC_INVALID_IDENTIFIER@$[0]",
        "CODEC_UNRESOLVED_REFERENCE@$[1]",
        "CODEC_TYPE_MISMATCH@$[3]",
        "CODEC_INVALID_IDENTIFIER@$[4]");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "1", "1.5", "true", "null", "{\"id\":\"a\"}"})
  void provisionalAnyOtherTypeIsATypeMismatch(String json) {
    // spec: VC23, R64
    Diags.assertSingleError(
        decode(Codecs.holderSet(blocks()), json, universe()),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$");
  }

  @Test
  void provisionalAnArrayEntryThatIsNotAStringIsATypeMismatch() {
    // spec: Codecs.holderSet Javadoc
    Diags.assertSummary(
        decode(Codecs.holderSet(blocks()), "[\"a\",true,null,{}]", universe()),
        "CODEC_TYPE_MISMATCH@$[1]",
        "CODEC_TYPE_MISMATCH@$[2]",
        "CODEC_TYPE_MISMATCH@$[3]");
  }

  @Test
  void provisionalASetOfAnotherRegistryDoesNotSeeTheEntriesOfThisOne() {
    // spec: R33, R64 (references are checked in the registry of the field)
    FakeAccess access = universe();
    Codec<HolderSet<String>> items =
        Codecs.holderSet(new RegistryKey<>(Identifier.minecraft("item")));
    Diags.assertSummary(decode(items, "\"a\"", access), "CODEC_UNRESOLVED_REFERENCE@$");
  }

  @Test
  void provisionalASingleReferenceAcceptsAnIdentifierOfTheRegistry() {
    // spec: VC24, R64
    Outcome<Holder<String>> outcome =
        decode(Codecs.registryReference(blocks()), "\"a\"", universe());
    assertThat(outcome.diagnostics()).isEmpty();
    Holder<String> holder = outcome.value().orElseThrow();
    assertThat(holder.id()).hasValue(Identifier.minecraft("a"));
    assertThat(holder).isEqualTo(Holder.reference(blocks(), Identifier.minecraft("a"), universe()));
  }

  @Test
  void provisionalASingleReferenceRefusesATagReference() {
    // spec: VC24, R64 ("#tag y est CODEC_INVALID_IDENTIFIER")
    Diags.assertSingleError(
        decode(Codecs.registryReference(blocks()), "\"#t\"", universe()),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$");
  }

  @Test
  void provisionalASingleReferenceToAnAbsentEntryIsUnresolved() {
    // spec: Codecs.registryReference Javadoc, K3
    Diags.assertSummary(
        decode(Codecs.registryReference(blocks()), "\"x\"", universe()),
        "CODEC_UNRESOLVED_REFERENCE@$");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "true", "null", "[]", "[\"a\"]", "{}"})
  void provisionalASingleReferenceAcceptsOnlyAString(String json) {
    // spec: R64, R62 f
    Diags.assertSingleError(
        decode(Codecs.registryReference(blocks()), json, universe()),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$");
  }

  @Test
  void provisionalASingleReferenceWithABadIdentifierIsInvalid() {
    // spec: R20, R64
    Diags.assertSingleError(
        decode(Codecs.registryReference(blocks()), "\"Bad:Id\"", universe()),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$");
  }

  @Test
  void provisionalReferencesInsideAnObjectKeepTheirFieldPath() {
    // spec: K3 (the owning module calls the combinator), R61 a
    Codec<List<Object>> codec =
        Codecs.object(
            reader -> {
              Supplier<Holder<String>> one =
                  reader.required("block", Codecs.registryReference(blocks()));
              Supplier<HolderSet<String>> many =
                  reader.required("blocks", Codecs.holderSet(blocks()));
              return () -> List.of(one.get(), many.get());
            });
    Outcome<List<Object>> outcome =
        decode(codec, "{\"block\":\"x\",\"blocks\":[\"a\",\"y\"]}", universe());
    Diags.assertSummary(
        outcome, "CODEC_UNRESOLVED_REFERENCE@$.block", "CODEC_UNRESOLVED_REFERENCE@$.blocks[1]");
  }
}
