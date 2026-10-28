package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StateStoreTest {

  private static final SafeTree.Fingerprint TREE = new SafeTree.Fingerprint(3, "list", "content");
  private static final StateStore.State STATE =
      new StateStore.State("26.3", "outer", "inner", TREE, TREE);

  @Test
  void writtenStateIsReadBack(@TempDir Path dir) throws IOException {
    StateStore store = new StateStore(dir.resolve("state.json"));

    store.write(STATE);

    assertThat(store.read()).contains(STATE);
    assertThat(dir).isDirectoryContaining(path -> path.endsWith("state.json"));
    assertThat(dir.resolve("state.json.tmp")).doesNotExist();
  }

  @Test
  void writeReplacesAPreviousStateAndALeftoverTemporaryFile(@TempDir Path dir) throws IOException {
    StateStore store = new StateStore(dir.resolve("state.json"));
    Files.writeString(dir.resolve("state.json"), "old");
    Files.writeString(dir.resolve("state.json.tmp"), "leftover of a killed run");

    store.write(STATE);

    assertThat(store.read()).contains(STATE);
    assertThat(dir.resolve("state.json.tmp")).doesNotExist();
  }

  @Test
  void stateWithoutGeneratedTreeIsComplete(@TempDir Path dir) throws IOException {
    StateStore store = new StateStore(dir.resolve("state.json"));
    StateStore.State withoutGenerated = new StateStore.State("26.3", "outer", "inner", TREE, null);

    store.write(withoutGenerated);

    assertThat(store.read()).contains(withoutGenerated);
  }

  @Test
  void missingStateIsEmpty(@TempDir Path dir) {
    assertThat(new StateStore(dir.resolve("state.json")).read()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "garbage{",
        "{}",
        "[]",
        "null",
        "\"text\"",
        "{\"gameVersion\":\"26.3\"}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServerSha256\":\"b\","
            + "\"resourceFiles\":9887,\"generatedFiles\":1674}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServerSha256\":\"b\","
            + "\"resources\":{\"files\":3,\"listing\":\"l\"}}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServerSha256\":\"b\","
            + "\"resources\":5}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServerSha256\":\"b\","
            + "\"resources\":{\"files\":1e40,\"listing\":\"l\",\"content\":\"c\"}}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServerSha256\":\"b\","
            + "\"resources\":{\"files\":3,\"listing\":\"l\",\"content\":\"c\"},"
            + "\"generated\":{\"files\":3}}",
        "{\"gameVersion\":\"26.3\",\"serverSha256\":\"a\",\"innerServer"
      })
  void malformedOrPartialStateIsEmptyNotAnError(String text, @TempDir Path dir) throws IOException {
    Path file = Files.writeString(dir.resolve("state.json"), text);

    assertThat(new StateStore(file).read()).isEmpty();
  }

  @Test
  void stateThatIsNotUtf8IsEmpty(@TempDir Path dir) throws IOException {
    Path file = Files.write(dir.resolve("state.json"), new byte[] {'{', (byte) 0xff, '}'});

    assertThat(new StateStore(file).read()).isEmpty();
  }

  @Test
  void stateThatIsADirectoryIsEmpty(@TempDir Path dir) throws IOException {
    Path file = Files.createDirectories(dir.resolve("state.json"));

    assertThat(new StateStore(file).read()).isEmpty();
  }

  @Test
  void invalidateDeletesTheState(@TempDir Path dir) throws IOException {
    StateStore store = new StateStore(dir.resolve("state.json"));
    store.write(STATE);

    store.invalidate();
    store.invalidate();

    assertThat(store.read()).isEmpty();
    assertThat(dir).isEmptyDirectory();
  }
}
