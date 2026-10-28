package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A static registry as the {@code registries.json} report describes it: its protocol id, its
 * entries and its optional default entry (R26). The entries are identifiers only: the typed values
 * belong to the module that owns the type. Immutable.
 */
public final class StaticRegistryData {

  private StaticRegistryData() {}

  /**
   * Reads the report.
   *
   * <p>Each registry has a {@code protocol_id}, its entries each with a {@code protocol_id}, and
   * possibly a {@code default}. The entry ids of a registry must be exactly 0 to n-1 and the
   * registry ids distinct; otherwise the registry is dropped with a {@code CODEC_NUMBER_RANGE}
   * error at the id (provisional: the R61 table has no closer code). A report of the wrong shape
   * gives a {@code CODEC_TYPE_MISMATCH} or {@code CODEC_MISSING_FIELD} error.
   *
   * @param report the parsed {@code generated/reports/registries.json}
   * @return the registries in order of their protocol ids (R26), or empty when the report is
   *     unusable, with the diagnostics found
   */
  public static Outcome<List<StaticRegistryData>> read(JsonNode report) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the name of the registry.
   *
   * @return the identifier, such as {@code minecraft:block}
   */
  public Identifier registry() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the protocol id of the registry.
   *
   * @return the id, 0 to 94 in the target (R26)
   */
  public int protocolId() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the entries in protocol id order, so that the position of an entry is its protocol id
   * (R26).
   *
   * @return an unmodifiable list
   */
  public List<Identifier> ids() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the protocol id of an entry.
   *
   * @param id the identifier of the entry
   * @return the id, or empty when the registry has no such entry
   */
  public OptionalInt entryProtocolId(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the default entry (R26).
   *
   * @return the identifier, or empty for the registries that have none
   */
  public Optional<Identifier> defaultEntry() {
    return Skeleton.throwNotImplemented();
  }
}
