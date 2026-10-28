package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Decides what each file path of a pack stack is (R8 to R16, R24). Immutable. */
public final class PathClassifier {

  private static final String DATA = "data/";
  private static final String TAGS = "tags/";
  private static final String JSON = ".json";

  // Lookup only: never iterated, so its order is not observable.
  private final Map<String, RegistryDescriptor> byFolder = new HashMap<>();
  private final int deepestFolder;

  private PathClassifier(RegistryCatalog catalog) {
    int deepest = 1;
    for (RegistryDescriptor descriptor : catalog.registries()) {
      String folder = descriptor.name().path();
      byFolder.put(folder, descriptor);
      deepest = Math.max(deepest, folder.split("/", -1).length);
    }
    this.deepestFolder = deepest;
  }

  /**
   * Builds a classifier for a catalog.
   *
   * @param catalog the loadable registries
   * @return the classifier
   */
  public static PathClassifier of(RegistryCatalog catalog) {
    return new PathClassifier(catalog);
  }

  /**
   * Classifies one stacked path.
   *
   * <ul>
   *   <li>{@code data/<ns>/<folder>/<path>.json} with a registry that takes elements is an {@link
   *       FileAttribution.Element} with identifier {@code <ns>:<path>}; the folder is the longest
   *       known registry folder prefix (R8, R11). The path may contain sub-folders.
   *   <li>{@code data/<ns>/tags/<folder>/<path>.json} is a {@link FileAttribution.Tag} when the
   *       registry takes tags (R9, R16).
   *   <li>{@code .mcfunction} under {@code function} and {@code .nbt} under {@code structure} are
   *       {@link FileAttribution.Content} (R10).
   *   <li>{@code pack.mcmeta}, {@code pack.png}, {@code .mcassetsroot} and anything outside {@code
   *       data/} are {@link FileAttribution.Metadata} (R12, R13, R18).
   *   <li>Another extension than the folder takes is {@link FileAttribution.Skipped}, with no
   *       diagnostic (R12).
   *   <li>Otherwise {@link FileAttribution.Ignored}, reported as {@code FILE_IGNORED}: an unknown
   *       folder or a file in a static registry folder (R15); a tag under a registry that takes
   *       none, such as {@code tags/structure} (R16); a namespace or path that breaks the
   *       identifier syntax, such as a capital letter, a space or {@code ..} (R24).
   * </ul>
   *
   * @param path the path in the stack, with {@code /} separators
   * @return the attribution
   */
  public FileAttribution classify(String path) {
    if (!path.startsWith(DATA) || path.equals(DATA + ".mcassetsroot")) {
      return new FileAttribution.Metadata();
    }
    int slash = path.indexOf('/', DATA.length());
    if (slash < 0) {
      return ignored("file directly under data/");
    }
    String namespace = path.substring(DATA.length(), slash);
    if (!Identifier.isValidNamespace(namespace)) {
      return ignored("invalid namespace: " + namespace);
    }
    String rest = path.substring(slash + 1);
    return rest.startsWith(TAGS)
        ? classifyTag(namespace, rest.substring(TAGS.length()))
        : classifyElement(namespace, rest);
  }

  private FileAttribution classifyElement(String namespace, String rest) {
    RegistryDescriptor registry = findFolder(rest);
    if (registry == null) {
      return ignored("unknown folder: " + rest);
    }
    if (registry.kind() == RegistryDescriptor.Kind.STATIC) {
      return ignored("static registry " + registry.name() + " takes tags only");
    }
    String file = rest.substring(registry.name().path().length() + 1);
    if (!file.endsWith(registry.extension())) {
      return new FileAttribution.Skipped();
    }
    String idPath = file.substring(0, file.length() - registry.extension().length());
    if (!Identifier.isValidPath(idPath)) {
      return ignored("invalid path: " + idPath);
    }
    Identifier id = Identifier.of(namespace, idPath);
    return registry.kind() == RegistryDescriptor.Kind.CONTENT
        ? new FileAttribution.Content(registry.name(), id)
        : new FileAttribution.Element(registry.name(), id);
  }

  private FileAttribution classifyTag(String namespace, String rest) {
    RegistryDescriptor registry = findFolder(rest);
    if (registry == null) {
      return ignored("tags of an unknown registry: " + rest);
    }
    if (!registry.tags()) {
      return ignored("registry " + registry.name() + " takes no tags");
    }
    String file = rest.substring(registry.name().path().length() + 1);
    if (!file.endsWith(JSON)) {
      return new FileAttribution.Skipped();
    }
    String idPath = file.substring(0, file.length() - JSON.length());
    if (!Identifier.isValidPath(idPath)) {
      return ignored("invalid path: " + idPath);
    }
    return new FileAttribution.Tag(registry.name(), Identifier.of(namespace, idPath));
  }

  /**
   * Finds the registry whose folder is the longest prefix of {@code rest}, a file name following.
   */
  private RegistryDescriptor findFolder(String rest) {
    int[] ends = new int[deepestFolder];
    int found = 0;
    for (int from = 0; found < deepestFolder; ) {
      int slash = rest.indexOf('/', from);
      if (slash < 0) {
        break;
      }
      ends[found++] = slash;
      from = slash + 1;
    }
    for (int i = found - 1; i >= 0; i--) {
      RegistryDescriptor descriptor = byFolder.get(rest.substring(0, ends[i]));
      if (descriptor != null) {
        return descriptor;
      }
    }
    return null;
  }

  private static FileAttribution ignored(String message) {
    return new FileAttribution.Ignored(message);
  }
}
