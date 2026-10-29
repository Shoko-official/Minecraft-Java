package dev.shoko.minecraft.loot.enchant;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R1 (registry), EN-R6 (slot groups), EN-R8 (exclusive sets, symmetric), EN-R9a (curse
// tag), V-E9 (members of an exclusive set)
class EnchantmentCatalogTest {

  private static EnchantmentCatalog catalog() {
    return EnchantFixtures.catalog();
  }

  private static RegistryAccess access() {
    return EnchantFixtures.access();
  }

  private static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  private static boolean compatible(String first, String second) {
    return catalog().areCompatible(id(first), id(second), access());
  }

  @Test
  void aLoadedDefinitionIsFoundByIdentifier() {
    // spec: EN-R1
    assertThat(catalog().find(id("sharpness"))).isPresent();
    assertThat(catalog().find(id("sharpness")).orElseThrow().id()).isEqualTo(id("sharpness"));
    assertThat(catalog().find(id("no_such_enchantment"))).isEmpty();
  }

  @Test
  void aLoadedProviderIsFoundByIdentifier() {
    // spec: EN-R34
    assertThat(catalog().provider(id("test_single"))).isPresent();
    assertThat(catalog().provider(id("no_such_provider"))).isEmpty();
  }

  @Test
  void membersOfATagAreListedInIdentifierOrder() {
    // spec: EnchantmentCatalog#members contract, V-E9 (damage set)
    List<EnchantmentDefinition> members =
        catalog().members(Sets.enchantmentTag("exclusive_set/damage"), access());
    assertThat(members.stream().map(d -> d.id().path()).toList())
        .containsExactly("bane_of_arthropods", "sharpness", "smite");
  }

  @Test
  void membersSkipAnIdentifierThatIsNotLoaded() {
    // spec: EnchantmentCatalog#members contract
    List<EnchantmentDefinition> members =
        catalog().members(Sets.enchantments("sharpness", "no_such_enchantment"), access());
    assertThat(members.stream().map(d -> d.id().path()).toList()).containsExactly("sharpness");
  }

  @Test
  void membersOfAnUnrestrictedSetAreEveryDefinition() {
    // spec: IdOrTagSet contract (options absent: all enchantments), EN-R36
    assertThat(catalog().members(Sets.anyEnchantment(), access()))
        .hasSize(catalog().definitions().size());
  }

  @Test
  void provisionalAnEnchantmentIsCompatibleWithItself() {
    // spec: EnchantmentCatalog#areCompatible contract
    assertThat(compatible("sharpness", "sharpness")).isTrue();
  }

  @Test
  void provisionalEnchantmentsOfAnExclusiveSetExcludeEachOther() {
    // spec: EN-R8
    assertThat(compatible("sharpness", "smite")).isFalse();
    assertThat(compatible("smite", "sharpness")).isFalse();
    assertThat(compatible("protection", "fire_protection")).isFalse();
    assertThat(compatible("fortune", "silk_touch")).isFalse();
  }

  @Test
  void provisionalEnchantmentsOfDifferentSetsAreCompatible() {
    // spec: EN-R8
    assertThat(compatible("sharpness", "protection")).isTrue();
    assertThat(compatible("unbreaking", "mending")).isTrue();
    assertThat(compatible("fortune", "sharpness")).isTrue();
  }

  @Test
  void provisionalTheExclusionIsSymmetricWhenOnlyOneSideDeclaresIt() {
    // spec: EN-R8
    assertThat(compatible("infinity", "mending")).isFalse();
    assertThat(compatible("mending", "infinity")).isFalse();
  }

  @Test
  void provisionalRiptideExcludesLoyaltyAndChannelingFromBothSides() {
    // spec: EN-R8
    assertThat(compatible("riptide", "loyalty")).isFalse();
    assertThat(compatible("loyalty", "riptide")).isFalse();
    assertThat(compatible("riptide", "channeling")).isFalse();
    assertThat(compatible("channeling", "riptide")).isFalse();
  }

  @Test
  void provisionalLoyaltyAndChannelingDoNotExcludeEachOther() {
    // spec: EN-R8
    assertThat(compatible("loyalty", "channeling")).isTrue();
    assertThat(compatible("channeling", "loyalty")).isTrue();
  }

  @Test
  void aCurseIsAMemberOfTheCurseTag() {
    // spec: EN-R9a, EnchantmentCatalog#isCurse contract
    assertThat(catalog().isCurse(id("binding_curse"), access())).isTrue();
    assertThat(catalog().isCurse(id("sharpness"), access())).isFalse();
    assertThat(catalog().isCurse(id("mending"), access())).isFalse();
  }

  @Test
  void provisionalASlotGroupCoversTheConcreteSlotsItNames() {
    // spec: EN-R6
    assertThat(SlotGroup.MAINHAND.contains(EquipmentSlot.MAINHAND)).isTrue();
    assertThat(SlotGroup.MAINHAND.contains(EquipmentSlot.OFFHAND)).isFalse();
    assertThat(SlotGroup.OFFHAND.contains(EquipmentSlot.OFFHAND)).isTrue();
    assertThat(SlotGroup.HEAD.contains(EquipmentSlot.HEAD)).isTrue();
    assertThat(SlotGroup.HEAD.contains(EquipmentSlot.FEET)).isFalse();
    assertThat(SlotGroup.FEET.contains(EquipmentSlot.FEET)).isTrue();
    assertThat(SlotGroup.CHEST.contains(EquipmentSlot.CHEST)).isTrue();
    assertThat(SlotGroup.LEGS.contains(EquipmentSlot.LEGS)).isTrue();
  }

  @Test
  void provisionalTheHandGroupCoversBothHands() {
    // spec: EN-R6
    assertThat(SlotGroup.HAND.contains(EquipmentSlot.MAINHAND)).isTrue();
    assertThat(SlotGroup.HAND.contains(EquipmentSlot.OFFHAND)).isTrue();
    assertThat(SlotGroup.HAND.contains(EquipmentSlot.HEAD)).isFalse();
  }

  @Test
  void provisionalTheArmorGroupCoversTheFourArmorSlots() {
    // spec: EN-R6
    for (EquipmentSlot slot :
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
      assertThat(SlotGroup.ARMOR.contains(slot)).as(slot.name()).isTrue();
    }
    assertThat(SlotGroup.ARMOR.contains(EquipmentSlot.MAINHAND)).isFalse();
    assertThat(SlotGroup.ARMOR.contains(EquipmentSlot.OFFHAND)).isFalse();
  }

  @Test
  void provisionalTheAnyGroupCoversEveryHandAndArmorSlot() {
    // spec: EN-R6
    for (EquipmentSlot slot :
        List.of(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET)) {
      assertThat(SlotGroup.ANY.contains(slot)).as(slot.name()).isTrue();
    }
  }
}
