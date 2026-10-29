package dev.shoko.minecraft.loot.assembly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCodec;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.MiniDatapack;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R61a (3 named predicates), LT-R50 (named providers), EN-R1 and EN-R34 (enchantment
// registries): decoding of the registries found by the datapack loader, element by element
class NamedRegistriesDecodeTest {

  private static ElementRegistryData registry(LoadedData data, String name) {
    return data.elementRegistry(Identifier.minecraft(name)).orElseThrow();
  }

  private static LoadedData load(Map<String, String> files) {
    return MiniDatapack.load(files);
  }

  @Test
  void theNamedPredicatesDecodeByIdentifierAndFailuresAreListedApart() {
    // spec: LT-R61a, ConditionCodec#decodeNamed contract
    Map<String, String> files = new LinkedHashMap<>();
    files.put(
        "predicate/tool/can_shear",
        "{'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}");
    files.put("predicate/broken", "{'type':'minecraft:no_such_condition'}");
    LoadedData data = load(files);
    LootDecoding<NamedPredicates> decoding =
        ConditionCodec.decodeNamed(registry(data, "predicate"), data.registryAccess());
    assertThat(Decoded.codes(decoding)).contains(LootFindingCode.UNKNOWN_TYPE);
    if (decoding instanceof LootDecoding.Success<NamedPredicates> success) {
      assertThat(success.value().find(Identifier.parse("minecraft:tool/can_shear")))
          .containsInstanceOf(LootCondition.MatchTool.class);
      assertThat(success.value().find(Identifier.parse("minecraft:broken"))).isEmpty();
    }
  }

  @Test
  void validNamedPredicatesDecodeWithoutFinding() {
    // spec: LT-R61a
    Map<String, String> files = new LinkedHashMap<>();
    files.put(
        "predicate/tool/can_shear",
        "{'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}");
    files.put("predicate/always", "{'type':'minecraft:all_of','terms':[]}");
    LoadedData data = load(files);
    NamedPredicates named =
        Decoded.value(
            ConditionCodec.decodeNamed(registry(data, "predicate"), data.registryAccess()));
    assertThat(named.predicates().keySet())
        .extracting(Identifier::path)
        .containsExactly("always", "tool/can_shear");
  }

  @Test
  void theNamedProvidersDecodeInBothFamilies() {
    // spec: LT-R50 (26 int providers and 4 float providers in the data)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("context_int_provider/cooking/time_coal", "1600");
    files.put(
        "context_int_provider/compostable/low", "{'type':'minecraft:uniform','min':0,'max':1}");
    files.put("context_float_provider/cooking/speed_default", "1.0");
    LoadedData data = load(files);
    NumberProviderSet set =
        Decoded.value(
            NumberProviderCodec.decodeSet(
                registry(data, "context_int_provider"),
                registry(data, "context_float_provider"),
                data.registryAccess()));
    assertThat(set.intProvider(Identifier.parse("minecraft:cooking/time_coal")))
        .contains(new IntProvider.Constant(1600));
    assertThat(set.floatProvider(Identifier.parse("minecraft:cooking/speed_default")))
        .contains(new FloatProvider.Constant(1.0f));
    assertThat(set.ints().keySet())
        .extracting(Identifier::path)
        .containsExactly("compostable/low", "cooking/time_coal");
  }

  @Test
  void aProviderThatFailsIsReportedAndAReferenceToItIsUnresolved() {
    // spec: NumberProviderCodec#decodeSet contract (UNRESOLVED_REFERENCE)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("context_int_provider/broken", "{'type':'minecraft:no_such_type'}");
    files.put("context_int_provider/user", "'minecraft:broken'");
    LoadedData data = load(files);
    LootDecoding<NumberProviderSet> decoding =
        NumberProviderCodec.decodeSet(
            registry(data, "context_int_provider"),
            registry(data, "context_float_provider"),
            data.registryAccess());
    assertThat(Decoded.codes(decoding))
        .contains(LootFindingCode.UNKNOWN_TYPE, LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void theEnchantmentCatalogDecodesDefinitionsAndProviders() {
    // spec: EN-R1, EN-R34
    Map<String, String> files = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    files.put(
        "enchantment_provider/test_single",
        "{'type':'minecraft:single','enchantment':'minecraft:silk_touch','level':1}");
    LoadedData data = load(files);
    EnchantmentCatalog catalog =
        Decoded.value(
            EnchantmentCodec.decodeCatalog(
                registry(data, "enchantment"),
                registry(data, "enchantment_provider"),
                data.registryAccess()));
    assertThat(catalog.definitions()).hasSize(EnchantFixtures.definitions().size());
    assertThat(catalog.find(Identifier.minecraft("sharpness"))).isPresent();
    assertThat(catalog.provider(Identifier.minecraft("test_single"))).isPresent();
  }

  @Test
  void anEnchantmentThatFailsIsAbsentAndListed() {
    // spec: EnchantmentCodec#decodeCatalog contract
    Map<String, String> files = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    files.put("enchantment/broken", "{'max_level':1}");
    LoadedData data = load(files);
    LootDecoding<EnchantmentCatalog> decoding =
        EnchantmentCodec.decodeCatalog(
            registry(data, "enchantment"),
            registry(data, "enchantment_provider"),
            data.registryAccess());
    assertThat(Decoded.codes(decoding)).contains(LootFindingCode.MISSING_FIELD);
    if (decoding instanceof LootDecoding.Success<EnchantmentCatalog> success) {
      assertThat(success.value().find(Identifier.minecraft("broken"))).isEmpty();
      assertThat(success.value().find(Identifier.minecraft("sharpness"))).isPresent();
    }
  }
}
