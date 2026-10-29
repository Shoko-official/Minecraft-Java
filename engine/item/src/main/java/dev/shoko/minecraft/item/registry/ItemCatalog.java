package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentCatalog;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;

/**
 * All items and their prototypes, loaded from the reports (IC-R1 to IC-R5). The reference data has
 * 1658 items, each with a prototype table. Immutable once loaded.
 */
public final class ItemCatalog {

  /** Key of the {@code minecraft:item} registry, for {@code Holder} and {@code HolderSet}. */
  public static final RegistryKey<ItemType> REGISTRY =
      new RegistryKey<>(Identifier.minecraft("item"));

  private final ComponentCatalog components;
  private final Registry<ItemType> registry;
  private final ItemType air;

  private ItemCatalog(ComponentCatalog components, Registry<ItemType> registry, ItemType air) {
    this.components = components;
    this.registry = registry;
    this.air = air;
  }

  /**
   * Loads and verifies the catalog. Each prototype table is decoded with the component registry of
   * the source; a table that fails to decode fails the load, with the item named in the path of the
   * finding.
   *
   * @param source the reports
   * @return the catalog, or the failure when an invariant is broken: the default entry is not the
   *     first item or is missing, an identifier is duplicated, a prototype breaks IC-R5 ({@code
   *     max_damage} present requires {@code max_stack_size} 1, {@code damage} requires {@code
   *     max_damage}), all as {@code INCONSISTENT_PROTOTYPES}
   */
  public static Decoding<ItemCatalog> load(ItemReportSource source) {
    ComponentCatalog components;
    try {
      components = ComponentCatalog.of(source.componentTypeIds());
    } catch (IllegalArgumentException e) {
      return failure(List.of(inconsistent("$", e.getMessage())));
    }
    List<ItemReportEntry> entries = source.items();
    List<Finding> findings = new ArrayList<>(checkRegistryShape(source.defaultItem(), entries));
    if (!findings.isEmpty()) {
      return failure(findings);
    }
    ComponentCodec codec = ComponentCodec.of(components);
    SequencedMap<Identifier, ItemType> items = new LinkedHashMap<>();
    for (ItemReportEntry entry : entries) {
      decodePrototype(codec, entry, items.size(), findings).ifPresent(t -> items.put(t.id(), t));
    }
    if (!findings.isEmpty()) {
      return failure(findings);
    }
    Registry<ItemType> registry = Registry.of(REGISTRY, items, source.defaultItem());
    ItemCatalog catalog = new ItemCatalog(components, registry, items.get(source.defaultItem()));
    return new Decoding.Success<>(catalog, List.of());
  }

  private static List<Finding> checkRegistryShape(
      Identifier defaultItem, List<ItemReportEntry> entries) {
    List<Finding> findings = new ArrayList<>();
    if (entries.isEmpty() || !entries.getFirst().id().equals(defaultItem)) {
      findings.add(inconsistent("$", "the default item " + defaultItem + " is not the first item"));
    }
    // Membership only: the set is never iterated.
    Set<Identifier> seen = new HashSet<>();
    for (ItemReportEntry entry : entries) {
      if (!seen.add(entry.id())) {
        findings.add(inconsistent(entry.id().toString(), "duplicated item"));
      }
    }
    return findings;
  }

  private static Optional<ItemType> decodePrototype(
      ComponentCodec codec, ItemReportEntry entry, int rawId, List<Finding> findings) {
    Decoding<ComponentMap> decoded = codec.tableFromJson(entry.components());
    if (!(decoded instanceof Decoding.Success<ComponentMap> success)) {
      for (Finding finding : decoded.findings()) {
        findings.add(new Finding(finding.code(), entry.id() + finding.path(), finding.message()));
      }
      return Optional.empty();
    }
    Optional<String> broken = brokenInvariant(success.value());
    if (broken.isPresent()) {
      findings.add(inconsistent(entry.id().toString(), broken.get()));
      return Optional.empty();
    }
    return Optional.of(new ItemType(entry.id(), rawId, success.value()));
  }

  /** IC-R5, V-I9: a durable item stacks to one, and damage needs a durability. */
  private static Optional<String> brokenInvariant(ComponentMap prototype) {
    boolean durable = prototype.contains(WellKnownComponents.MAX_DAMAGE);
    if (durable) {
      boolean single =
          prototype
              .get(WellKnownComponents.MAX_STACK_SIZE)
              .filter(value -> value.tag().equals(new IntTag(1)))
              .isPresent();
      if (!single) {
        return Optional.of("max_damage requires max_stack_size 1");
      }
    }
    if (prototype.contains(WellKnownComponents.DAMAGE) && !durable) {
      return Optional.of("damage requires max_damage");
    }
    return Optional.empty();
  }

  private static Finding inconsistent(String path, String message) {
    return new Finding(FindingCode.INCONSISTENT_PROTOTYPES, path, message);
  }

  private static Decoding<ItemCatalog> failure(List<Finding> findings) {
    return new Decoding.Failure<>(List.copyOf(findings));
  }

  /**
   * Returns the component types.
   *
   * @return the catalog of the 122 component types
   */
  public ComponentCatalog components() {
    return components;
  }

  /**
   * Returns the items as a {@code data} registry, for the references of ingredients (the default
   * entry is air).
   *
   * @return the registry
   */
  public Registry<ItemType> registry() {
    return registry;
  }

  /**
   * Finds an item by identifier.
   *
   * @param id an item identifier
   * @return the item, or empty when it is not registered
   */
  public Optional<ItemType> find(Identifier id) {
    return registry.get(id);
  }

  /**
   * Returns the default entry (IC-R1).
   *
   * @return {@code minecraft:air}
   */
  public ItemType air() {
    return air;
  }

  /**
   * Returns the number of items.
   *
   * @return the size, 1658 for the reference data
   */
  public int size() {
    return registry.size();
  }
}
