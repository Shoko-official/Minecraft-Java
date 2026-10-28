package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.catalog.FileAttribution;
import dev.shoko.minecraft.data.catalog.PathClassifier;
import dev.shoko.minecraft.data.catalog.RegistryDescriptor;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackContentException;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.pack.PackFilterException;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackMeta;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.tag.TagFile;
import dev.shoko.minecraft.data.tag.TagFileReader;
import dev.shoko.minecraft.data.tag.TagSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** One execution of the loader: reads the files of the packs and collects what they hold. */
final class LoadRun {

  private final LoadRequest request;
  private final PathClassifier classifier;
  private final List<Diagnostic> diagnostics = new ArrayList<>();
  private final Map<Identifier, Map<Identifier, JsonNode>> elements = new TreeMap<>();
  private final Map<Identifier, Map<Identifier, PackFileRef>> contents = new TreeMap<>();
  private final Map<Identifier, Map<Identifier, List<TagSource>>> tagLayers = new TreeMap<>();
  // Membership tests only: never iterated, so its order is not observable.
  private final Set<String> experimental = new HashSet<>();

  LoadRun(LoadRequest request) {
    this.request = request;
    this.classifier = PathClassifier.of(request.catalog());
  }

  @SuppressWarnings("try") // OpenFiles is used for its closing only
  Outcome<LoadedData> run() throws IOException {
    for (RegistryDescriptor registry : request.catalog().registries()) {
      switch (registry.kind()) {
        case ELEMENTS -> elements.put(registry.name(), new TreeMap<>());
        case CONTENT -> contents.put(registry.name(), new TreeMap<>());
        case STATIC -> {
          // static registries hold tags only
        }
      }
    }
    List<Pack> all = request.packs().packs();
    Map<String, FeatureFlags> missing = new HashMap<>();
    // The reason each pack left out of the load is reported, by pack identifier.
    Map<String, Diagnostic> dropped = new HashMap<>();
    List<Pack> kept = new ArrayList<>();
    for (Pack pack : all) {
      FeatureFlags absent = missingFlags(pack.meta());
      missing.put(pack.id(), absent);
      if (absent.flags().isEmpty()) {
        kept.add(pack);
      } else {
        dropped.put(pack.id(), inactiveFlags(pack, absent));
      }
    }
    try (OpenFiles open = new OpenFiles(all)) {
      Map<String, List<PackFileRef>> filesByPack = stackFiles(all, kept, dropped);
      for (Pack pack : all) {
        if (kept.contains(pack)) {
          readFiles(pack.id(), filesByPack.getOrDefault(pack.id(), List.of()));
        } else {
          diagnostics.add(dropped.get(pack.id()));
        }
      }
    }
    TagLoading tags = new TagLoading(request, elements, contents, tagLayers);
    diagnostics.addAll(tags.diagnostics());
    return Outcome.success(
        new LoadedData(
            request.staticRegistries(),
            elementRegistries(),
            tags.resolutions(),
            contents,
            request.activeFlags(),
            reports(all, missing)),
        diagnostics);
  }

  private FeatureFlags missingFlags(PackMeta meta) {
    FeatureFlags active = request.activeFlags();
    return FeatureFlags.of(
        meta.features().flags().stream().filter(f -> !active.contains(f)).toList());
  }

  private static Diagnostic inactiveFlags(Pack pack, FeatureFlags missing) {
    String message = "pack requires flags that are not active: " + missing.flags();
    FileOrigin origin = new FileOrigin(pack.id(), "pack.mcmeta");
    return new Diagnostic(DiagnosticCode.PACK_FLAGS_INACTIVE, origin, JsonPath.root(), message);
  }

  /**
   * Stacks the kept packs and lists their files. A pack whose block filter is too costly to
   * evaluate is left out of the stack, with a diagnostic, and the stack is built again.
   */
  private static Map<String, List<PackFileRef>> stackFiles(
      List<Pack> all, List<Pack> kept, Map<String, Diagnostic> dropped) throws IOException {
    PackType type = all.isEmpty() ? PackType.DATA : all.get(0).type();
    while (true) {
      try {
        return filesByPack(PackStack.of(type, kept));
      } catch (PackFilterException e) {
        Pack refused =
            kept.stream().filter(pack -> pack.id().equals(e.pack())).findFirst().orElseThrow();
        kept.remove(refused);
        FileOrigin origin = new FileOrigin(refused.id(), "pack.mcmeta");
        dropped.put(
            refused.id(),
            new Diagnostic(DiagnosticCode.PACK_INVALID, origin, JsonPath.root(), e.getMessage()));
      }
    }
  }

  /** Lists the files of each pack in sorted path order, so diagnostics follow the pack stack. */
  private static Map<String, List<PackFileRef>> filesByPack(PackStack stack) throws IOException {
    Map<String, List<PackFileRef>> byPack = new LinkedHashMap<>();
    for (String path : stack.paths()) {
      for (PackFileRef ref : stack.layers(path)) {
        byPack.computeIfAbsent(ref.pack(), id -> new ArrayList<>()).add(ref);
      }
    }
    return byPack;
  }

  private void readFiles(String pack, List<PackFileRef> files) throws IOException {
    for (PackFileRef file : files) {
      switch (classifier.classify(file.path())) {
        case FileAttribution.Element element -> readElement(pack, file, element);
        case FileAttribution.Tag tag -> readTag(file, tag);
        case FileAttribution.Content content ->
            contents.get(content.registry()).put(content.id(), file);
        case FileAttribution.Ignored ignored ->
            diagnostics.add(
                new Diagnostic(
                    DiagnosticCode.FILE_IGNORED,
                    file.origin(),
                    JsonPath.root(),
                    ignored.message()));
        case FileAttribution.Metadata metadata -> {
          // pack metadata and extraction markers take no part in loading
        }
        case FileAttribution.Skipped skipped -> {
          // another extension than the folder takes: ignored without diagnostic (R12)
        }
      }
    }
  }

  private void readElement(String pack, PackFileRef file, FileAttribution.Element element)
      throws IOException {
    JsonNode tree = parse(file);
    if (tree == null) {
      return;
    }
    elements.get(element.registry()).put(element.id(), tree);
    boolean stable = request.catalog().find(element.registry()).orElseThrow().stable();
    if (!stable) {
      experimental.add(pack);
    }
  }

  private void readTag(PackFileRef file, FileAttribution.Tag tag) throws IOException {
    JsonNode root = parse(file);
    if (root == null) {
      return;
    }
    Outcome<TagFile> read = TagFileReader.read(root);
    diagnostics.addAll(read.diagnostics());
    read.value()
        .ifPresent(
            tagFile ->
                tagLayers
                    .computeIfAbsent(tag.registry(), registry -> new LinkedHashMap<>())
                    .computeIfAbsent(tag.id(), id -> new ArrayList<>())
                    .add(new TagSource(file.origin(), tagFile)));
  }

  /** Reads one JSON file; returns null when it is invalid, after recording its diagnostic. */
  private JsonNode parse(PackFileRef file) throws IOException {
    byte[] bytes;
    try {
      bytes = file.read();
    } catch (PackContentException e) {
      // Too large or damaged: the file is invalid like any other, the rest of the pack loads (R54).
      diagnostics.add(
          new Diagnostic(
              DiagnosticCode.JSON_SYNTAX, file.origin(), JsonPath.root(), e.getMessage()));
      return null;
    }
    Outcome<JsonNode> read = JsonReader.read(bytes, file.origin());
    diagnostics.addAll(read.diagnostics());
    return read.value().orElse(null);
  }

  private List<ElementRegistryData> elementRegistries() {
    List<ElementRegistryData> registries = new ArrayList<>();
    elements.forEach((name, files) -> registries.add(new ElementRegistryData(name, files)));
    return registries;
  }

  private List<PackReport> reports(List<Pack> all, Map<String, FeatureFlags> missing) {
    List<PackReport> reports = new ArrayList<>();
    for (Pack pack : all) {
      reports.add(
          new PackReport(
              pack.id(),
              pack.meta().compatibility(pack.type()),
              experimental.contains(pack.id()),
              pack.meta().features(),
              missing.get(pack.id())));
    }
    return reports;
  }
}
