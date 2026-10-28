package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;
import java.util.Optional;

/**
 * The registries that datapacks can fill, read from the {@code datapack.json} report of the
 * extracted data (R15 to R17): 155 registries plus the {@code function} and {@code structure}
 * contents (R10). Immutable.
 */
public final class RegistryCatalog {

  private RegistryCatalog() {}

  /**
   * Reads the report.
   *
   * <p>Each registry gives a {@link RegistryDescriptor}; the entries of the {@code others} section
   * give {@link RegistryDescriptor.Kind#CONTENT} descriptors. A report that does not have this
   * shape gives no value and a {@code CODEC_TYPE_MISMATCH} or {@code CODEC_MISSING_FIELD} error at
   * the faulty path (the R61 table has no code of its own for a report; provisional).
   *
   * @param report the parsed {@code generated/reports/datapack.json}
   * @return the catalog, or empty when the report is unusable, with the diagnostics found
   */
  public static Outcome<RegistryCatalog> read(JsonNode report) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns every descriptor.
   *
   * @return the descriptors in code point order of their names
   */
  public List<RegistryDescriptor> registries() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds a registry by name.
   *
   * @param name the identifier of the registry, such as {@code minecraft:worldgen/biome}
   * @return the descriptor, or empty when the registry is unknown
   */
  public Optional<RegistryDescriptor> find(Identifier name) {
    return Skeleton.throwNotImplemented();
  }
}
