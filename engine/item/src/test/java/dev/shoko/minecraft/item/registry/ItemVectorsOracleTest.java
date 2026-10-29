package dev.shoko.minecraft.item.registry;

import static dev.shoko.minecraft.item.registry.OracleCatalog.prototype;
import static dev.shoko.minecraft.item.registry.OracleCatalog.report;
import static dev.shoko.minecraft.item.registry.OracleCatalog.value;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Vectors V-I1 to V-I6 of the spec on the extracted reference data (item prototype files). Reads
 * the local cache, copies nothing, and skips itself when the cache is absent.
 */
@Tag("oracle")
@Timeout(300)
class ItemVectorsOracleTest {

  @Test
  void airHasThirteenComponents() {
    // spec: V-I1
    assertThat(prototype("air").entries()).hasSize(13);
    assertThat(prototype("air").get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(value("max_stack_size", "64"));
    assertThat(prototype("air").get(Identifier.minecraft("item_name")))
        .contains(value("item_name", "{'translate':'block.minecraft.air'}"));
  }

  @Test
  void theStonePickaxeMatchesTheVector() {
    // spec: V-I2
    ComponentMap proto = prototype("stone_pickaxe");
    assertThat(proto.get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(value("max_stack_size", "1"));
    assertThat(proto.get(WellKnownComponents.MAX_DAMAGE)).contains(value("max_damage", "131"));
    assertThat(proto.get(WellKnownComponents.DAMAGE)).contains(value("damage", "0"));
    assertThat(proto.get(Identifier.minecraft("enchantable")))
        .contains(value("enchantable", "{'value':5}"));
    assertThat(proto.get(Identifier.minecraft("repairable")))
        .contains(value("repairable", "{'items':'#minecraft:stone_tool_materials'}"));
    assertThat(proto.get(Identifier.minecraft("weapon")))
        .contains(value("weapon", "{'item_damage_per_attack':2}"));
    assertThat(proto.get(Identifier.minecraft("tool")))
        .contains(
            value(
                "tool",
                "{'rules':[{'blocks':'#minecraft:incorrect_for_stone_tool',"
                    + "'correct_for_drops':false},{'blocks':'#minecraft:mineable/pickaxe',"
                    + "'correct_for_drops':true,'speed':4.0}]}"));
  }

  @Test
  void breadAndGoldenAppleAreFood() {
    // spec: V-I3
    assertThat(prototype("bread").get(Identifier.minecraft("food")))
        .contains(value("food", "{'nutrition':5,'saturation':6.0}"));
    assertThat(prototype("bread").contains(Identifier.minecraft("consumable"))).isTrue();
    assertThat(prototype("bread").get(Identifier.minecraft("villager_food")))
        .contains(value("villager_food", "{'nutrition':4}"));
    assertThat(prototype("golden_apple").contains(Identifier.minecraft("food"))).isTrue();
    assertThat(prototype("golden_apple").contains(Identifier.minecraft("consumable"))).isTrue();
  }

  @Test
  void sevenItemsLeaveARemainder() {
    // spec: V-I4, RC-R22
    Map<String, String> expected = new TreeMap<>();
    expected.put("beetroot_soup", "bowl");
    expected.put("honey_bottle", "glass_bottle");
    expected.put("milk_bucket", "bucket");
    expected.put("mushroom_stew", "bowl");
    expected.put("potion", "glass_bottle");
    expected.put("rabbit_stew", "bowl");
    expected.put("suspicious_stew", "bowl");
    Map<String, String> actual = new TreeMap<>();
    for (ItemReportEntry entry : report().items()) {
      prototype(entry.id().path())
          .get(WellKnownComponents.USE_REMAINDER)
          .ifPresent(
              v -> actual.put(entry.id().path(), expected.getOrDefault(entry.id().path(), "?")));
    }
    assertThat(actual).isEqualTo(expected);
    expected.forEach(
        (item, remainder) ->
            assertThat(prototype(item).get(WellKnownComponents.USE_REMAINDER))
                .as(item)
                .contains(value("use_remainder", "{'id':'minecraft:" + remainder + "'}")));
  }

  @Test
  void theDecoratedPotCarriesFourBricks() {
    // spec: V-I5
    String brick = "{'id':'minecraft:brick'}";
    assertThat(prototype("decorated_pot").get(Identifier.minecraft("pot_decorations")))
        .contains(
            value(
                "pot_decorations",
                "{'back':"
                    + brick
                    + ",'front':"
                    + brick
                    + ",'left':"
                    + brick
                    + ",'right':"
                    + brick
                    + "}"));
    assertThat(prototype("decorated_pot").get(WellKnownComponents.CONTAINER))
        .contains(value("container", "[]"));
  }

  @Test
  void theWhiteBannerHasNoPatternAndStacksToSixteen() {
    // spec: V-I6
    assertThat(prototype("white_banner").get(Identifier.minecraft("banner_patterns")))
        .contains(value("banner_patterns", "[]"));
    assertThat(prototype("white_banner").get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(value("max_stack_size", "16"));
  }
}
