package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * Downloads the official server bundle of the pinned version, checks it against {@code pins.json},
 * extracts its data resources (never its code) and runs the official data generator. Nothing is
 * written outside the cache directory (ADR 0003).
 *
 * <p>A later run skips the work only when {@code state.json} matches the pins and the extracted
 * trees still have the recorded files and sizes (see {@link SafeTree.Fingerprint}). A file
 * rewritten with different bytes of the same size is only detected with {@code --verify-content},
 * which hashes every extracted file again.
 */
public final class Bootstrap {

  /** What a run did. */
  enum Outcome {
    ALREADY_VERIFIED,
    REBUILT
  }

  /**
   * How a run behaves.
   *
   * @param generate whether to run the data generator
   * @param verifyContent whether the up-to-date check hashes every extracted file
   */
  record Options(boolean generate, boolean verifyContent, ArchiveExtractor.Limits limits) {}

  private static final String USAGE =
      "usage: Bootstrap [--cache-dir DIR] [--no-generate] [--verify-content]";

  private final Pins pins;
  private final CacheLayout layout;
  private final Downloader downloader;
  private final Options options;
  private final StateStore states;

  Bootstrap(Pins pins, CacheLayout layout, Downloader downloader, Options options) {
    this.pins = pins;
    this.layout = layout;
    this.downloader = downloader;
    this.options = options;
    this.states = new StateStore(layout.state());
  }

  public static void main(String[] args) {
    System.exit(execute(List.of(args), System.getenv("MINECRAFT_CACHE_DIR"), System.err));
  }

  /** Runs the tool and returns its exit status: 0 on success, 1 on failure, 2 on bad arguments. */
  static int execute(List<String> args, String cacheEnv, PrintStream err) {
    Path cache;
    Options options;
    try {
      cache = Path.of(cacheEnv == null ? ".cache" : cacheEnv);
      boolean generate = true;
      boolean verifyContent = false;
      for (int i = 0; i < args.size(); i++) {
        switch (args.get(i)) {
          case "--cache-dir" -> {
            if (++i >= args.size()) {
              throw new IllegalArgumentException("--cache-dir needs a directory");
            }
            cache = Path.of(args.get(i));
          }
          case "--no-generate" -> generate = false;
          case "--verify-content" -> verifyContent = true;
          default -> throw new IllegalArgumentException("unknown argument: " + args.get(i));
        }
      }
      options = new Options(generate, verifyContent, ArchiveExtractor.Limits.DEFAULT);
    } catch (IllegalArgumentException e) {
      err.println("bootstrap: " + e.getMessage());
      err.println(USAGE);
      return 2;
    }
    try {
      Pins pins = Pins.load();
      new Bootstrap(pins, new CacheLayout(cache, pins.gameVersion()), new Downloader(), options)
          .run();
      return 0;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      err.println("bootstrap interrupted: nothing unverified was published, run it again");
      return 1;
    } catch (IOException
        | UncheckedIOException
        | JsonParseException
        | InvalidPathException
        | NullPointerException e) {
      err.println("bootstrap failed: " + e.getClass().getName() + ": " + e.getMessage());
      if (e instanceof NullPointerException) {
        // Every expected data problem is reported as an IOException: this one may be a bug.
        e.printStackTrace(err);
      }
      return 1;
    }
  }

  Outcome run() throws IOException, InterruptedException {
    if (Runtime.version().feature() < 25) {
      throw new IOException("Java 25 is required to run the official data generator");
    }
    URI serverUrl = fetchVersionMetadata();
    downloader.fetch(
        serverUrl,
        layout.serverJar(),
        new Downloader.Expected(
            pins.server().size(),
            List.of(
                new Downloader.Digest("SHA-1", pins.server().sha1()),
                new Downloader.Digest("SHA-256", pins.server().sha256()))));
    if (upToDate()) {
      log("reference data for " + pins.gameVersion() + " already verified");
      return Outcome.ALREADY_VERIFIED;
    }
    // Before any tree is touched, so that an interrupted run is never mistaken for a complete one.
    states.invalidate();
    String innerSha256 = extractResources();
    SafeTree.Fingerprint generated = null;
    if (options.generate()) {
      SafeTree.clear(layout.generated(), layout.root());
      GeneratorRunner.reports(layout.serverJar(), layout.generatorWork(), layout.generated());
      generated = SafeTree.fingerprint(layout.generated(), true);
    }
    SafeTree.Fingerprint resources = SafeTree.fingerprint(layout.resources(), true);
    states.write(
        new StateStore.State(
            pins.gameVersion(), pins.server().sha256(), innerSha256, resources, generated));
    log(
        "done: "
            + resources.files()
            + " resource files, "
            + (generated == null ? 0 : generated.files())
            + " generated report files");
    return Outcome.REBUILT;
  }

  /** Fetches the version metadata and returns the server download URL, after cross-checks. */
  private URI fetchVersionMetadata() throws IOException, InterruptedException {
    downloader.fetch(
        URI.create(pins.versionJson().url()),
        layout.versionJson(),
        Downloader.Expected.sha1(pins.versionJson().sha1()));
    String document = "Version metadata " + layout.versionJson().getFileName();
    JsonObject root =
        JsonFields.parseObject(
            Files.readString(layout.versionJson(), StandardCharsets.UTF_8), document);
    JsonObject server =
        JsonFields.object(JsonFields.object(root, "downloads", document), "server", document);
    if (!JsonFields.string(server, "sha1", document).equals(pins.server().sha1())
        || JsonFields.integer(server, "size", document) != pins.server().size()) {
      throw new IOException("Official metadata no longer matches pins.json for the server bundle");
    }
    String url = JsonFields.string(server, "url", document);
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new IOException(document + ": invalid server URL " + url, e);
    }
  }

  /** Checks the bundle, replaces the resources tree, and returns the inner archive SHA-256. */
  private String extractResources() throws IOException {
    BundleVerifier verifier = new BundleVerifier(pins);
    try (ZipFile bundle = new ZipFile(layout.serverJar().toFile())) {
      byte[] identity = verifier.checkIdentity(bundle);
      BundleVerifier.InnerJar inner = verifier.checkInnerJar(bundle);
      Files.createDirectories(layout.reference());
      Files.write(layout.identity(), identity);
      SafeTree.clear(layout.resources(), layout.root());
      try (InputStream in = bundle.getInputStream(inner.entry())) {
        ArchiveExtractor.extract(
            in,
            layout.resources(),
            name ->
                !name.endsWith(".class")
                    && (name.startsWith("data/") || name.startsWith("assets/")),
            options.limits());
      }
      return inner.sha256();
    }
  }

  private boolean upToDate() throws IOException {
    Optional<StateStore.State> read = states.read();
    if (read.isEmpty()) {
      return stale("no complete, readable state.json");
    }
    StateStore.State state = read.get();
    if (!state.gameVersion().equals(pins.gameVersion())
        || !state.serverSha256().equals(pins.server().sha256())
        || !state.innerServerSha256().equals(pins.innerServerSha256())) {
      return stale("state.json was recorded for other pins");
    }
    if (!matches(layout.resources(), state.resources())) {
      return stale("resources/ differs from state.json");
    }
    if (options.generate()
        && (state.generated() == null || !matches(layout.generated(), state.generated()))) {
      return stale("generated/ differs from state.json");
    }
    return true;
  }

  private boolean matches(Path dir, SafeTree.Fingerprint recorded) throws IOException {
    SafeTree.Fingerprint actual = SafeTree.fingerprint(dir, options.verifyContent());
    return recorded.files() > 0
        && actual.files() == recorded.files()
        && actual.listing().equals(recorded.listing())
        && (!options.verifyContent() || actual.content().equals(recorded.content()));
  }

  private static boolean stale(String reason) {
    log("rebuilding: " + reason);
    return false;
  }

  private static void log(String message) {
    System.out.println("bootstrap: " + message);
  }
}
