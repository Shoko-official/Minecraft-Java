package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentMap;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.ReferenceCache;

/**
 * The reference item report and catalog, read once per test run (skips when the cache is absent).
 */
final class OracleCatalog {

  private static ItemReportSource report;
  private static ItemCatalog catalog;

  private OracleCatalog() {}

  static synchronized ItemReportSource report() {
    if (report == null) {
      report = ReferenceCache.itemReport();
    }
    return report;
  }

  static synchronized ItemCatalog catalog() {
    if (catalog == null) {
      catalog = Findings.value(ItemCatalog.load(report()));
    }
    return catalog;
  }

  static ComponentCodec codec() {
    return ComponentCodec.of(catalog().components());
  }

  static ComponentValue value(String component, String relaxedJson) {
    return Findings.value(
        codec().valueFromJson(Identifier.minecraft(component), Json.parse(relaxedJson)));
  }

  static ComponentMap prototype(String item) {
    return catalog().find(Identifier.minecraft(item)).orElseThrow().prototype();
  }
}
