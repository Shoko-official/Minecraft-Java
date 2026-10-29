package dev.shoko.minecraft.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import dev.shoko.minecraft.architecture.fixture.cycle.a.CycleA;
import dev.shoko.minecraft.architecture.fixture.cycle.b.CycleB;
import dev.shoko.minecraft.architecture.fixture.cycle.tools.alpha.ToolAlpha;
import dev.shoko.minecraft.architecture.fixture.cycle.tools.beta.ToolBeta;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesClock;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesCollectionsShuffle;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesCurrentTimeMillis;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesDefaultRandomGenerator;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesHijrahDateNow;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesInstantNow;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesInstantSource;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesMathRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesNanoTime;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesRandomUuid;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesSecureRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesSplittableRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesStrictMathRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesThreadLocalRandom;
import dev.shoko.minecraft.architecture.fixture.determinism.core.UsesZonedDateTimeNow;
import dev.shoko.minecraft.architecture.fixture.gson.data.catalog.GsonInMethodBody;
import dev.shoko.minecraft.architecture.fixture.gson.data.codec.GsonInPrivateField;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonConstructorParameter;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonInArrayParameter;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonInGenericReturn;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonParameter;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonProtectedMethod;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonPublicField;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonReturned;
import dev.shoko.minecraft.architecture.fixture.gson.data.json.GsonThrown;
import dev.shoko.minecraft.architecture.fixture.gson.world.GsonOutsideData;
import dev.shoko.minecraft.architecture.fixture.layers.core.CoreUsesWorld;
import dev.shoko.minecraft.architecture.fixture.layers.item.ItemLeaf;
import dev.shoko.minecraft.architecture.fixture.layers.item.ItemUsesWorld;
import dev.shoko.minecraft.architecture.fixture.layers.tools.oracle.OracleUsesWorld;
import dev.shoko.minecraft.architecture.fixture.layers.world.WorldLeaf;
import dev.shoko.minecraft.architecture.fixture.layers.world.WorldUsesItem;
import dev.shoko.minecraft.architecture.fixture.stray.StrayAtRoot;
import dev.shoko.minecraft.architecture.fixture.stray.blocks.StrayBlock;
import dev.shoko.minecraft.architecture.fixture.stray.engine.core.StrayEngineCore;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The rules must report each injected violation, otherwise a green build proves nothing. Every
 * fixture holds a single violation, and each test checks that the report names that fixture and the
 * forbidden target, so that a rule weakened for one case cannot hide behind another fixture.
 */
class RuleEffectivenessTest {

  private static final String FIXTURE = "dev.shoko.minecraft.architecture.fixture";
  private static final String LAYERS = FIXTURE + ".layers";
  private static final String CYCLE = FIXTURE + ".cycle";
  private static final String STRAY = FIXTURE + ".stray";
  private static final String DETERMINISM = FIXTURE + ".determinism";
  private static final String GOOD = FIXTURE + ".good";
  private static final String GSON = FIXTURE + ".gson";

  /** Fixtures import a few classes only, so every layer may be empty. */
  private static final ArchRule LAYERED_FIXTURE =
      ModuleRules.layers(LAYERS, ModuleRules.DIRECT.keySet());

  private static JavaClasses classes(Class<?>... classes) {
    return new ClassFileImporter().importClasses(classes);
  }

  /** Every reported violation comes from {@code origin}, and one of them reaches {@code target}. */
  private static void assertViolation(EvaluationResult result, Class<?> origin, String target) {
    assertThat(result.getFailureReport().getDetails())
        .isNotEmpty()
        .allMatch(detail -> detail.contains(origin.getName()))
        .anyMatch(detail -> detail.contains(target));
  }

  @Test
  void coreCannotDependOnWorld() {
    assertViolation(
        LAYERED_FIXTURE.evaluate(classes(CoreUsesWorld.class, WorldLeaf.class)),
        CoreUsesWorld.class,
        WorldLeaf.class.getName());
  }

  @Test
  void worldCannotDependOnItem() {
    assertViolation(
        LAYERED_FIXTURE.evaluate(classes(WorldUsesItem.class, ItemLeaf.class)),
        WorldUsesItem.class,
        ItemLeaf.class.getName());
  }

  @Test
  void itemCannotDependOnWorld() {
    assertViolation(
        LAYERED_FIXTURE.evaluate(classes(ItemUsesWorld.class, WorldLeaf.class)),
        ItemUsesWorld.class,
        WorldLeaf.class.getName());
  }

  @Test
  void toolsOracleCannotUseTheEngine() {
    assertViolation(
        LAYERED_FIXTURE.evaluate(classes(OracleUsesWorld.class, WorldLeaf.class)),
        OracleUsesWorld.class,
        WorldLeaf.class.getName());
  }

  @Test
  void packageCycleIsDetected() {
    assertViolation(
        ModuleRules.noPackageCycles(CYCLE).evaluate(classes(CycleA.class, CycleB.class)),
        CycleA.class,
        CycleB.class.getName());
  }

  @Test
  void cycleBetweenToolsIsDetected() {
    assertViolation(
        ModuleRules.noToolCycles(CYCLE).evaluate(classes(ToolAlpha.class, ToolBeta.class)),
        ToolAlpha.class,
        ToolBeta.class.getName());
  }

  @ParameterizedTest
  @MethodSource("classesOutsideModules")
  void classOutsideTheModulePackagesIsDetected(Class<?> stray) {
    assertViolation(
        ModuleRules.modulePackagesOnly(STRAY).evaluate(classes(stray)), stray, stray.getName());
  }

  static Stream<Class<?>> classesOutsideModules() {
    return Stream.of(StrayAtRoot.class, StrayBlock.class, StrayEngineCore.class);
  }

  @ParameterizedTest
  @MethodSource("forbiddenClockAndRandomness")
  void clockAndUnseededRandomnessAreDetectedInTheEngine(Class<?> fixture, String target) {
    assertViolation(
        ModuleRules.determinism(DETERMINISM).evaluate(classes(fixture)), fixture, target);
  }

  static Stream<Arguments> forbiddenClockAndRandomness() {
    return Stream.of(
        Arguments.of(UsesMathRandom.class, "java.lang.Math.random()"),
        Arguments.of(UsesStrictMathRandom.class, "java.lang.StrictMath.random()"),
        Arguments.of(UsesCurrentTimeMillis.class, "java.lang.System.currentTimeMillis()"),
        Arguments.of(UsesNanoTime.class, "java.lang.System.nanoTime()"),
        Arguments.of(UsesRandomUuid.class, "java.util.UUID.randomUUID()"),
        Arguments.of(UsesCollectionsShuffle.class, "java.util.Collections.shuffle(java.util.List)"),
        Arguments.of(UsesInstantNow.class, "java.time.Instant.now()"),
        Arguments.of(UsesZonedDateTimeNow.class, "java.time.ZonedDateTime.now(java.time.ZoneId)"),
        Arguments.of(UsesHijrahDateNow.class, "java.time.chrono.HijrahDate.now(java.time.ZoneId)"),
        Arguments.of(UsesClock.class, "java.time.Clock"),
        Arguments.of(UsesInstantSource.class, "java.time.InstantSource"),
        Arguments.of(UsesRandom.class, "java.util.Random"),
        Arguments.of(UsesSplittableRandom.class, "java.util.SplittableRandom"),
        Arguments.of(UsesThreadLocalRandom.class, "java.util.concurrent.ThreadLocalRandom"),
        Arguments.of(UsesSecureRandom.class, "java.security.SecureRandom"),
        Arguments.of(UsesDefaultRandomGenerator.class, "java.util.random.RandomGenerator"));
  }

  @ParameterizedTest
  @MethodSource("gsonOutsideTheJsonPackage")
  void gsonOutsideTheDataJsonPackageIsDetected(Class<?> fixture, String target) {
    assertViolation(ModuleRules.gsonConfinement(GSON).evaluate(classes(fixture)), fixture, target);
  }

  static Stream<Arguments> gsonOutsideTheJsonPackage() {
    return Stream.of(
        Arguments.of(GsonInMethodBody.class, "com.google.gson.JsonObject"),
        Arguments.of(GsonInPrivateField.class, "com.google.gson.Gson"),
        Arguments.of(GsonOutsideData.class, "com.google.gson.JsonObject"));
  }

  @ParameterizedTest
  @MethodSource("gsonInPublicSignatures")
  void gsonInAPublicOrProtectedSignatureIsDetected(Class<?> fixture, String target) {
    assertViolation(
        ModuleRules.noGsonInSignatures(GSON).evaluate(classes(fixture)), fixture, target);
  }

  static Stream<Arguments> gsonInPublicSignatures() {
    return Stream.of(
        Arguments.of(GsonReturned.class, "com.google.gson.JsonObject"),
        Arguments.of(GsonParameter.class, "com.google.gson.JsonElement"),
        Arguments.of(GsonConstructorParameter.class, "com.google.gson.stream.JsonReader"),
        Arguments.of(GsonPublicField.class, "com.google.gson.JsonObject"),
        Arguments.of(GsonProtectedMethod.class, "com.google.gson.JsonObject"),
        Arguments.of(GsonInGenericReturn.class, "com.google.gson.JsonElement"),
        Arguments.of(GsonInArrayParameter.class, "com.google.gson.JsonElement"),
        Arguments.of(GsonThrown.class, "com.google.gson.JsonParseException"));
  }

  @Test
  void downwardDependenciesAndAllowedApisAreAccepted() {
    JavaClasses good = new ClassFileImporter().importPackages(GOOD);
    assertThat(good.stream().count()).isPositive();
    assertThat(ModuleRules.layers(GOOD, ModuleRules.DIRECT.keySet()).evaluate(good).hasViolation())
        .isFalse();
    assertThat(ModuleRules.modulePackagesOnly(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.determinism(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.noPackageCycles(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.gsonConfinement(GOOD).evaluate(good).hasViolation()).isFalse();
    assertThat(ModuleRules.noGsonInSignatures(GOOD).evaluate(good).hasViolation()).isFalse();
  }
}
