package dev.shoko.minecraft.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** The rules must detect injected violations, otherwise a green build proves nothing. */
class RuleEffectivenessTest {

  private static final String BAD = "dev.shoko.minecraft.architecture.fixture.bad";
  private static final String GOOD = "dev.shoko.minecraft.architecture.fixture.good";

  private static JavaClasses classesIn(String root) {
    return new ClassFileImporter().importPackages(root);
  }

  @Test
  void upwardDependencyIsDetected() {
    assertThat(ModuleRules.layers(BAD).evaluate(classesIn(BAD)).hasViolation()).isTrue();
  }

  @Test
  void clockAndUnseededRandomnessAreDetectedInTheEngine() {
    assertThat(ModuleRules.determinism(BAD).evaluate(classesIn(BAD)).hasViolation()).isTrue();
  }

  @Test
  void packageCycleIsDetected() {
    assertThat(ModuleRules.noPackageCycles(BAD).evaluate(classesIn(BAD)).hasViolation()).isTrue();
  }

  @Test
  void downwardDependenciesAreAccepted() {
    JavaClasses good = classesIn(GOOD);
    assertThat(good.stream().count()).isPositive();
    assertThat(ModuleRules.layers(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.determinism(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.noPackageCycles(GOOD).evaluate(good).hasViolation()).isFalse();
  }

  @Test
  void toolsOracleCannotUseTheEngine() {
    JavaClasses bad = classesIn(BAD);
    assertThat(ModuleRules.reachable("tools.oracle")).isEmpty();
    assertThat(ModuleRules.layers(BAD).evaluate(bad).getFailureReport().getDetails())
        .anyMatch(detail -> detail.contains("tools.oracle"));
  }
}
