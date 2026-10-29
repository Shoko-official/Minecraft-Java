package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R2, IC-R6b, IC-R10: decoding the whole component table of a
// prototype.
class ComponentCodecTableTest {

  private static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  private static ComponentMap table(String relaxedJson) {
    return Findings.value(CodecFixture.codec().tableFromJson(Json.parse(relaxedJson)));
  }

  @Test
  void theTableKeepsTheOrderOfTheFile() {
    // spec: IC-R2 ("in file order")
    ComponentMap forward =
        table("{'minecraft:rarity':'common','minecraft:damage':0,'minecraft:max_damage':5}");
    assertThat(forward.entries().keySet())
        .containsExactly(id("rarity"), id("damage"), id("max_damage"));
    ComponentMap backward =
        table("{'minecraft:max_damage':5,'minecraft:damage':0,'minecraft:rarity':'common'}");
    assertThat(backward.entries().keySet())
        .containsExactly(id("max_damage"), id("damage"), id("rarity"));
    assertThat(backward).isEqualTo(forward);
  }

  @Test
  void anEmptyObjectIsAnEmptyTable() {
    // spec: IC-R2
    assertThat(table("{}").entries()).isEmpty();
  }

  private static String uniqueKeys(List<CodecFixture.Sample> samples) {
    StringBuilder text = new StringBuilder("{");
    Set<String> seen = new HashSet<>();
    for (CodecFixture.Sample sample : samples) {
      if (seen.add(sample.component())) {
        text.append(text.length() > 1 ? "," : "")
            .append("'minecraft:")
            .append(sample.component())
            .append("':")
            .append(sample.json());
      }
    }
    return text.append('}').toString();
  }

  @Test
  void aTableOfEverySampleDecodesEachValueAsDecodedAlone() {
    // spec: IC-R2: the table decoding and the value decoding agree
    ComponentCodec codec = CodecFixture.codec();
    List<CodecFixture.Sample> samples = CodecFixture.samples();
    ComponentMap decoded = Findings.value(codec.tableFromJson(Json.parse(uniqueKeys(samples))));
    Set<String> seen = new HashSet<>();
    for (CodecFixture.Sample sample : samples) {
      if (seen.add(sample.component())) {
        assertThat(decoded.get(id(sample.component())))
            .as("%s", sample)
            .contains(CodecFixture.decode(codec, sample.component(), sample.json()));
      }
    }
    assertThat(decoded.entries()).hasSize(seen.size());
  }

  @Test
  void theFakePrototypeOfAPickaxeDecodesToItsComponents() {
    // spec: V-I2, IC-R2
    ComponentMap proto = TestItems.type("stone_pickaxe").prototype();
    assertThat(proto.contains(id("tool"))).isTrue();
    assertThat(proto.contains(id("weapon"))).isTrue();
    assertThat(proto.contains(id("food"))).isFalse();
  }

  @Test
  void provisionalAnEmptyObjectValueIsAPresentComponent() {
    // spec: IC-R6b (provisional): {} is significant and differs from an absent component
    ComponentMap map = table("{'minecraft:consumable':{},'minecraft:rarity':'common'}");
    assertThat(map.contains(id("consumable"))).isTrue();
    assertThat(map.get(id("consumable"))).isPresent();
    assertThat(map).isNotEqualTo(table("{'minecraft:rarity':'common'}"));
  }

  @Test
  void provisionalAnUnregisteredIdentifierFailsTheWholeTable() {
    // spec: IC-R10 (provisional): never ignored silently
    ComponentCodec codec = CodecFixture.codec();
    for (String text :
        new String[] {
          "{'minecraft:rarity':'common','minecraft:not_a_component':{}}",
          "{'minecraft:not_a_component':{},'minecraft:rarity':'common'}",
          "{'other:rarity':'common'}"
        }) {
      Findings.assertFailure(codec.tableFromJson(Json.parse(text)), FindingCode.UNKNOWN_COMPONENT);
    }
  }

  @Test
  void provisionalABadValueFailsTheWholeTableWithTheCodeOfTheValue() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.tableFromJson(
            Json.parse("{'minecraft:rarity':'common','minecraft:food':{'nutrition':5}}")),
        FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAKeyPrefixedByAnExclamationMarkIsNotAllowedInAFullTable() {
    // spec: ComponentCodec.tableFromJson ("with no key prefixed by an exclamation mark")
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertAnyFailure(codec.tableFromJson(Json.parse("{'!minecraft:rarity':{}}")));
  }

  @Test
  void provisionalAnythingButAnObjectIsNotATable() {
    // spec: ComponentCodec.tableFromJson (provisional)
    ComponentCodec codec = CodecFixture.codec();
    for (String text : new String[] {"[]", "'minecraft:rarity'", "5", "true", "null"}) {
      Findings.assertAnyFailure(codec.tableFromJson(Json.parse(text)));
    }
  }

  @Test
  void provisionalAKeyThatIsNotAnIdentifierIsRefused() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertAnyFailure(codec.tableFromJson(Json.parse("{'Not An Id':1}")));
  }
}
