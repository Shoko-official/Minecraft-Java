package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R9, IC-R10: the registered component types are the
// acceptance list of the decoding.
class ComponentCatalogTest {

  private static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  @Test
  void theCatalogHasOneTypePerIdentifier() {
    // spec: IC-R9
    ComponentCatalog catalog = ComponentCatalog.of(TestItems.COMPONENT_IDS);
    assertThat(catalog.size()).isEqualTo(TestItems.COMPONENT_IDS.size());
    assertThat(catalog.types()).hasSize(TestItems.COMPONENT_IDS.size());
  }

  @Test
  void rawIdsFollowTheOrderOfTheRegistry() {
    // spec: IC-R9 (entry i has raw id i)
    List<Identifier> ids = List.of(id("rarity"), id("damage"), id("food"));
    ComponentCatalog catalog = ComponentCatalog.of(ids);
    assertThat(catalog.types())
        .containsExactly(
            new ComponentType(id("rarity"), 0),
            new ComponentType(id("damage"), 1),
            new ComponentType(id("food"), 2));
  }

  @Test
  void aRegisteredTypeIsFoundByIdentifier() {
    // spec: IC-R9
    ComponentCatalog catalog = ComponentCatalog.of(List.of(id("rarity"), id("damage")));
    assertThat(catalog.find(id("damage"))).contains(new ComponentType(id("damage"), 1));
  }

  @Test
  void anUnregisteredTypeIsNotFound() {
    // spec: IC-R9, IC-R10
    ComponentCatalog catalog = ComponentCatalog.of(List.of(id("rarity")));
    assertThat(catalog.find(id("food"))).isEmpty();
    assertThat(catalog.find(Identifier.of("other", "rarity"))).isEmpty();
  }

  @Test
  void anEmptyListGivesAnEmptyCatalog() {
    // spec: IC-R9
    ComponentCatalog catalog = ComponentCatalog.of(List.of());
    assertThat(catalog.size()).isZero();
    assertThat(catalog.types()).isEmpty();
    assertThat(catalog.find(id("rarity"))).isEmpty();
  }

  @Test
  void aDuplicatedIdentifierIsRefused() {
    // spec: ComponentCatalog.of (IllegalArgumentException)
    assertThatThrownBy(() -> ComponentCatalog.of(List.of(id("rarity"), id("damage"), id("rarity"))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theTypesCannotBeModifiedThroughTheReturnedList() {
    // spec: ComponentCatalog.types (unmodifiable)
    ComponentCatalog catalog = ComponentCatalog.of(List.of(id("rarity")));
    List<ComponentType> types = catalog.types();
    assertThatThrownBy(() -> types.add(new ComponentType(id("damage"), 1)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(catalog.size()).isEqualTo(1);
  }
}
