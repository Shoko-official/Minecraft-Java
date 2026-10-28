package dev.shoko.minecraft.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Architecture rules of docs/ARCHITECTURE.md as code. The direct dependencies below are the single
 * source of truth for package-level checks; Gradle enforces the same graph at compile time.
 */
final class ModuleRules {

  /** Direct dependencies of each module, by package suffix under the root package. */
  static final Map<String, Set<String>> DIRECT = directDependencies();

  /** Modules whose code must be deterministic: no clock, no unseeded randomness. */
  static final List<String> ENGINE =
      List.of("core", "data", "world", "item", "loot", "worldgen", "entity", "game");

  private ModuleRules() {}

  private static Map<String, Set<String>> directDependencies() {
    Map<String, Set<String>> m = new LinkedHashMap<>();
    m.put("core", Set.of());
    m.put("data", Set.of("core"));
    m.put("world", Set.of("data"));
    m.put("item", Set.of("data"));
    m.put("loot", Set.of("world", "item"));
    m.put("worldgen", Set.of("world"));
    m.put("entity", Set.of("world", "item", "loot"));
    m.put("game", Set.of("worldgen", "entity", "loot"));
    m.put("net", Set.of("game"));
    m.put("server", Set.of("net"));
    m.put("client", Set.of("net"));
    m.put("tools.bootstrap", Set.of());
    m.put("tools.oracle", Set.of());
    m.put("tools.diff", Set.of("world", "worldgen", "entity", "loot", "game", "tools.oracle"));
    return m;
  }

  /** A module may use its direct dependencies and everything they reach. */
  static Set<String> reachable(String module) {
    Set<String> result = new LinkedHashSet<>();
    List<String> pending = new ArrayList<>(DIRECT.get(module));
    while (!pending.isEmpty()) {
      String next = pending.remove(pending.size() - 1);
      if (result.add(next)) {
        pending.addAll(DIRECT.get(next));
      }
    }
    return result;
  }

  static ArchRule layers(String root) {
    Architectures.LayeredArchitecture architecture =
        Architectures.layeredArchitecture().consideringOnlyDependenciesInLayers();
    for (String module : DIRECT.keySet()) {
      architecture = architecture.layer(module).definedBy(root + "." + module + "..");
    }
    for (String module : DIRECT.keySet()) {
      String[] allowed = reachable(module).toArray(String[]::new);
      architecture =
          allowed.length == 0
              ? architecture.whereLayer(module).mayNotAccessAnyLayer()
              : architecture.whereLayer(module).mayOnlyAccessLayers(allowed);
    }
    return architecture.withOptionalLayers(true);
  }

  static ArchRule noPackageCycles(String root) {
    return slices().matching(root + ".(*)..").should().beFreeOfCycles();
  }

  static ArchRule noToolCycles(String root) {
    return slices().matching(root + ".tools.(*)..").should().beFreeOfCycles();
  }

  /** The engine takes time and randomness from explicit, seeded sources only. */
  static ArchRule determinism(String root) {
    String[] packages = ENGINE.stream().map(m -> root + "." + m + "..").toArray(String[]::new);
    return noClasses()
        .that()
        .resideInAnyPackage(packages)
        .should()
        .dependOnClassesThat()
        .areAssignableTo(java.util.Random.class)
        .orShould()
        .callMethod(Math.class, "random")
        .orShould()
        .callMethod(System.class, "currentTimeMillis")
        .orShould()
        .callMethod(System.class, "nanoTime")
        .because("engine behavior must not depend on wall-clock time or unseeded randomness");
  }
}
