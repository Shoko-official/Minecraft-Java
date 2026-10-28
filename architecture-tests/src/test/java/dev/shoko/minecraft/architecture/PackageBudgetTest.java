package dev.shoko.minecraft.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class PackageBudgetTest {

  private static final String BUDGET = "dev.shoko.minecraft.architecture.fixture.budget";

  private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages(BUDGET);

  @Test
  void packageOverTheMaximumIsReported() {
    assertThat(PackageBudget.oversized(FIXTURES, 2)).containsExactly(entry(BUDGET + ".big", 3));
  }

  @Test
  void nestedClassesAndPackageInfoAreNotCounted() {
    assertThat(PackageBudget.oversized(FIXTURES, 1))
        .containsExactly(entry(BUDGET + ".big", 3), entry(BUDGET + ".small", 2));
  }

  @Test
  void packageAtTheMaximumIsAccepted() {
    assertThat(PackageBudget.oversized(FIXTURES, 3)).isEmpty();
  }
}
