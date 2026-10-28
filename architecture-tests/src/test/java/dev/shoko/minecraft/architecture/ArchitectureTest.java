package dev.shoko.minecraft.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "dev.shoko.minecraft",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "dev.shoko.minecraft";

  @ArchTest static final ArchRule MODULES_DEPEND_DOWNWARDS = ModuleRules.layers(ROOT);

  @ArchTest static final ArchRule NO_PACKAGE_CYCLES = ModuleRules.noPackageCycles(ROOT);

  @ArchTest static final ArchRule NO_TOOL_CYCLES = ModuleRules.noToolCycles(ROOT);

  @ArchTest static final ArchRule ENGINE_IS_DETERMINISTIC = ModuleRules.determinism(ROOT);
}
