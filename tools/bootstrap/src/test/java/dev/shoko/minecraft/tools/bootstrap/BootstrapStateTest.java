package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reuse, invalidation and rebuild of the cache from its recorded state and file listing. */
class BootstrapStateTest extends BootstrapFixture {

  @Test
  void secondRunDownloadsAndExtractsNothing() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    int requestsAfterFirstRun = requests.get();

    Bootstrap.Outcome outcome = run(pins, OPTIONS);

    assertThat(outcome).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
    assertThat(requests).hasValue(requestsAfterFirstRun);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "garbage{",
        "{}",
        "{\"gameVersion\":\"26.3\"}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"other\",\"innerServerSha256\":\"x\","
            + "\"resources\":{\"files\":2,\"listing\":\"l\",\"content\":\"c\"}}"
      })
  void rebuildsOverAMalformedOrObsoleteState(String state) throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.state(), state);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"gameVersion", "serverSha256", "innerServerSha256"})
  void rebuildsOverAStateRecordedForOtherPins(String field) throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    StateStore store = new StateStore(layout.state());
    StateStore.State state = store.read().orElseThrow();
    store.write(
        new StateStore.State(
            field.equals("gameVersion") ? "26.2" : state.gameVersion(),
            field.equals("serverSha256") ? "other" : state.serverSha256(),
            field.equals("innerServerSha256") ? "other" : state.innerServerSha256(),
            state.resources(),
            state.generated()));

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
  }

  @Test
  void rebuildsWhenAnExtractedFileIsDeleted() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.delete(layout.resources().resolve(STICK));

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
  }

  @Test
  void rebuildsWhenAFileIsAdded() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve("data/minecraft/extra.json"), "{}");

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
  }

  @Test
  void rebuildsWhenAnExtractedFileIsTruncated() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve(LANG), "{}");

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void sameSizeRewriteIsOnlySeenWithVerifyContent() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.writeString(layout.resources().resolve(LANG), LANG_TEXT.replace('x', 'y'));
    Bootstrap.Options verifyContent =
        new Bootstrap.Options(false, true, ArchiveExtractor.Limits.DEFAULT);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.ALREADY_VERIFIED);
    assertThat(run(pins, verifyContent)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void anInterruptedExtractionInvalidatesTheStateAndIsRedoneFromScratch() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Files.delete(layout.resources().resolve(STICK));
    // The language file is larger than this limit: extraction fails after the first resource.
    Bootstrap.Options failing =
        new Bootstrap.Options(false, false, new ArchiveExtractor.Limits(1000, 1L << 30));

    assertThatThrownBy(() -> run(pins, failing))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("expands beyond");
    assertThat(layout.resources().resolve(STICK)).exists();
    assertThat(layout.state()).doesNotExist();

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
  }

  @Test
  void aJunctionPlantedInTheResourcesIsRemovedWithoutTouchingItsTarget() throws Exception {
    Pins pins = publishGood();
    run(pins, OPTIONS);
    Path outside = Files.createDirectories(dir.resolve("outside"));
    Path keep = Files.writeString(outside.resolve("keep.txt"), "keep");
    TestLinks.directoryLink(layout.resources().resolve("data/escape"), outside);

    assertThat(run(pins, OPTIONS)).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(keep).hasContent("keep");
    assertThat(layout.resources().resolve("data/escape")).doesNotExist();
  }
}
