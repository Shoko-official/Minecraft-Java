package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Runs the whole tool against a fake release served from loopback: a bundle built in memory with
 * its identity file, its {@code versions.list} and an inner archive mixing resources and code.
 */
class BootstrapTest {

  private static final String VERSION = "26.3";
  private static final String STICK = "data/minecraft/recipe/stick.json";
  private static final String LANG = "assets/minecraft/lang/en_us.json";
  private static final String LANG_TEXT = "{\"key\":\"" + "x".repeat(2000) + "\"}";
  private static final Bootstrap.Options OPTIONS =
      new Bootstrap.Options(false, false, ArchiveExtractor.Limits.DEFAULT);

  @TempDir Path dir;
  private final AtomicInteger requests = new AtomicInteger();
  private HttpServer server;
  private byte[] metadata;
  private byte[] bundle;
  private Path cache;
  private CacheLayout layout;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/26.3.json", exchange -> serve(exchange, metadata));
    server.createContext("/server.jar", exchange -> serve(exchange, bundle));
    server.start();
    cache = dir.resolve("cache");
    layout = new CacheLayout(cache, VERSION);
  }

  private void serve(HttpExchange exchange, byte[] body) throws IOException {
    requests.incrementAndGet();
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private static byte[] zip(Map<String, byte[]> entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream out = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        out.putNextEntry(new ZipEntry(entry.getKey()));
        out.write(entry.getValue());
        out.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /** The inner server archive: two resources, code inside and outside data/, a root file. */
  private static byte[] innerJar() throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put(STICK, utf8("{\"type\":\"crafting_shaped\"}"));
    entries.put(LANG, utf8(LANG_TEXT));
    entries.put("data/minecraft/Sneaky.class", utf8("code"));
    entries.put("net/minecraft/server/Main.class", utf8("code"));
    entries.put("pack.mcmeta", utf8("{}"));
    return zip(entries);
  }

  private static JsonObject identity() {
    JsonObject pack = new JsonObject();
    pack.addProperty("resource_major", 97);
    pack.addProperty("resource_minor", 1);
    pack.addProperty("data_major", 121);
    pack.addProperty("data_minor", 0);
    JsonObject identity = new JsonObject();
    identity.addProperty("id", VERSION);
    identity.addProperty("world_version", 5023);
    identity.addProperty("protocol_version", 777);
    identity.add("pack_version", pack);
    return identity;
  }

  private static byte[] bundle(JsonObject identity, byte[] inner, String listedSha256)
      throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("version.json", utf8(identity.toString()));
    entries.put(
        "META-INF/versions.list", utf8(listedSha256 + "\t" + VERSION + "\tserver-26.3.jar\n"));
    entries.put("META-INF/versions/server-26.3.jar", inner);
    return zip(entries);
  }

  /** Serves {@code served} and returns pins that match it, with the given inner archive pin. */
  private Pins publish(byte[] served, String innerPin) {
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    JsonObject download = new JsonObject();
    download.addProperty("sha1", Hashes.hex(served, "SHA-1"));
    download.addProperty("size", served.length);
    download.addProperty("url", base + "/server.jar");
    JsonObject downloads = new JsonObject();
    downloads.add("server", download);
    JsonObject root = new JsonObject();
    root.add("downloads", downloads);
    bundle = served;
    metadata = utf8(root.toString());
    return new Pins(
        VERSION,
        new Pins.VersionJson(base + "/26.3.json", Hashes.hex(metadata, "SHA-1")),
        new Pins.Server(Hashes.hex(served, "SHA-1"), Hashes.hex(served, "SHA-256"), served.length),
        innerPin,
        5023,
        777,
        new Pins.PackVersion("121.0", "97.1"));
  }

  private Pins publishGood() throws IOException {
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    return publish(bundle(identity(), inner, innerSha256), innerSha256);
  }

  private Bootstrap.Outcome run(Pins pins, Bootstrap.Options options) throws Exception {
    return new Bootstrap(pins, layout, new Downloader(), options).run();
  }

  private List<String> resourceFiles() throws IOException {
    try (Stream<Path> paths = Files.walk(layout.resources())) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> layout.resources().relativize(path).toString().replace('\\', '/'))
          .sorted()
          .toList();
    }
  }

  @Test
  void extractsTheResourcesAndNoCode() throws Exception {
    Bootstrap.Outcome outcome = run(publishGood(), OPTIONS);

    assertThat(outcome).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
    assertThat(layout.identity()).hasContent(identity().toString());
    assertThat(new StateStore(layout.state()).read())
        .hasValueSatisfying(state -> assertThat(state.resources().files()).isEqualTo(2));
  }

  @Test
  void classFilesUnderDataAreNotExtracted() throws Exception {
    run(publishGood(), OPTIONS);

    assertThat(layout.resources().resolve("data/minecraft/Sneaky.class")).doesNotExist();
    assertThat(layout.resources().resolve("net")).doesNotExist();
    assertThat(layout.resources().resolve("pack.mcmeta")).doesNotExist();
  }

  @Test
  void secondRunDownloadsAndExtractsNothing() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    int requestsAfterFirstRun = requests.get();

    Bootstrap.Outcome outcome = run(pins, OPTIONS);

    assertThat(outcome).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
    assertThat(requests).hasValue(requestsAfterFirstRun);
  }

  @Test
  void rejectsABundleWithAnotherSha256AndDoesNotKeepIt() throws Exception {
    Pins good = publishGood();
    Pins wrong =
        new Pins(
            good.gameVersion(),
            good.versionJson(),
            new Pins.Server(good.server().sha1(), "0".repeat(64), good.server().size()),
            good.innerServerSha256(),
            good.worldVersion(),
            good.protocolVersion(),
            good.packVersion());

    assertThatThrownBy(() -> run(wrong, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-256");
    assertThat(layout.serverJar()).doesNotExist();
    assertThat(layout.reference()).doesNotExist();
  }

  @Test
  void rejectsAnInnerArchiveThatDiffersFromTheList() throws Exception {
    byte[] inner = innerJar();
    String actual = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity(), inner, "0".repeat(64)), actual);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Inner server archive");
    assertThat(layout.resources()).doesNotExist();
  }

  @Test
  void rejectsAnInnerArchiveThatDiffersFromThePin() throws Exception {
    byte[] inner = innerJar();
    String actual = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity(), inner, actual), "0".repeat(64));

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Inner server archive");
    assertThat(layout.resources()).doesNotExist();
  }

  @ParameterizedTest
  @CsvSource({
    "id, 26.2",
    "world_version, 5022",
    "protocol_version, 776",
    "pack_version.data_major, 120",
    "pack_version.data_minor, 1",
    "pack_version.resource_major, 96",
    "pack_version.resource_minor, 0"
  })
  void rejectsABundleWithAnotherIdentity(String field, String value) throws Exception {
    JsonObject identity = identity();
    JsonObject owner = identity;
    String name = field;
    if (field.startsWith("pack_version.")) {
      owner = identity.getAsJsonObject("pack_version");
      name = field.substring("pack_version.".length());
    }
    if (name.equals("id")) {
      owner.addProperty(name, value);
    } else {
      owner.addProperty(name, Integer.parseInt(value));
    }
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity, inner, innerSha256), innerSha256);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("identity differs");
    assertThat(layout.resources()).doesNotExist();
  }

  @Test
  void rejectsAnIdentityWithAMissingFieldWithoutANullPointerException() throws Exception {
    JsonObject identity = identity();
    identity.remove("protocol_version");
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity, inner, innerSha256), innerSha256);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("field protocol_version");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "garbage{",
        "{}",
        "{\"gameVersion\":\"26.3\"}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"other\",\"innerServerSha256\":\"x\","
            + "\"resources\":{\"files\":2,\"listing\":\"l\",\"content\":\"c\"}}"
      })
  void rebuildsOverAMalformedOrObsoleteState(String state) throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.state(), state);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"gameVersion", "serverSha256", "innerServerSha256"})
  void rebuildsOverAStateRecordedForOtherPins(String field) throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    StateStore store = new StateStore(layout.state());
    StateStore.State state = store.read().orElseThrow();
    store.write(
        new StateStore.State(
            field.equals("gameVersion") ? "26.2" : state.gameVersion(),
            field.equals("serverSha256") ? "other" : state.serverSha256(),
            field.equals("innerServerSha256") ? "other" : state.innerServerSha256(),
            state.resources(),
            state.generated()));

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
  }

  @Test
  void rejectsMetadataThatNoLongerMatchesThePins() throws Exception {
    Pins good = publishGood();
    Pins otherSize =
        new Pins(
            good.gameVersion(),
            good.versionJson(),
            new Pins.Server(good.server().sha1(), good.server().sha256(), good.server().size() + 1),
            good.innerServerSha256(),
            good.worldVersion(),
            good.protocolVersion(),
            good.packVersion());

    assertThatThrownBy(() -> run(otherSize, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Official metadata no longer matches");
  }

  @Test
  void rebuildsWhenAnExtractedFileIsDeleted() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.delete(layout.resources().resolve(STICK));

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
  }

  @Test
  void rebuildsWhenAFileIsAdded() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve("data/minecraft/extra.json"), "{}");

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
  }

  @Test
  void rebuildsWhenAnExtractedFileIsTruncated() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve(LANG), "{}");

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void sameSizeRewriteIsOnlySeenWithVerifyContent() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve(LANG), LANG_TEXT.replace('x', 'y'));
    Bootstrap.Options verifyContent =
        new Bootstrap.Options(false, true, ArchiveExtractor.Limits.DEFAULT);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
    assertThat(run(pins, verifyContent)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void anInterruptedExtractionInvalidatesTheStateAndIsRedoneFromScratch() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.delete(layout.resources().resolve(STICK));
    // The language file is larger than this limit: extraction fails after the first resource.
    Bootstrap.Options failing =
        new Bootstrap.Options(false, false, new ArchiveExtractor.Limits(1000, 1L << 30));

    assertThatThrownBy(() -> run(pins, failing))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("expands beyond");
    assertThat(layout.resources().resolve(STICK)).exists();
    assertThat(layout.state()).doesNotExist();

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void aJunctionPlantedInTheResourcesIsRemovedWithoutTouchingItsTarget() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Path outside = Files.createDirectories(dir.resolve("outside"));
    Path keep = Files.writeString(outside.resolve("keep.txt"), "keep");
    TestLinks.directoryLink(layout.resources().resolve("data/escape"), outside);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(keep).hasContent("keep");
    assertThat(layout.resources().resolve("data/escape")).doesNotExist();
  }

  private static int execute(ByteArrayOutputStream err, String... args) {
    return Bootstrap.execute(
        List.of(args), null, new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  @Test
  void anUnknownArgumentPrintsTheUsage() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    assertThat(execute(err, "--bogus")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("--bogus").contains("usage:");
  }

  @Test
  void aMissingOrInvalidCacheDirectoryPrintsTheUsage() {
    ByteArrayOutputStream missing = new ByteArrayOutputStream();
    ByteArrayOutputStream invalid = new ByteArrayOutputStream();

    assertThat(execute(missing, "--cache-dir")).isEqualTo(2);
    assertThat(execute(invalid, "--cache-dir", "bad\0path")).isEqualTo(2);
    assertThat(missing.toString(StandardCharsets.UTF_8)).contains("usage:");
    assertThat(invalid.toString(StandardCharsets.UTF_8)).contains("usage:");
  }

  @Test
  void aFailureReportsTheExceptionTypeWithoutTheUsage() throws IOException {
    Path notADirectory = Files.writeString(dir.resolve("cache-file"), "x");
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    assertThat(execute(err, "--cache-dir", notADirectory.toString(), "--no-generate")).isEqualTo(1);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .startsWith("bootstrap failed: java.nio.file.")
        .doesNotContain("usage:");
  }
}
