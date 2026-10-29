package dev.shoko.minecraft.item.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R1 to IC-R5, V-I1 to V-I4, V-I6, V-I9: the catalog loaded
// from a small made-up item report that has the shapes of the reference data.
class ItemCatalogTest {

  private static ItemCatalog loaded() {
    return TestItems.catalog();
  }

  private static ComponentValue prototypeValue(String item, String component) {
    return TestItems.type(item).prototype().get(Identifier.minecraft(component)).orElseThrow();
  }

  @Test
  void aCleanReportLoadsWithoutFindings() {
    // spec: IC-R1, IC-R2
    Decoding<ItemCatalog> decoding = ItemCatalog.load(TestItems.source());
    assertThat(Findings.value(decoding)).isNotNull();
    assertThat(decoding.findings()).isEmpty();
  }

  @Test
  void theCatalogHoldsEveryItemOfTheReport() {
    // spec: IC-R1
    assertThat(loaded().size()).isEqualTo(TestItems.itemPaths().size());
    for (String path : TestItems.itemPaths()) {
      assertThat(loaded().find(Identifier.minecraft(path))).as(path).isPresent();
    }
  }

  @Test
  void rawIdsFollowTheOrderOfTheReport() {
    // spec: IC-R1 (entry i of the report has raw id i)
    List<String> paths = TestItems.itemPaths();
    for (int i = 0; i < paths.size(); i++) {
      ItemType type = loaded().find(Identifier.minecraft(paths.get(i))).orElseThrow();
      assertThat(type.rawId()).as(paths.get(i)).isEqualTo(i);
      assertThat(type.id()).isEqualTo(Identifier.minecraft(paths.get(i)));
    }
  }

  @Test
  void theDefaultEntryIsAirAtRawIdZero() {
    // spec: IC-R1
    ItemType air = loaded().air();
    assertThat(air.id()).isEqualTo(Identifier.minecraft("air"));
    assertThat(air.rawId()).isZero();
    assertThat(air.isAir()).isTrue();
    assertThat(loaded().registry().defaultId()).contains(Identifier.minecraft("air"));
  }

  @Test
  void theRegistryListsTheItemsInOrderWithTheirTypes() {
    // spec: IC-R1 (the data registry used by ingredient references)
    List<Identifier> expected = TestItems.itemPaths().stream().map(Identifier::minecraft).toList();
    assertThat(loaded().registry().ids()).containsExactlyElementsOf(expected);
    assertThat(loaded().registry().size()).isEqualTo(expected.size());
    for (Identifier id : expected) {
      assertThat(loaded().registry().get(id)).isEqualTo(loaded().find(id));
    }
  }

  @Test
  void theRegistryKeyIsTheItemRegistry() {
    // spec: IC-R1
    assertThat(ItemCatalog.REGISTRY.name()).isEqualTo(Identifier.minecraft("item"));
  }

  @Test
  void anUnknownItemIsNotFound() {
    // spec: IC-R1
    assertThat(loaded().find(Identifier.minecraft("not_an_item"))).isEmpty();
    assertThat(loaded().find(Identifier.of("other", "stone"))).isEmpty();
  }

  @Test
  void theComponentCatalogIsTheOneOfTheReport() {
    // spec: IC-R9
    assertThat(loaded().components().size()).isEqualTo(TestItems.COMPONENT_IDS.size());
    assertThat(loaded().components().types().stream().map(t -> t.id()).toList())
        .containsExactlyElementsOf(TestItems.COMPONENT_IDS);
  }

  @Test
  void everyPrototypeHasTheThirteenUniversalComponents() {
    // spec: IC-R3 (the fake items follow the rule; the real count is in the oracle test)
    for (String path : TestItems.itemPaths()) {
      for (String component : TestItems.UNIVERSAL) {
        assertThat(TestItems.type(path).prototype().contains(Identifier.minecraft(component)))
            .as("%s %s", path, component)
            .isTrue();
      }
    }
  }

  @Test
  void aPrototypeKeepsTheOrderOfItsFile() {
    // spec: IC-R2 (components "in file order")
    for (var entry : TestItems.source().items()) {
      List<Identifier> fileOrder =
          ((JsonObject) entry.components())
              .members().keySet().stream().map(Identifier::parse).toList();
      assertThat(loaded().find(entry.id()).orElseThrow().prototype().entries().keySet())
          .as(entry.id().toString())
          .containsExactlyElementsOf(fileOrder);
    }
  }

  @Test
  void prototypesAreReadFromTheReportNotCodedIn() {
    // spec: IC-R2
    var odd = TestItems.entry("odd_block", 7, Map.of("rarity", "'epic'"));
    ItemCatalog catalog = Findings.value(ItemCatalog.load(TestItems.source().plusItem(odd)));
    ItemType type = catalog.find(Identifier.minecraft("odd_block")).orElseThrow();
    assertThat(type.rawId()).isEqualTo(catalog.size() - 1);
    assertThat(type.prototype().get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(TestItems.value("max_stack_size", "7"));
    assertThat(type.prototype().get(Identifier.minecraft("rarity")))
        .contains(TestItems.value("rarity", "'epic'"));
  }

  @Test
  void airIsAnOrdinaryPrototypeWithThirteenComponents() {
    // spec: V-I1
    var air = loaded().air().prototype();
    assertThat(air.entries()).hasSize(13);
    assertThat(air.get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(TestItems.value("max_stack_size", "64"));
    assertThat(prototypeValue("air", "item_name"))
        .isEqualTo(TestItems.value("item_name", "{'translate':'block.minecraft.air'}"));
  }

  @Test
  void aPickaxeHasDurabilityAndStacksToOne() {
    // spec: V-I2, IC-R5
    var proto = TestItems.type("stone_pickaxe").prototype();
    assertThat(proto.get(WellKnownComponents.MAX_STACK_SIZE))
        .contains(TestItems.value("max_stack_size", "1"));
    assertThat(proto.get(WellKnownComponents.MAX_DAMAGE))
        .contains(TestItems.value("max_damage", "131"));
    assertThat(proto.get(WellKnownComponents.DAMAGE)).contains(TestItems.value("damage", "0"));
    assertThat(prototypeValue("stone_pickaxe", "enchantable"))
        .isEqualTo(TestItems.value("enchantable", "{'value':5}"));
    assertThat(prototypeValue("stone_pickaxe", "repairable"))
        .isEqualTo(TestItems.value("repairable", "{'items':'#minecraft:stone_tool_materials'}"));
    assertThat(prototypeValue("stone_pickaxe", "weapon"))
        .isEqualTo(TestItems.value("weapon", "{'item_damage_per_attack':2}"));
  }

  @Test
  void aPrototypeOverridesTheDefaultOfAUniversalComponent() {
    // spec: V-I2 (attribute_modifiers is universal and non empty for the pickaxe)
    assertThat(prototypeValue("stone_pickaxe", "attribute_modifiers"))
        .isNotEqualTo(prototypeValue("stone", "attribute_modifiers"));
  }

  @Test
  void breadIsFoodAndConsumable() {
    // spec: V-I3
    assertThat(prototypeValue("bread", "food"))
        .isEqualTo(TestItems.value("food", "{'nutrition':5,'saturation':6.0}"));
    assertThat(TestItems.type("bread").prototype().contains(Identifier.minecraft("consumable")))
        .isTrue();
    assertThat(prototypeValue("bread", "villager_food"))
        .isEqualTo(TestItems.value("villager_food", "{'nutrition':4}"));
  }

  @Test
  void milkBucketLeavesABucket() {
    // spec: V-I4
    assertThat(prototypeValue("milk_bucket", "use_remainder"))
        .isEqualTo(TestItems.value("use_remainder", "{'id':'minecraft:bucket'}"));
  }

  @Test
  void aBannerHasNoPatternsAndStacksToSixteen() {
    // spec: V-I6
    assertThat(prototypeValue("white_banner", "banner_patterns"))
        .isEqualTo(TestItems.value("banner_patterns", "[]"));
    assertThat(prototypeValue("white_banner", "max_stack_size"))
        .isEqualTo(TestItems.value("max_stack_size", "16"));
  }

  @Test
  void anAbsentComponentIsNotInThePrototype() {
    // spec: IC-R7 (damage is carried by durable items only)
    assertThat(TestItems.type("stone").prototype().contains(WellKnownComponents.DAMAGE)).isFalse();
    assertThat(TestItems.type("stone").prototype().get(WellKnownComponents.MAX_DAMAGE)).isEmpty();
  }

  @Test
  void aDamageableItemOfStackSizeOneIsConsistent() {
    // spec: IC-R5, V-I9 (the invariant holds)
    var tool = TestItems.entry("extra_tool", 1, Map.of("max_damage", "10", "damage", "0"));
    assertThat(ItemCatalog.load(TestItems.source().plusItem(tool)))
        .isInstanceOf(Decoding.Success.class);
  }

  @Test
  void maxDamageWithAStackSizeAboveOneIsRefused() {
    // spec: IC-R5, V-I9
    var bad = TestItems.entry("bad_tool", 64, Map.of("max_damage", "10"));
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().plusItem(bad)), FindingCode.INCONSISTENT_PROTOTYPES);
    var sixteen = TestItems.entry("bad_tool", 16, Map.of("max_damage", "10"));
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().plusItem(sixteen)),
        FindingCode.INCONSISTENT_PROTOTYPES);
  }

  @Test
  void damageWithoutMaxDamageIsRefused() {
    // spec: IC-R5, V-I9
    var bad = TestItems.entry("bad_tool", 1, Map.of("damage", "0"));
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().plusItem(bad)), FindingCode.INCONSISTENT_PROTOTYPES);
  }

  @Test
  void aDefaultItemOutsideTheReportIsRefused() {
    // spec: IC-R1
    var source = TestItems.source().withDefault(Identifier.minecraft("nothing"));
    Findings.assertFailure(ItemCatalog.load(source), FindingCode.INCONSISTENT_PROTOTYPES);
  }

  @Test
  void aDefaultItemThatIsNotFirstIsRefused() {
    // spec: IC-R1 (the default entry is the first item)
    List<ItemReportEntry> items = new ArrayList<>(TestItems.source().items());
    ItemReportEntry first = items.get(0);
    items.set(0, items.get(1));
    items.set(1, first);
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().withItems(items)), FindingCode.INCONSISTENT_PROTOTYPES);
  }

  @Test
  void aDuplicatedIdentifierIsRefused() {
    // spec: IC-R1
    var duplicate = TestItems.entry("stone", 64);
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().plusItem(duplicate)),
        FindingCode.INCONSISTENT_PROTOTYPES);
  }

  @Test
  void provisionalAPrototypeWithAnUnregisteredComponentFailsTheLoadNamingTheItem() {
    // spec: IC-R10 (provisional), ItemCatalog.load: the item is named in the finding path
    List<Identifier> withoutFood =
        TestItems.COMPONENT_IDS.stream().filter(id -> !id.path().equals("food")).toList();
    var source = TestItems.source().withComponentTypes(withoutFood);
    Decoding<ItemCatalog> decoding = ItemCatalog.load(source);
    Findings.assertFailure(decoding, FindingCode.UNKNOWN_COMPONENT);
    assertThat(decoding.findings()).anyMatch(f -> f.path().contains("bread"));
  }

  @Test
  void provisionalAPrototypeWithAnUnknownFieldFailsTheLoad() {
    // spec: IC-R12 (provisional rejection of unknown fields)
    var bad =
        TestItems.entry(
            "bad_food",
            64,
            Map.of("food", "{'nutrition':1,'saturation':1.0," + "'invented':true}"));
    Findings.assertFailure(
        ItemCatalog.load(TestItems.source().plusItem(bad)), FindingCode.UNKNOWN_FIELD);
  }
}
