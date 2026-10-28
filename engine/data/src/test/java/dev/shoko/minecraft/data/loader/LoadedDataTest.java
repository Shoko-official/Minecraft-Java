package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.codec.Codecs;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.data.testing.Cx;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading K3, K4, R33, R64 (LoadedData.registryAccess: existence questions
// answered from identifiers alone, usable by the codecs of the owning modules)
class LoadedDataTest {

  private static RegistryKey<Object> key(String name) {
    return new RegistryKey<>(Identifier.parse(name));
  }

  private static RegistryAccess access() {
    return MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/minecraft/loot_table/ok.json",
                "{}",
                "data/minecraft/loot_table/broken.json",
                "{",
                "data/sonde/function/f1.mcfunction",
                "say 1",
                "data/minecraft/tags/block/logs.json",
                "{\"values\":[\"minecraft:birch_log\",\"minecraft:oak_log\"]}",
                "data/minecraft/tags/block/bad.json",
                "{\"values\":[\"minecraft:nope\"]}",
                "data/minecraft/tags/block/empty.json",
                "{\"values\":[]}"))
        .value()
        .orElseThrow()
        .registryAccess();
  }

  @Test
  void provisionalLoadedElementsAreKnownAndInvalidOrMissingOnesAreNot() {
    // spec: K4, R54
    RegistryAccess access = access();
    assertThat(access.contains(key("minecraft:loot_table"), Identifier.parse("ok"))).isTrue();
    assertThat(access.contains(key("minecraft:loot_table"), Identifier.parse("broken"))).isFalse();
    assertThat(access.contains(key("minecraft:loot_table"), Identifier.parse("absent"))).isFalse();
  }

  @Test
  void provisionalStaticEntriesAreKnownFromTheReport() {
    // spec: R26, R33
    RegistryAccess access = access();
    assertThat(access.contains(key("minecraft:block"), Identifier.parse("stone"))).isTrue();
    assertThat(access.contains(key("minecraft:block"), Identifier.parse("minecraft:oak_log")))
        .isTrue();
    assertThat(access.contains(key("minecraft:block"), Identifier.parse("granite"))).isFalse();
    assertThat(access.contains(key("minecraft:item"), Identifier.parse("stone"))).isFalse();
  }

  @Test
  void provisionalLoadedFunctionsAreKnown() {
    // spec: R10, R33
    RegistryAccess access = access();
    assertThat(access.contains(key("minecraft:function"), Identifier.parse("sonde:f1"))).isTrue();
    assertThat(access.contains(key("minecraft:function"), Identifier.parse("sonde:f2"))).isFalse();
  }

  @Test
  void anUnknownRegistryKnowsNothing() {
    assertThat(access().contains(key("minecraft:nothing"), Identifier.parse("ok"))).isFalse();
  }

  @Test
  void provisionalResolvedTagsGiveTheirMembersInOrder() {
    // spec: R35, RegistryAccess.tagMembers
    TagKey<Object> logs = new TagKey<>(key("minecraft:block"), Identifier.parse("logs"));
    assertThat(access().tagMembers(logs).orElseThrow().stream().map(Identifier::toString))
        .containsExactly("minecraft:birch_log", "minecraft:oak_log");
  }

  @Test
  void provisionalAnEmptyTagHasMembersAndAFailedOrAbsentTagHasNone() {
    // spec: R36, R38
    RegistryAccess access = access();
    RegistryKey<Object> block = key("minecraft:block");
    assertThat(access.tagMembers(new TagKey<>(block, Identifier.parse("empty"))))
        .hasValue(List.of());
    assertThat(access.tagMembers(new TagKey<>(block, Identifier.parse("bad")))).isEmpty();
    assertThat(access.tagMembers(new TagKey<>(block, Identifier.parse("absent")))).isEmpty();
    assertThat(access.tagMembers(new TagKey<>(key("minecraft:item"), Identifier.parse("logs"))))
        .isEmpty();
  }

  @Test
  void noTypedRegistryExistsUntilTheCallerBuildsOne() {
    // spec: LoadedData.registryAccess Javadoc
    assertThat(access().registry(key("minecraft:block"))).isEmpty();
    assertThat(access().registry(key("minecraft:loot_table"))).isEmpty();
  }

  @Test
  void provisionalCodecsCheckReferencesAgainstTheLoadedData() {
    // spec: K3, R64: an owning module decodes a reference field against the loaded universe
    RegistryAccess access = access();
    RegistryKey<String> blocks = Cx.blocks();
    Outcome<HolderSet<String>> tagged = Cx.decode(Codecs.holderSet(blocks), "\"#logs\"", access);
    assertThat(tagged.diagnostics()).isEmpty();
    assertThat(
            tagged.value().orElseThrow().holders().stream()
                .map(h -> h.id().orElseThrow().toString()))
        .containsExactly("minecraft:birch_log", "minecraft:oak_log");
    Diags.assertSummary(
        Cx.decode(Codecs.holderSet(blocks), "[\"stone\",\"granite\"]", access),
        "CODEC_UNRESOLVED_REFERENCE@$[1]");
    Diags.assertSummary(
        Cx.decode(Codecs.holderSet(blocks), "\"#bad\"", access), "CODEC_UNRESOLVED_REFERENCE@$");
  }
}
