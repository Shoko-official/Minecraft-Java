package dev.shoko.minecraft.loot.context;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R13 (registry context_key_set, 31 types in the order of their numeric ids),
// LT-R14b (parameter sets, provisional), LT-R2 (absent type reads as generic)
class LootContextTypesTest {

  private static final List<String> NAMES =
      List.of(
          "empty",
          "generic",
          "chest",
          "command",
          "command_slot_source",
          "command_compute_default",
          "command_compute_position",
          "command_compute_entity",
          "selector",
          "villager_trade",
          "fishing",
          "entity",
          "equipment",
          "archaeology",
          "gift",
          "barter",
          "vault",
          "advancement_reward",
          "advancement_entity",
          "advancement_location",
          "block_use",
          "block",
          "shearing",
          "entity_interact",
          "block_interact",
          "container_process",
          "enchanted_damage",
          "enchanted_item",
          "enchanted_location",
          "enchanted_entity",
          "hit_block");

  private static LootContextType type(String name) {
    return LootContextTypes.find(Identifier.minecraft(name)).orElseThrow();
  }

  @Test
  void thirtyOneTypesInTheOrderOfTheirNumericIds() {
    // spec: LT-R13
    List<LootContextType> all = LootContextTypes.all();
    assertThat(all).hasSize(31);
    assertThat(all.stream().map(t -> t.id().path()).toList()).isEqualTo(NAMES);
    for (int i = 0; i < all.size(); i++) {
      assertThat(all.get(i).rawId()).as(NAMES.get(i)).isEqualTo(i);
    }
  }

  @Test
  void everyTypeIsFoundByItsIdentifier() {
    // spec: LT-R13
    for (String name : NAMES) {
      assertThat(LootContextTypes.find(Identifier.minecraft(name))).as(name).isPresent();
    }
  }

  @Test
  void anIdentifierOutsideTheRegistryIsNotFound() {
    // spec: LT-R13 (a table with another type is refused at load time)
    assertThat(LootContextTypes.find(Identifier.minecraft("no_such_set"))).isEmpty();
    assertThat(LootContextTypes.find(Identifier.parse("other:chest"))).isEmpty();
  }

  @Test
  void genericIsTheTypeOfATableWithoutType() {
    // spec: LT-R2 (absent type reads as generic, provisional)
    assertThat(LootContextTypes.generic().id()).isEqualTo(Identifier.minecraft("generic"));
    assertThat(LootContextTypes.generic().rawId()).isEqualTo(1);
  }

  @Test
  void theRequiredParametersAreAmongTheAllowedOnes() {
    // spec: LT-R13 (record contract: allowed includes required)
    for (LootContextType type : LootContextTypes.all()) {
      assertThat(type.allowed()).as(type.id().path()).containsAll(type.required());
    }
  }

  @Test
  void provisionalABlockSetAllowsWhatBlockDropsRead() {
    // spec: LT-R14b
    assertThat(type("block").allowed())
        .contains(
            ContextKey.ORIGIN,
            ContextKey.BLOCK_STATE,
            ContextKey.TOOL,
            ContextKey.EXPLOSION_RADIUS,
            ContextKey.BLOCK_ENTITY,
            ContextKey.THIS_ENTITY);
  }

  @Test
  void provisionalAnEntitySetAllowsWhatMobDropsRead() {
    // spec: LT-R14b
    assertThat(type("entity").allowed())
        .contains(
            ContextKey.THIS_ENTITY,
            ContextKey.ATTACKER,
            ContextKey.DIRECT_ATTACKER,
            ContextKey.DAMAGE_SOURCE,
            ContextKey.ATTACKING_PLAYER);
  }

  @Test
  void provisionalAChestSetAllowsAnOriginAndAnEntity() {
    // spec: LT-R14b (origin for location_check, this_entity for the luck of the player)
    assertThat(type("chest").allowed()).contains(ContextKey.ORIGIN, ContextKey.THIS_ENTITY);
  }

  @Test
  void provisionalAFishingSetAllowsAnOriginAnEntityAndATool() {
    // spec: LT-R14b
    assertThat(type("fishing").allowed())
        .contains(ContextKey.ORIGIN, ContextKey.THIS_ENTITY, ContextKey.TOOL);
  }

  @Test
  void provisionalShearingAndGiftSetsAllowTheEntity() {
    // spec: LT-R14b
    assertThat(type("shearing").allowed()).contains(ContextKey.THIS_ENTITY);
    assertThat(type("gift").allowed()).contains(ContextKey.THIS_ENTITY);
  }

  @Test
  void provisionalABlockInteractSetAllowsTheBlockState() {
    // spec: LT-R14b
    assertThat(type("block_interact").allowed()).contains(ContextKey.BLOCK_STATE);
  }
}
