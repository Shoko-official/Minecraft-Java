package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.codec.Codec;
import dev.shoko.minecraft.data.codec.Codecs;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * The registries that datapacks can fill, read from the {@code datapack.json} report of the
 * extracted data (R15 to R17): 155 registries plus the {@code function} and {@code structure}
 * contents (R10). Immutable.
 */
public final class RegistryCatalog {

  // The report names the format of a content folder; structures are NBT files (R10).
  private static final String STRUCTURE_FORMAT = "structure";

  private final List<RegistryDescriptor> registries;
  // Lookup only: never iterated, so its order is not observable.
  private final Map<Identifier, RegistryDescriptor> byName = new HashMap<>();

  private RegistryCatalog(Map<Identifier, RegistryDescriptor> sorted) {
    this.registries = List.copyOf(sorted.values());
    byName.putAll(sorted);
  }

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
    Map<Identifier, RegistryDescriptor> found = new TreeMap<>();
    section(root, "registries", RegistryDescriptor.Kind.ELEMENTS, found, errors);
    section(root, "others", RegistryDescriptor.Kind.CONTENT, found, errors);
    if (!errors.isEmpty()) {
      return Outcome.failure(errors);
    }
    return Outcome.success(new RegistryCatalog(found));
  }

  /**
   * Returns every descriptor.
   *
   * @return the descriptors in code point order of their names
   */
  public List<RegistryDescriptor> registries() {
    return registries;
  }

  /**
   * Finds a registry by name.
   *
   * @param name the identifier of the registry, such as {@code minecraft:worldgen/biome}
   * @return the descriptor, or empty when the registry is unknown
   */
  public Optional<RegistryDescriptor> find(Identifier name) {
    return Optional.ofNullable(byName.get(name));
  }

  private static void section(
      JsonObject root,
      String key,
      RegistryDescriptor.Kind sectionKind,
      Map<Identifier, RegistryDescriptor> found,
      List<Diagnostic> errors) {
    JsonNode node = root.members().get(key);
    if (node == null) {
      errors.add(missing(root, key));
      return;
    }
    if (!(node instanceof JsonObject entries)) {
      errors.add(mismatch(node, "an object"));
      return;
    }
    for (Map.Entry<String, JsonNode> entry : entries.members().entrySet()) {
      Optional<Identifier> name = Identifier.tryParse(entry.getKey());
      if (name.isEmpty()) {
        errors.add(
            new Diagnostic(
                DiagnosticCode.CODEC_INVALID_IDENTIFIER,
                entries.origin(),
                entries.path().field(entry.getKey()),
                "invalid registry name"));
        continue;
      }
      Outcome<RegistryDescriptor> descriptor =
          descriptorCodec(name.get(), sectionKind).decode(entry.getValue(), RegistryAccess.empty());
      errors.addAll(descriptor.diagnostics());
      descriptor.value().ifPresent(value -> found.put(name.get(), value));
    }
  }

  private static Codec<RegistryDescriptor> descriptorCodec(
      Identifier name, RegistryDescriptor.Kind sectionKind) {
    return Codecs.object(
        reader -> {
          Supplier<Boolean> elements = reader.required("elements", Codecs.bool());
          Supplier<Boolean> stable = reader.required("stable", Codecs.bool());
          Supplier<Boolean> tags = reader.required("tags", Codecs.bool());
          Supplier<String> format =
              sectionKind == RegistryDescriptor.Kind.CONTENT
                  ? reader.required("format", contentFormat())
                  : () -> ".json";
          return () -> {
            RegistryDescriptor.Kind kind =
                sectionKind == RegistryDescriptor.Kind.CONTENT
                    ? RegistryDescriptor.Kind.CONTENT
                    : elements.get()
                        ? RegistryDescriptor.Kind.ELEMENTS
                        : RegistryDescriptor.Kind.STATIC;
            return new RegistryDescriptor(name, kind, tags.get(), stable.get(), format.get());
          };
        });
  }

  private static Codec<String> contentFormat() {
    return Codecs.string().map(format -> STRUCTURE_FORMAT.equals(format) ? ".nbt" : "." + format);
  }

  private static Diagnostic mismatch(JsonNode node, String expected) {
    return new Diagnostic(
        DiagnosticCode.CODEC_TYPE_MISMATCH, node.origin(), node.path(), "expected " + expected);
  }

  private static Diagnostic missing(JsonObject object, String key) {
    return new Diagnostic(
        DiagnosticCode.CODEC_MISSING_FIELD,
        object.origin(),
        object.path().field(key),
        "missing " + key);
  }
}
