package dev.shoko.minecraft.item.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Item catalog against the extracted reference data (counts of IC-R1 to IC-R9, vectors V-I1 to
 * V-I9). Reads {@code generated/reports} from the local cache, copies nothing, and skips itself
 * when the cache is absent. Command that produces the counts: {@code python -I components_stats.py
 * "$REF"} (annex of the spec).
 */
@Tag("oracle")
@Timeout(300)
class ItemCatalogOracleTest {

  private static ItemReportSource report() {
    return OracleCatalog.report();
  }

  private static ItemCatalog catalog() {
    return OracleCatalog.catalog();
  }

  private static ComponentCodec codec() {
    return OracleCatalog.codec();
  }

  private static ComponentValue value(String component, String relaxedJson) {
    return OracleCatalog.value(component, relaxedJson);
  }

  private static ComponentMap prototype(String item) {
    return OracleCatalog.prototype(item);
  }

  private static Map<Identifier, Integer> presence() {
    Map<Identifier, Integer> counts = new TreeMap<>();
    for (ItemReportEntry entry : report().items()) {
      ((JsonObject) entry.components())
          .members()
          .keySet()
          .forEach(k -> counts.merge(Identifier.parse(k), 1, Integer::sum));
    }
    return counts;
  }

  @Test
  void theReferenceItemsLoadWithoutFindings() {
    // spec: IC-R1 (1658 items), IC-R2
    var decoding = ItemCatalog.load(report());
    ItemCatalog loaded = Findings.value(decoding);
    assertThat(decoding.findings()).isEmpty();
    assertThat(loaded.size()).isEqualTo(1658);
    assertThat(loaded.registry().size()).isEqualTo(1658);
  }

  @Test
  void airIsTheDefaultEntryAtRawIdZero() {
    // spec: IC-R1
    assertThat(catalog().air().id()).isEqualTo(Identifier.minecraft("air"));
    assertThat(catalog().air().rawId()).isZero();
    assertThat(catalog().registry().defaultId()).contains(Identifier.minecraft("air"));
  }

  @Test
  void rawIdsFollowTheOrderOfTheReport() {
    // spec: IC-R1
    List<ItemReportEntry> items = report().items();
    for (int i = 0; i < items.size(); i++) {
      ItemType type = catalog().find(items.get(i).id()).orElseThrow();
      assertThat(type.rawId()).as(type.id().toString()).isEqualTo(i);
    }
  }

  @Test
  void theCatalogHas122ComponentTypes() {
    // spec: IC-R9 (122 types), IC-R7 (69 seen in a prototype, 53 in none)
    assertThat(catalog().components().size()).isEqualTo(122);
    Map<Identifier, Integer> seen = presence();
    assertThat(seen).hasSize(69);
    long absent =
        catalog().components().types().stream().filter(t -> !seen.containsKey(t.id())).count();
    assertThat(absent).isEqualTo(53);
  }

  @Test
  void thirteenComponentsAreInEveryPrototype() {
    // spec: IC-R3, IC-R7: 23072 component entries
    Map<Identifier, Integer> counts = presence();
    List<Identifier> universal =
        counts.entrySet().stream()
            .filter(e -> e.getValue() == 1658)
            .map(Map.Entry::getKey)
            .toList();
    assertThat(universal)
        .containsExactlyInAnyOrderElementsOf(
            TestItems.UNIVERSAL.stream().map(Identifier::minecraft).toList());
    assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(23072);
    for (ItemType type :
        catalog().registry().ids().stream().map(i -> catalog().find(i).orElseThrow()).toList()) {
      for (Identifier id : universal) {
        assertThat(type.prototype().contains(id)).as("%s %s", type.id(), id).isTrue();
      }
    }
  }

  @Test
  void thePresenceOfSomeNonUniversalComponentsMatchesTheSpec() {
    // spec: IC-R7
    Map<Identifier, Integer> counts = presence();
    Map<String, Integer> expected = new TreeMap<>();
    expected.put("cooking_fuel", 347);
    expected.put("compostable", 122);
    expected.put("entity_data", 88);
    expected.put("equippable", 84);
    expected.put("damage", 84);
    expected.put("max_damage", 84);
    expected.put("enchantable", 77);
    expected.put("repairable", 75);
    expected.put("container", 45);
    expected.put("food", 44);
    expected.put("tool", 38);
    expected.put("use_remainder", 7);
    expected.put("villager_food", 4);
    expected.forEach(
        (name, count) ->
            assertThat(counts.get(Identifier.minecraft(name))).as(name).isEqualTo(count));
  }

  @Test
  void rarityIsMostlyCommon() {
    // spec: IC-R4: common 1543, uncommon 78, rare 18, epic 19
    Map<String, Integer> counts = new TreeMap<>();
    for (ItemReportEntry entry : report().items()) {
      ComponentValue rarity =
          prototype(entry.id().path()).get(Identifier.minecraft("rarity")).orElseThrow();
      counts.merge(((StringTag) rarity.tag()).value(), 1, Integer::sum);
    }
    assertThat(counts)
        .containsExactly(
            Map.entry("common", 1543),
            Map.entry("epic", 19),
            Map.entry("rare", 18),
            Map.entry("uncommon", 78));
  }

  @Test
  void stackSizesAre64Or16Or1() {
    // spec: IC-R5: 64 for 1342 items, 16 for 68, 1 for 248
    Map<ComponentValue, Integer> counts = new HashMap<>();
    for (ItemReportEntry entry : report().items()) {
      counts.merge(
          prototype(entry.id().path()).get(WellKnownComponents.MAX_STACK_SIZE).orElseThrow(),
          1,
          Integer::sum);
    }
    assertThat(counts).hasSize(3);
    assertThat(counts.get(value("max_stack_size", "64"))).isEqualTo(1342);
    assertThat(counts.get(value("max_stack_size", "16"))).isEqualTo(68);
    assertThat(counts.get(value("max_stack_size", "1"))).isEqualTo(248);
  }

  @Test
  void durableItemsStackToOne() {
    // spec: IC-R5, V-I9: max_damage implies max_stack_size 1; damage implies max_damage
    ComponentValue one = value("max_stack_size", "1");
    int durable = 0;
    for (ItemReportEntry entry : report().items()) {
      ComponentMap proto = prototype(entry.id().path());
      if (proto.contains(WellKnownComponents.MAX_DAMAGE)) {
        durable++;
        assertThat(proto.get(WellKnownComponents.MAX_STACK_SIZE))
            .as("%s", entry.id())
            .contains(one);
      }
      if (proto.contains(WellKnownComponents.DAMAGE)) {
        assertThat(proto.contains(WellKnownComponents.MAX_DAMAGE)).as("%s", entry.id()).isTrue();
      }
    }
    assertThat(durable).isEqualTo(84);
  }

  @Test
  void theBreakSoundIsTheDefaultOneExceptForTwoItems() {
    // spec: IC-R4: 1656 items, the shield and the wolf armor differ
    Identifier key = Identifier.minecraft("break_sound");
    ComponentValue normal = new ComponentValue(new StringTag("minecraft:entity.item.break"));
    int normals = 0;
    for (ItemReportEntry entry : report().items()) {
      if (prototype(entry.id().path()).get(key).orElseThrow().equals(normal)) {
        normals++;
      }
    }
    assertThat(normals).isEqualTo(1656);
    assertThat(prototype("shield").get(key))
        .contains(new ComponentValue(new StringTag("minecraft:item.shield.break")));
    assertThat(prototype("wolf_armor").get(key))
        .contains(new ComponentValue(new StringTag("minecraft:item.wolf_armor.break")));
  }

  @Test
  void theItemModelIsTheIdentifierOfTheItem() {
    // spec: IC-R4
    for (ItemReportEntry entry : report().items()) {
      assertThat(prototype(entry.id().path()).get(Identifier.minecraft("item_model")))
          .as("%s", entry.id())
          .contains(new ComponentValue(new StringTag(entry.id().toString())));
    }
  }

  @Test
  void theNameIsATranslationKeyWithABlockOrItemPrefix() {
    // spec: IC-R4: block for 1126 items, item for 532
    int blocks = 0;
    int items = 0;
    Identifier key = Identifier.minecraft("item_name");
    for (ItemReportEntry entry : report().items()) {
      String path = entry.id().path();
      ComponentValue name = prototype(path).get(key).orElseThrow();
      if (name.equals(value("item_name", "{'translate':'block.minecraft." + path + "'}"))) {
        blocks++;
      } else if (name.equals(value("item_name", "{'translate':'item.minecraft." + path + "'}"))) {
        items++;
      }
    }
    assertThat(blocks).isEqualTo(1126);
    assertThat(items).isEqualTo(532);
  }

  @Test
  void emptyObjectComponentsArePresent() {
    // spec: IC-R6a, IC-R6b: counts of the non universal components written {}
    Map<String, Integer> expected = new TreeMap<>();
    expected.put("consumable", 30);
    expected.put("map_decorations", 17);
    expected.put("weapon", 16);
    expected.put("bucket_entity_data", 7);
    expected.put("potion_contents", 4);
    expected.put("debug_stick_state", 1);
    expected.put("glider", 1);
    expected.put("stored_enchantments", 1);
    expected.put("writable_book_content", 1);
    expected.forEach(
        (name, count) -> {
          Identifier id = Identifier.minecraft(name);
          int empty = 0;
          for (ItemReportEntry entry : report().items()) {
            JsonNode node = ((JsonObject) entry.components()).get("minecraft:" + name).orElse(null);
            if (node instanceof JsonObject object && object.members().isEmpty()) {
              empty++;
              assertThat(prototype(entry.id().path()).contains(id))
                  .as("%s %s", entry.id(), name)
                  .isTrue();
            }
          }
          assertThat(empty).as(name).isEqualTo(count);
        });
  }

  @Test
  void eachPrototypeIsTheDecodingOfItsOwnFile() {
    // spec: IC-R2 (the engine loads the tables and does not code them in)
    ComponentCodec codec = codec();
    for (ItemReportEntry entry : report().items()) {
      ComponentMap decoded = Findings.value(codec.tableFromJson(entry.components()));
      ComponentMap proto = prototype(entry.id().path());
      assertThat(proto).as("%s", entry.id()).isEqualTo(decoded);
      assertThat(proto.entries().keySet()).containsExactlyElementsOf(decoded.entries().keySet());
    }
  }

  @Test
  void everyPrototypeValueSurvivesTheJsonAndNbtRoundTrips() {
    // spec: IC-R9, IC-R12, IC-R13, IC-R18 (the 69 types seen in a prototype)
    ComponentCodec codec = codec();
    for (ItemReportEntry entry : report().items()) {
      prototype(entry.id().path())
          .entries()
          .forEach(
              (id, value) -> {
                JsonNode written = Json.parseStrict(codec.valueToJson(id, value));
                assertThat(Findings.value(codec.valueFromJson(id, written)))
                    .as("%s %s json", entry.id(), id)
                    .isEqualTo(value);
                assertThat(Findings.value(codec.valueFromNbt(id, value.tag())))
                    .as("%s %s nbt", entry.id(), id)
                    .isEqualTo(value);
              });
    }
  }
}
