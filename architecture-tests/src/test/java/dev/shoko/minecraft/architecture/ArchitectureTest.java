package dev.shoko.minecraft.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AnalyzeClasses(
    packages = ArchitectureTest.ROOT,
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  static final String ROOT = "dev.shoko.minecraft";

  private static final List<String> TOOLS =
      ModuleRules.DIRECT.keySet().stream().filter(m -> m.startsWith("tools.")).toList();

  @ArchTest
  static final ArchRule MODULES_DEPEND_DOWNWARDS = ModuleRules.layers(ROOT, modulesWithoutCode());

  @ArchTest
  static final ArchRule CLASSES_LIVE_IN_MODULES =
      ModuleRules.modulePackagesOnly(ROOT).allowEmptyShould(noCodeIn(ModuleRules.DIRECT.keySet()));

  @ArchTest
  static final ArchRule NO_PACKAGE_CYCLES =
      ModuleRules.noPackageCycles(ROOT).allowEmptyShould(noCodeIn(ModuleRules.DIRECT.keySet()));

  @ArchTest
  static final ArchRule NO_TOOL_CYCLES =
      ModuleRules.noToolCycles(ROOT).allowEmptyShould(noCodeIn(TOOLS));

  @ArchTest
  static final ArchRule ENGINE_IS_DETERMINISTIC =
      ModuleRules.determinism(ROOT).allowEmptyShould(noCodeIn(ModuleRules.ENGINE));

  @ArchTest
  static final ArchRule GSON_IS_CONFINED =
      ModuleRules.gsonConfinement(ROOT)
          .allowEmptyShould(noCodeIn(ModuleRules.gsonConfinedModules()));

  @ArchTest
  static final ArchRule GSON_IS_NOT_EXPOSED =
      ModuleRules.noGsonInSignatures(ROOT).allowEmptyShould(noCodeIn(List.of("data")));

  @ArchTest
  static final ArchRule GSON_IS_NOT_A_SUPERTYPE =
      ModuleRules.noGsonInSupertypes(ROOT).allowEmptyShould(noCodeIn(List.of("data")));

  /**
   * A rule may be empty only while none of the modules it covers has code. The import check below
   * makes sure that code, once written, is actually seen by the rules.
   */
  private static boolean noCodeIn(Collection<String> modules) {
    return Collections.disjoint(BuildModules.withCode(), modules);
  }

  private static Set<String> modulesWithoutCode() {
    Set<String> modules = new LinkedHashSet<>(ModuleRules.DIRECT.keySet());
    modules.removeAll(BuildModules.withCode());
    return modules;
  }

  @ArchTest
  static void everyModuleWithCodeIsImported(JavaClasses classes) {
    List<String> missing =
        BuildModules.withCode().stream()
            .filter(m -> classes.that(resideInAPackage(ROOT + "." + m + "..")).isEmpty())
            .toList();
    assertThat(missing).as("modules with sources but no imported class").isEmpty();
  }

  @ArchTest
  static void packagesStayWithinBudget(JavaClasses classes) {
    assertThat(PackageBudget.oversized(classes, PackageBudget.MAX_CLASSES))
        .as("packages over %d top-level classes", PackageBudget.MAX_CLASSES)
        .isEmpty();
  }
}
