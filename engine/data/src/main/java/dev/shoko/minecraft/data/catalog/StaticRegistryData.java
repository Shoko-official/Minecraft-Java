package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.codec.Codecs;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A static registry as the {@code registries.json} report describes it: its protocol id, its
 * entries and its optional default entry (R26). The entries are identifiers only: the typed values
 * belong to the module that owns the type. Immutable.
 */
public final class StaticRegistryData {

  private final Identifier registry;
  private final int protocolId;
  private final List<Identifier> ids;
  // Lookup only: never iterated, so its order is not observable.
  private final Map<Identifier, Integer> entryIds = new HashMap<>();
  // Null when the registry has no default entry.
  private final Identifier defaultEntry;

  private StaticRegistryData(
      Identifier registry, int protocolId, List<Identifier> ids, Identifier defaultEntry) {
    this.registry = registry;
    this.protocolId = protocolId;
    this.ids = List.copyOf(ids);
    this.defaultEntry = defaultEntry;
    for (int i = 0; i < ids.size(); i++) {
      entryIds.put(ids.get(i), i);
    }
  }

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
    if (!(report instanceof JsonObject root)) {
      return Outcome.failure(
          List.of(
              new Diagnostic(
                  DiagnosticCode.CODEC_TYPE_MISMATCH,
                  report.origin(),
                  report.path(),
                  "expected an object")));
    }
    List<Diagnostic> errors = new ArrayList<>();
    List<StaticRegistryData> registries = new ArrayList<>();
    Set<Integer> usedIds = new HashSet<>();
    for (Map.Entry<String, JsonNode> entry : root.members().entrySet()) {
      Optional<StaticRegistryData> registry = readRegistry(entry, usedIds, errors);
      registry.ifPresent(registries::add);
    }
    registries.sort(Comparator.comparingInt(StaticRegistryData::protocolId));
    return Outcome.success(List.copyOf(registries), errors);
  }

  private static Optional<StaticRegistryData> readRegistry(
      Map.Entry<String, JsonNode> entry, Set<Integer> usedIds, List<Diagnostic> errors) {
    JsonNode node = entry.getValue();
    Optional<Identifier> name = Identifier.tryParse(entry.getKey());
    if (name.isEmpty()) {
      errors.add(error(DiagnosticCode.CODEC_INVALID_IDENTIFIER, node, "invalid registry name"));
      return Optional.empty();
    }
    if (!(node instanceof JsonObject object)) {
      errors.add(error(DiagnosticCode.CODEC_TYPE_MISMATCH, node, "expected an object"));
      return Optional.empty();
    }
    List<Diagnostic> found = new ArrayList<>();
    Optional<StaticRegistryData> data = parse(name.get(), object, found);
    errors.addAll(found);
    if (data.isEmpty() || !usedIds.add(data.get().protocolId)) {
      if (data.isPresent()) {
        JsonNode idNode = object.members().get("protocol_id");
        errors.add(error(DiagnosticCode.CODEC_NUMBER_RANGE, idNode, "protocol id already used"));
      }
      return Optional.empty();
    }
    return data;
  }

  private static Optional<StaticRegistryData> parse(
      Identifier name, JsonObject object, List<Diagnostic> errors) {
    Optional<Integer> id = ReportFields.required(object, "protocol_id", Codecs.integer(), errors);
    Optional<Identifier> defaultEntry =
        ReportFields.optional(object, "default", Codecs.identifier(), errors);
    Optional<List<Identifier>> ids = entries(object, errors);
    if (id.isEmpty()
        || ids.isEmpty()
        || (defaultEntry.isPresent() && !ids.get().contains(defaultEntry.get()))) {
      return Optional.empty();
    }
    return Optional.of(
        new StaticRegistryData(name, id.get(), ids.get(), defaultEntry.orElse(null)));
  }

  /** Reads the entries and orders them by protocol id, which must be exactly 0 to n-1. */
  private static Optional<List<Identifier>> entries(JsonObject object, List<Diagnostic> errors) {
    JsonNode node = object.members().get("entries");
    if (node == null) {
      errors.add(missing(object, "entries"));
      return Optional.empty();
    }
    if (!(node instanceof JsonObject entries)) {
      errors.add(error(DiagnosticCode.CODEC_TYPE_MISMATCH, node, "expected an object"));
      return Optional.empty();
    }
    int size = entries.members().size();
    Identifier[] byId = new Identifier[size];
    boolean valid = true;
    for (Map.Entry<String, JsonNode> entry : entries.members().entrySet()) {
      Optional<Identifier> id = Identifier.tryParse(entry.getKey());
      Optional<Integer> protocolId = entryId(entry.getValue(), errors);
      if (id.isEmpty()) {
        errors.add(
            error(DiagnosticCode.CODEC_INVALID_IDENTIFIER, entry.getValue(), "invalid entry name"));
      }
      if (id.isEmpty() || protocolId.isEmpty()) {
        valid = false;
      } else if (protocolId.get() < 0
          || protocolId.get() >= size
          || byId[protocolId.get()] != null) {
        JsonNode idNode = ((JsonObject) entry.getValue()).members().get("protocol_id");
        errors.add(
            error(DiagnosticCode.CODEC_NUMBER_RANGE, idNode, "entry ids must be exactly 0 to n-1"));
        valid = false;
      } else {
        byId[protocolId.get()] = id.get();
      }
    }
    return valid ? Optional.of(List.of(byId)) : Optional.empty();
  }

  private static Optional<Integer> entryId(JsonNode node, List<Diagnostic> errors) {
    if (!(node instanceof JsonObject entry)) {
      errors.add(error(DiagnosticCode.CODEC_TYPE_MISMATCH, node, "expected an object"));
      return Optional.empty();
    }
    return ReportFields.required(entry, "protocol_id", Codecs.integer(), errors);
  }

  private static Diagnostic missing(JsonObject object, String key) {
    return new Diagnostic(
        DiagnosticCode.CODEC_MISSING_FIELD,
        object.origin(),
        object.path().field(key),
        "missing " + key);
  }

  private static Diagnostic error(DiagnosticCode code, JsonNode node, String message) {
    return new Diagnostic(code, node.origin(), node.path(), message);
  }

  /**
   * Returns the name of the registry.
   *
   * @return the identifier, such as {@code minecraft:block}
   */
  public Identifier registry() {
    return registry;
  }

  /**
   * Returns the protocol id of the registry.
   *
   * @return the id, 0 to 94 in the target (R26)
   */
  public int protocolId() {
    return protocolId;
  }

  /**
   * Returns the entries in protocol id order, so that the position of an entry is its protocol id
   * (R26).
   *
   * @return an unmodifiable list
   */
  public List<Identifier> ids() {
    return ids;
  }

  /**
   * Returns the protocol id of an entry.
   *
   * @param id the identifier of the entry
   * @return the id, or empty when the registry has no such entry
   */
  public OptionalInt entryProtocolId(Identifier id) {
    Integer position = entryIds.get(id);
    return position == null ? OptionalInt.empty() : OptionalInt.of(position);
  }

  /**
   * Returns the default entry (R26).
   *
   * @return the identifier, or empty for the registries that have none
   */
  public Optional<Identifier> defaultEntry() {
    return Optional.ofNullable(defaultEntry);
  }
}
