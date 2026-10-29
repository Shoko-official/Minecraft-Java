package dev.shoko.minecraft.loot;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the findings of one decoding and turns them into a {@link LootDecoding}. A decoder takes
 * the result of each sub-decoder with {@link #take}, which keeps its findings and hands back the
 * value (or {@code null} after a failure; the value is then never used, because {@link #failed()}
 * is checked before the result is built).
 */
public final class Findings {

  private final List<LootFinding> list = new ArrayList<>();
  private boolean failed;

  /**
   * Records a finding at the place of a JSON node.
   *
   * @param code the cause
   * @param at the node the finding is about
   * @param message for humans
   */
  public void add(LootFindingCode code, JsonNode at, String message) {
    add(code, at.path().toString(), message);
  }

  /**
   * Records a finding at a path.
   *
   * @param code the cause
   * @param path the JSON path
   * @param message for humans
   */
  public void add(LootFindingCode code, String path, String message) {
    list.add(new LootFinding(code, path, message));
    if (code.severity() == Severity.ERROR) {
      failed = true;
    }
  }

  /**
   * Keeps the findings of a sub-decoding.
   *
   * @param decoding the result of a sub-decoder
   * @param <T> its value type
   * @return the value, {@code null} when it failed
   */
  public <T> T take(LootDecoding<T> decoding) {
    list.addAll(decoding.findings());
    if (decoding instanceof LootDecoding.Success<T> success) {
      return success.value();
    }
    failed = true;
    return null;
  }

  /**
   * Keeps all the findings of a list.
   *
   * @param findings findings made elsewhere
   */
  public void addAll(List<LootFinding> findings) {
    for (LootFinding finding : findings) {
      list.add(finding);
      if (finding.code().severity() == Severity.ERROR) {
        failed = true;
      }
    }
  }

  /**
   * Tells whether an error was recorded.
   *
   * @return {@code true} after any finding of severity {@code ERROR}
   */
  public boolean failed() {
    return failed;
  }

  /**
   * Returns the findings made so far.
   *
   * @return an unmodifiable copy
   */
  public List<LootFinding> list() {
    return List.copyOf(list);
  }

  /**
   * Builds the result.
   *
   * @param value the decoded value, used only when nothing failed
   * @param <T> its type
   * @return a success, or a failure when an error was recorded
   */
  public <T> LootDecoding<T> result(T value) {
    if (failed) {
      return new LootDecoding.Failure<>(list());
    }
    return new LootDecoding.Success<>(value, list());
  }

  /**
   * Builds a failure.
   *
   * @param <T> the type a success would have yielded
   * @return a failure carrying the findings made so far
   */
  public <T> LootDecoding<T> failure() {
    return new LootDecoding.Failure<>(list());
  }
}
