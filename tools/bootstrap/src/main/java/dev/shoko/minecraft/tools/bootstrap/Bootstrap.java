package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads the official server bundle of the pinned version, checks it against {@code pins.json},
 * extracts its data resources (never its code) and runs the official data generator. Nothing is
 * written outside the cache directory (ADR 0003).
 */
public final class Bootstrap {

  /** What was verified, written last so that an interrupted run is redone. */
  record State(
      String gameVersion,
      String serverSha256,
      String innerServerSha256,
      int resourceFiles,
      int generatedFiles) {}

  /** The server archive nested in the bundle, as listed by the bundle itself. */
  private record InnerJar(String path, String sha256) {}

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final Pins pins;
  private final CacheLayout layout;
  private final Downloader downloader;
  private final boolean generate;

  Bootstrap(Pins pins, CacheLayout layout, Downloader downloader, boolean generate) {
    this.pins = pins;
    this.layout = layout;
    this.downloader = downloader;
    this.generate = generate;
  }

  public static void main(String[] args) {
    try {
      Path cache = Path.of(System.getenv().getOrDefault("MINECRAFT_CACHE_DIR", ".cache"));
      boolean generate = true;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--cache-dir" -> {
            if (++i >= args.length) {
              throw new IllegalArgumentException("--cache-dir needs a directory");
            }
            cache = Path.of(args[i]);
          }
          case "--no-generate" -> generate = false;
          default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
        }
      }
      Pins pins = Pins.load();
      new Bootstrap(pins, new CacheLayout(cache, pins.gameVersion()), new Downloader(), generate)
          .run();
    } catch (IOException | IllegalArgumentException e) {
      System.err.println("bootstrap failed: " + e.getMessage());
      System.err.println("usage: Bootstrap [--cache-dir DIR] [--no-generate]");
      System.exit(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.exit(1);
    }
  }

  void run() throws IOException, InterruptedException {
    if (Runtime.version().feature() < 25) {
      throw new IOException("Java 25 is required to run the official data generator");
    }
    String serverUrl = fetchVersionMetadata();
    fetchServer(serverUrl);
    if (upToDate()) {
      log("reference data for " + pins.gameVersion() + " already verified");
      return;
    }
    int resources;
    InnerJar inner;
    try (ZipFile bundle = new ZipFile(layout.serverJar().toFile())) {
      checkIdentity(bundle);
      inner = checkInnerJar(bundle);
      clear(layout.resources());
      resources = extractResources(bundle, inner.path());
    }
    int generated = 0;
    if (generate) {
      clear(layout.generated());
      GeneratorRunner.reports(layout.serverJar(), layout.generatorWork(), layout.generated());
      generated = countFiles(layout.generated());
    }
    State state =
        new State(pins.gameVersion(), pins.server().sha256(), inner.sha256(), resources, generated);
    Files.writeString(layout.state(), GSON.toJson(state), StandardCharsets.UTF_8);
    log("done: " + resources + " resource files, " + generated + " generated report files");
  }

  /** Fetches the version metadata and returns the server download URL, after cross-checks. */
  private String fetchVersionMetadata() throws IOException, InterruptedException {
    downloader.fetch(
        URI.create(pins.versionJson().url()), layout.versionJson(), pins.versionJson().sha1(), -1);
    JsonObject root =
        JsonParser.parseString(Files.readString(layout.versionJson(), StandardCharsets.UTF_8))
            .getAsJsonObject();
    JsonObject server = root.getAsJsonObject("downloads").getAsJsonObject("server");
    if (!server.get("sha1").getAsString().equals(pins.server().sha1())
        || server.get("size").getAsLong() != pins.server().size()) {
      throw new IOException("Official metadata no longer matches pins.json for the server bundle");
    }
    return server.get("url").getAsString();
  }

  private void fetchServer(String url) throws IOException, InterruptedException {
    downloader.fetch(
        URI.create(url), layout.serverJar(), pins.server().sha1(), pins.server().size());
    String sha256 = Hashes.hex(layout.serverJar(), "SHA-256");
    if (!sha256.equals(pins.server().sha256())) {
      throw new IOException(
          "Server bundle SHA-256 " + sha256 + " instead of " + pins.server().sha256());
    }
  }

  private boolean upToDate() throws IOException {
    if (!Files.isRegularFile(layout.state())) {
      return false;
    }
    State state =
        GSON.fromJson(Files.readString(layout.state(), StandardCharsets.UTF_8), State.class);
    return state != null
        && state.gameVersion().equals(pins.gameVersion())
        && state.serverSha256().equals(pins.server().sha256())
        && state.innerServerSha256().equals(pins.innerServerSha256())
        && state.resourceFiles() > 0
        && (!generate || state.generatedFiles() > 0);
  }

  /** Checks the identity file of the bundle against the pinned versions and keeps a copy. */
  private void checkIdentity(ZipFile bundle) throws IOException {
    ZipEntry entry = requireEntry(bundle, "version.json");
    byte[] bytes;
    try (InputStream in = bundle.getInputStream(entry)) {
      bytes = in.readAllBytes();
    }
    JsonObject identity =
        JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    if (!identity.get("id").getAsString().equals(pins.gameVersion())
        || identity.get("world_version").getAsInt() != pins.worldVersion()
        || identity.get("protocol_version").getAsInt() != pins.protocolVersion()) {
      throw new IOException("Bundle identity differs from pins.json: " + identity);
    }
    Files.createDirectories(layout.reference());
    Files.write(layout.identity(), bytes);
  }

  /** Verifies the inner server archive listed by the bundle against the list and the pin. */
  private InnerJar checkInnerJar(ZipFile bundle) throws IOException {
    ZipEntry list = requireEntry(bundle, "META-INF/versions.list");
    String content;
    try (InputStream in = bundle.getInputStream(list)) {
      content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    for (String line : (Iterable<String>) content.lines()::iterator) {
      String[] fields = line.split("\t", -1);
      if (fields.length == 3 && fields[1].equals(pins.gameVersion())) {
        String actual;
        try (InputStream in = bundle.getInputStream(innerEntry(bundle, fields[2]))) {
          actual = Hashes.hex(in, "SHA-256");
        }
        if (!actual.equals(fields[0]) || !actual.equals(pins.innerServerSha256())) {
          throw new IOException(
              "Inner server archive SHA-256 " + actual + " differs from the list or the pin");
        }
        return new InnerJar(fields[2], actual);
      }
    }
    throw new IOException("versions.list has no entry for " + pins.gameVersion());
  }

  private int extractResources(ZipFile bundle, String innerPath) throws IOException {
    try (InputStream in = bundle.getInputStream(innerEntry(bundle, innerPath))) {
      return ArchiveExtractor.extract(
          in,
          layout.resources(),
          name ->
              !name.endsWith(".class") && (name.startsWith("data/") || name.startsWith("assets/")));
    }
  }

  private static ZipEntry innerEntry(ZipFile bundle, String path) throws IOException {
    return requireEntry(bundle, "META-INF/versions/" + path);
  }

  private static ZipEntry requireEntry(ZipFile bundle, String name) throws IOException {
    ZipEntry entry = bundle.getEntry(name);
    if (entry == null) {
      throw new IOException("Bundle has no entry " + name);
    }
    return entry;
  }

  private static void clear(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
        Files.delete(path);
      }
    }
  }

  private static int countFiles(Path dir) throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      return (int) paths.filter(Files::isRegularFile).count();
    }
  }

  private static void log(String message) {
    System.out.println("bootstrap: " + message);
  }
}
