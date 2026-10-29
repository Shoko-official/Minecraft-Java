package dev.shoko.minecraft.architecture;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * Architecture rules of docs/ARCHITECTURE.md as code. The direct dependencies below are the single
 * source of truth for package-level checks; Gradle enforces the same graph at compile time, and
 * ModuleGraphTest keeps both the documentation table and the Gradle modules equal to it.
 */
final class ModuleRules {

  /** Direct dependencies of each module, by package suffix under the root package. */
  static final Map<String, Set<String>> DIRECT = directDependencies();

  /** Modules whose code must be deterministic: no clock, no unseeded randomness. */
  static final List<String> ENGINE =
      List.of("core", "data", "world", "item", "loot", "worldgen", "entity", "game");

  /** Test-only code under the root package, outside every module. */
  static final String ARCHITECTURE_PACKAGE = "architecture";

  /** The JSON library of the data module (ADR 0006). */
  private static final String GSON = "com.google.gson..";

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

  /**
   * One layer per module; the layers of {@code optional} may be empty. For a layered architecture,
   * allowEmptyShould is the same switch as optional layers, so modules without code are passed here
   * instead.
   */
  static Architectures.LayeredArchitecture layers(String root, Set<String> optional) {
    Architectures.LayeredArchitecture architecture =
        Architectures.layeredArchitecture().consideringOnlyDependenciesInLayers();
    for (String module : DIRECT.keySet()) {
      architecture =
          (optional.contains(module)
                  ? architecture.optionalLayer(module)
                  : architecture.layer(module))
              .definedBy(root + "." + module + "..");
    }
    for (String module : DIRECT.keySet()) {
      String[] allowed = reachable(module).toArray(String[]::new);
      architecture =
          allowed.length == 0
              ? architecture.whereLayer(module).mayNotAccessAnyLayer()
              : architecture.whereLayer(module).mayOnlyAccessLayers(allowed);
    }
    return architecture;
  }

  /** Every class under the root package belongs to a module, so that no rule can miss it. */
  static ArchRule modulePackagesOnly(String root) {
    String[] modules =
        DIRECT.keySet().stream().map(m -> root + "." + m + "..").toArray(String[]::new);
    return classes()
        .that()
        .resideInAPackage(root + "..")
        .and()
        .resideOutsideOfPackage(root + "." + ARCHITECTURE_PACKAGE + "..")
        .should()
        .resideInAnyPackage(modules)
        .because("a class outside the module packages escapes every architecture rule");
  }

  static ArchRule noPackageCycles(String root) {
    return slices().matching(root + ".(*)..").should().beFreeOfCycles();
  }

  static ArchRule noToolCycles(String root) {
    return slices().matching(root + ".tools.(*)..").should().beFreeOfCycles();
  }

  /**
   * The engine takes time and randomness from explicit, seeded sources only. RandomGenerator covers
   * Random, SplittableRandom, ThreadLocalRandom and SecureRandom; the engine's own generators do
   * not implement it. InstantSource covers Clock.
   */
  static ArchRule determinism(String root) {
    String[] packages = ENGINE.stream().map(m -> root + "." + m + "..").toArray(String[]::new);
    return noClasses()
        .that()
        .resideInAnyPackage(packages)
        .should()
        .dependOnClassesThat()
        .areAssignableTo(RandomGenerator.class)
        .orShould()
        .dependOnClassesThat()
        .areAssignableTo(InstantSource.class)
        .orShould()
        .callMethodWhere(targetOwner(resideInAPackage("java.time..")).and(target(name("now"))))
        .orShould()
        .callMethod(UUID.class, "randomUUID")
        .orShould()
        .callMethod(Collections.class, "shuffle", List.class)
        .orShould()
        .callMethod(Math.class, "random")
        .orShould()
        .callMethod(StrictMath.class, "random")
        .orShould()
        .callMethod(System.class, "currentTimeMillis")
        .orShould()
        .callMethod(System.class, "nanoTime")
        .because("engine behavior must not depend on wall-clock time or unseeded randomness");
  }

  /**
   * ADR 0006: within the engine, Gson is used by the data JSON package only, so that replacing the
   * library touches one package. The tools are programs of their own, with their own dependency on
   * Gson.
   */
  static ArchRule gsonConfinement(String root) {
    String[] packages = ENGINE.stream().map(m -> root + "." + m + "..").toArray(String[]::new);
    return noClasses()
        .that()
        .resideInAnyPackage(packages)
        .and()
        .resideOutsideOfPackage(root + ".data.json..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(GSON)
        .because("ADR 0006 confines Gson to the data JSON package");
  }

  /**
   * ADR 0006: no Gson type in the signature a data class shows to its callers. The Gradle
   * dependency is {@code implementation}, which hides Gson from the compile classpath of the other
   * modules only; this rule also covers a type that leaks through a generic argument, an array or a
   * throws clause.
   */
  static ArchRule noGsonInSignatures(String root) {
    DescribedPredicate<JavaMember> exported =
        DescribedPredicate.describe(
            "public or protected",
            member ->
                member.getModifiers().contains(JavaModifier.PUBLIC)
                    || member.getModifiers().contains(JavaModifier.PROTECTED));
    return members()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage(root + ".data..")
        .and(exported)
        .should(notExposeGson())
        .because("ADR 0006 keeps Gson out of every public signature of the data module");
  }

  private static ArchCondition<JavaMember> notExposeGson() {
    return new ArchCondition<>("not have a Gson type in their signature") {
      @Override
      public void check(JavaMember member, ConditionEvents events) {
        signatureTypes(member)
            .filter(type -> type.getPackageName().startsWith("com.google.gson"))
            .forEach(
                type ->
                    events.add(
                        SimpleConditionEvent.violated(
                            member,
                            member.getDescription()
                                + " exposes "
                                + type.getName()
                                + " in its signature in "
                                + member.getSourceCodeLocation())));
      }
    };
  }

  /** Every class the signature of a member involves: generic arguments and array elements too. */
  private static Stream<JavaClass> signatureTypes(JavaMember member) {
    if (member instanceof JavaField field) {
      return field.getType().getAllInvolvedRawTypes().stream();
    }
    JavaCodeUnit unit = (JavaCodeUnit) member;
    Stream<JavaType> declared =
        Stream.concat(Stream.of(unit.getReturnType()), unit.getParameterTypes().stream());
    return Stream.concat(
        declared.flatMap(type -> type.getAllInvolvedRawTypes().stream()),
        unit.getThrowsClause().getTypes().stream());
  }
}
