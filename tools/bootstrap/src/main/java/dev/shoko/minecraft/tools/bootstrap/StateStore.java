package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Reads and writes {@code state.json}, the record of what a run verified.
 *
 * <p>The protocol makes a stale record impossible to trust: the file is deleted before any
 * extracted tree is touched, and written last, through a temporary file moved atomically, so a run
 * killed at any point leaves either no record or a complete one. A record that cannot be read is
 * simply absent: it never stops the tool, it only makes the next run redo the work.
 */
final class StateStore {

  /**
   * What was verified.
   *
   * @param generated null when the data generator was not run
   */
  record State(
      String gameVersion,
      String serverSha256,
      String innerServerSha256,
      SafeTree.Fingerprint resources,
      SafeTree.Fingerprint generated) {

    /** Whether every field a check relies on is present (Gson leaves missing fields null). */
    boolean isComplete() {
      return gameVersion != null
          && serverSha256 != null
          && innerServerSha256 != null
          && isComplete(resources)
          && (generated == null || isComplete(generated));
    }

    private static boolean isComplete(SafeTree.Fingerprint fingerprint) {
      return fingerprint != null && fingerprint.listing() != null && fingerprint.content() != null;
    }
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final Path file;

  StateStore(Path file) {
    this.file = file;
  }

  /** Returns the recorded state, or empty when it is absent, unreadable, malformed or partial. */
  Optional<State> read() {
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      State state = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
      return state != null && state.isComplete() ? Optional.of(state) : Optional.empty();
    } catch (IOException | JsonParseException e) {
      return Optional.empty();
    }
  }

  /** Deletes the record; to be called before anything it describes is modified. */
  void invalidate() throws IOException {
    Files.deleteIfExists(file);
  }

  void write(State state) throws IOException {
    Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
    Files.writeString(temporary, GSON.toJson(state), StandardCharsets.UTF_8);
    Files.move(
        temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }
}
