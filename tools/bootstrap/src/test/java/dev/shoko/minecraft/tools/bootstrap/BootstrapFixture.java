package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.junit.jupiter.api.io.TempDir;

/**
 * Fake release served from loopback for the bootstrap tests: a bundle built in memory with its
 * identity file, its {@code versions.list} and an inner archive mixing resources and code.
 */
abstract class BootstrapFixture {
  static final String VERSION = "26.3";
  static final String STICK = "data/minecraft/recipe/stick.json";
  static final String LANG = "assets/minecraft/lang/en_us.json";
  static final String LANG_TEXT = "{\"key\":\"" + "x".repeat(2000) + "\"}";
  static final Bootstrap.Options OPTIONS =
      new Bootstrap.Options(false, false, ArchiveExtractor.Limits.DEFAULT);

  @TempDir Path dir;
  final AtomicInteger requests = new AtomicInteger();
  HttpServer server;
  byte[] metadata;
  byte[] bundle;
  Path cache;
  CacheLayout layout;

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

  static byte[] zip(Map<String, byte[]> entries) throws IOException {
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

  static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /** The inner server archive: two resources, code inside and outside data/, a root file. */
  static byte[] innerJar() throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put(STICK, utf8("{\"type\":\"crafting_shaped\"}"));
    entries.put(LANG, utf8(LANG_TEXT));
    entries.put("data/minecraft/Sneaky.class", utf8("code"));
    entries.put("net/minecraft/server/Main.class", utf8("code"));
    entries.put("pack.mcmeta", utf8("{}"));
    return zip(entries);
  }

  static JsonObject identity() {
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

  static byte[] bundle(JsonObject identity, byte[] inner, String listedSha256) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("version.json", utf8(identity.toString()));
    entries.put(
        "META-INF/versions.list", utf8(listedSha256 + "\t" + VERSION + "\tserver-26.3.jar\n"));
    entries.put("META-INF/versions/server-26.3.jar", inner);
    return zip(entries);
  }

  /** Serves {@code served} and returns pins that match it, with the given inner archive pin. */
  Pins publish(byte[] served, String innerPin) {
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

  Pins publishGood() throws IOException {
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    return publish(bundle(identity(), inner, innerSha256), innerSha256);
  }

  Bootstrap.Outcome run(Pins pins, Bootstrap.Options options) throws Exception {
    return new Bootstrap(pins, layout, new Downloader(), options).run();
  }

  List<String> resourceFiles() throws IOException {
    try (Stream<Path> paths = Files.walk(layout.resources())) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> layout.resources().relativize(path).toString().replace('\\', '/'))
          .sorted()
          .toList();
    }
  }
}
