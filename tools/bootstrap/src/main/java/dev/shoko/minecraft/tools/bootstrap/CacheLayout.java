package dev.shoko.minecraft.tools.bootstrap;

import java.nio.file.Path;

/**
 * Where bootstrap keeps its files. Everything lives under one cache directory that git ignores.
 *
 * <pre>
 * downloads/&lt;version&gt;.json          official version metadata
 * downloads/server-&lt;version&gt;.jar     official server bundle
 * reference/&lt;version&gt;/version.json   identity read from the bundle
 * reference/&lt;version&gt;/resources/     data and assets resources of the server (no code)
 * reference/&lt;version&gt;/generated/     output of the official data generator
 * reference/&lt;version&gt;/state.json     what was verified, to skip repeated work
 * </pre>
 */
record CacheLayout(Path root, String version) {

  Path versionJson() {
    return root.resolve("downloads").resolve(version + ".json");
  }

  Path serverJar() {
    return root.resolve("downloads").resolve("server-" + version + ".jar");
  }

  Path reference() {
    return root.resolve("reference").resolve(version);
  }

  Path identity() {
    return reference().resolve("version.json");
  }

  Path resources() {
    return reference().resolve("resources");
  }

  Path generated() {
    return reference().resolve("generated");
  }

  Path generatorWork() {
    return reference().resolve("work");
  }

  Path state() {
    return reference().resolve("state.json");
  }
}
