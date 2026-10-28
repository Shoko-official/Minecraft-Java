package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipException;

/** Finds the packs available on disk (R3, R14, R19, R40). */
public final class PackDiscovery {

  private static final String VANILLA_ID = "vanilla";
  private static final String BUILT_IN_FOLDER = "data/minecraft/datapacks";
  private static final String ARCHIVE_SUFFIX = ".zip";
  private static final String WORLD_PREFIX = "file/";

  private PackDiscovery() {}

  /**
   * Finds the packs of the extracted reference data: the implicit vanilla pack (R3), then the
   * built-in packs stored under {@code data/minecraft/datapacks/} (R14).
   *
   * <p>The vanilla pack excludes that folder (R14) and ignores the {@code .mcassetsroot} marker
   * (R13). It has the identifier {@code vanilla} and the origin {@link PackOrigin#BUILT_IN}. Each
   * built-in pack is a datapack named after its folder (R40: {@code minecart_improvements}, {@code
   * redstone_experiments}, {@code trade_rebalance}), with {@link PackOrigin#FEATURE}, ordered by
   * name by Unicode code point.
   *
   * @param resourcesRoot the {@code resources} folder of the extracted data
   * @return the packs, vanilla first, with the diagnostics of the packs that could not be opened
   * @throws IOException when a folder cannot be listed
   */
  public static Outcome<List<Pack>> referencePacks(Path resourcesRoot) throws IOException {
    PackFiles root = PackFiles.directory(resourcesRoot);
    List<Pack> packs = new ArrayList<>();
    packs.add(
        Pack.implicit(
            VANILLA_ID, PackOrigin.BUILT_IN, PackType.DATA, root.without(BUILT_IN_FOLDER)));
    List<Diagnostic> diagnostics = new ArrayList<>();
    Path builtIn = resourcesRoot.resolve(BUILT_IN_FOLDER);
    if (Files.isDirectory(builtIn)) {
      for (Path folder : sortedChildren(builtIn)) {
        if (Files.isDirectory(folder)) {
          String name = folder.getFileName().toString();
          open(name, PackOrigin.FEATURE, PackFiles.directory(folder), packs, diagnostics);
        }
      }
    }
    return Outcome.success(packs, diagnostics);
  }

  /**
   * Finds the packs of a world {@code datapacks} folder (R67, provisional, probe P24).
   *
   * <ul>
   *   <li>Candidates are the direct sub-folders and the direct files whose name ends with {@code
   *       .zip} in lower case (R19). Names that start with {@code .} and every other file, such as
   *       {@code readme.txt} or {@code y.ZIP}, are ignored without diagnostic.
   *   <li>A candidate is named {@code file/<full name>}, extension included, and has {@link
   *       PackOrigin#WORLD} (R40).
   *   <li>A candidate without a usable {@code pack.mcmeta} is not a pack: it is left out with a
   *       {@code PACK_INVALID} warning (R7).
   *   <li>The result follows the name of the candidate, compared by Unicode code point, whatever
   *       the file system, the locale or the case rules of the system: {@code Z}, {@code a}, {@code
   *       p10}, {@code p2}, {@code x.zip}, then the name that starts with e-acute.
   *   <li>An incompatible pack (R5) is returned like the others: it stays available (R6).
   * </ul>
   *
   * @param datapacksFolder the {@code datapacks} folder of a world
   * @return the packs in discovery order, with the diagnostics of the candidates that were left out
   * @throws IOException when the folder cannot be listed
   */
  public static Outcome<List<Pack>> worldPacks(Path datapacksFolder) throws IOException {
    List<Pack> packs = new ArrayList<>();
    List<Diagnostic> diagnostics = new ArrayList<>();
    for (Path candidate : sortedChildren(datapacksFolder)) {
      String name = candidate.getFileName().toString();
      if (name.startsWith(".")) {
        continue;
      }
      PackFiles files = null;
      if (Files.isDirectory(candidate)) {
        files = PackFiles.directory(candidate);
      } else if (name.endsWith(ARCHIVE_SUFFIX) && Files.isRegularFile(candidate)) {
        files = PackFiles.archive(candidate);
      }
      if (files != null) {
        open(WORLD_PREFIX + name, PackOrigin.WORLD, files, packs, diagnostics);
      }
    }
    return Outcome.success(packs, diagnostics);
  }

  /**
   * Chooses the packs to activate and their order, from the packs found and the list kept by the
   * world (R40, R67 e, provisional).
   *
   * <p>First come the packs named in {@code savedEnabledIds}, in that order, whatever their
   * compatibility (an explicit activation is accepted, R6); an identifier that matches no pack is
   * skipped. Then come the packs that are not in the saved list and are compatible (R5), in
   * discovery order; an incompatible or broken pack that is not in the saved list stays inactive.
   * The first pack of the result has the lowest priority and the last the highest (R40).
   *
   * @param discovered the packs found, in discovery order
   * @param savedEnabledIds the identifiers of the packs enabled in the world, lowest priority first
   * @return the packs to activate, lowest priority first
   */
  public static List<Pack> activationOrder(List<Pack> discovered, List<String> savedEnabledIds) {
    Map<String, Pack> byId = new HashMap<>();
    for (Pack pack : discovered) {
      byId.putIfAbsent(pack.id(), pack);
    }
    Set<String> chosen = new HashSet<>();
    List<Pack> order = new ArrayList<>();
    for (String id : savedEnabledIds) {
      Pack pack = byId.get(id);
      if (pack != null && chosen.add(id)) {
        order.add(pack);
      }
    }
    for (Pack pack : discovered) {
      boolean compatible = pack.meta().compatibility(pack.type()) == PackCompatibility.COMPATIBLE;
      if (compatible && chosen.add(pack.id())) {
        order.add(pack);
      }
    }
    return order;
  }

  private static void open(
      String id, PackOrigin origin, PackFiles files, List<Pack> packs, List<Diagnostic> found)
      throws IOException {
    try {
      Outcome<Pack> opened = Pack.open(id, origin, PackType.DATA, files);
      opened.value().ifPresent(packs::add);
      found.addAll(opened.diagnostics());
    } catch (ZipException e) {
      String message = "unreadable archive: " + e.getMessage();
      FileOrigin where = new FileOrigin(id, Pack.META_FILE);
      found.add(new Diagnostic(DiagnosticCode.PACK_INVALID, where, JsonPath.root(), message));
    }
  }

  private static List<Path> sortedChildren(Path folder) throws IOException {
    try (Stream<Path> children = Files.list(folder)) {
      return children.sorted((a, b) -> PathOrder.compare(name(a), name(b))).toList();
    }
  }

  private static String name(Path path) {
    return path.getFileName().toString();
  }
}
