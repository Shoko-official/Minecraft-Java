package dev.shoko.minecraft.architecture;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Facts about the Gradle modules, passed by architecture-tests/build.gradle.kts. */
final class BuildModules {

  private BuildModules() {}

  /** Every code module, named like the keys of {@link ModuleRules#DIRECT}. */
  static Set<String> all() {
    return read("architecture.modules");
  }

  /** Modules with Java sources other than package-info. */
  static Set<String> withCode() {
    return read("architecture.modulesWithCode");
  }

  private static Set<String> read(String property) {
    String value = System.getProperty(property);
    if (value == null) {
      throw new IllegalStateException(
          "system property " + property + " is missing: run the tests through Gradle");
    }
    Set<String> modules = new LinkedHashSet<>();
    Arrays.stream(value.split(",")).filter(m -> !m.isEmpty()).forEach(modules::add);
    return modules;
  }
}
