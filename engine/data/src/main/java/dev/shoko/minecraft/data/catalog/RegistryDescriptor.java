package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * A registry as the loadable-registries report describes it: its folder, what files it takes and
 * whether it is stable (R15 to R17).
 *
 * @param name the identifier of the registry; its path is the folder under {@code
 *     data/<namespace>/} (R8), possibly with {@code /}
 * @param kind what the folder holds
 * @param tags whether the registry takes tags (R16)
 * @param stable the {@code stable} property of the report (R17); false marks registries that make a
 *     pack "experimental"
 * @param extension the extension of the element files, with the dot: {@code .json} for registries,
 *     {@code .mcfunction} and {@code .nbt} for the other contents (R10, R12)
 */
public record RegistryDescriptor(
    Identifier name, Kind kind, boolean tags, boolean stable, String extension) {

  /** What a registry folder holds. */
  public enum Kind {
    /** A static registry: no element files, tags only (R15, 95 in the target). */
    STATIC,
    /** A registry loaded from JSON element files (60 in the target). */
    ELEMENTS,
    /** Opaque content kept as bytes: functions and structures (R10). */
    CONTENT
  }
}
