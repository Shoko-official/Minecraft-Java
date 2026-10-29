package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;

/**
 * Port: the content of the extracted reports that this module needs (IC-R1, IC-R2, IC-R9): the
 * {@code item} and {@code data_component_type} registries of {@code registries.json} joined with
 * the 1658 prototype files.
 *
 * <p>The report is passed unchecked: {@link ItemCatalog#load(ItemReportSource)} verifies it.
 */
public interface ItemReportSource {

  /**
   * Returns the identifiers of the {@code data_component_type} registry in registry order.
   *
   * @return the identifiers, entry {@code i} having raw id {@code i}
   */
  List<Identifier> componentTypeIds();

  /**
   * Returns the default entry of the {@code item} registry.
   *
   * @return the identifier, {@code minecraft:air} in the reference data
   */
  Identifier defaultItem();

  /**
   * Returns every item in the order of the {@code item} registry.
   *
   * @return the items, entry {@code i} having raw id {@code i}
   */
  List<ItemReportEntry> items();
}
