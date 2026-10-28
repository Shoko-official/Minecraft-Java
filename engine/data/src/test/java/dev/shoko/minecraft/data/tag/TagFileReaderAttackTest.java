package dev.shoko.minecraft.data.tag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.RandomJson;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R32, R54, R56, R62 g, R63 c (adversarial): random JSON read as a tag
// file against a model of R32 written here. Fixed seed.
class TagFileReaderAttackTest {

  private static final List<String> KEYS = List.of("values", "replace", "id", "required", "zzz");
  private static final List<String> SCALARS =
      List.of(
          "true",
          "false",
          "null",
          "1",
          "1.5",
          "\"a\"",
          "\"#t\"",
          "\"minecraft:b\"",
          "\"Bad:Id\"",
          "\"\"",
          "\"#\"",
          "\"a:b:c\"",
          "[]",
          "{}");

  /** The entries of the file when R32 accepts it, or empty when it is invalid. */
  private static Optional<List<TagEntry>> model(JsonNode root) {
    if (!(root instanceof JsonObject object)) {
      return Optional.empty();
    }
    if (object.get("replace").map(n -> !(n instanceof JsonBoolean)).orElse(false)) {
      return Optional.empty();
    }
    if (!(object.get("values").orElse(null) instanceof JsonArray values)) {
      return Optional.empty();
    }
    List<TagEntry> entries = new ArrayList<>();
    for (JsonNode entry : values.elements()) {
      Optional<TagEntry> parsed = entry(entry);
      if (parsed.isEmpty()) {
        return Optional.empty();
      }
      entries.add(parsed.get());
    }
    return Optional.of(entries);
  }

  private static Optional<TagEntry> entry(JsonNode node) {
    if (node instanceof JsonString string) {
      return reference(string.value(), true);
    }
    if (node instanceof JsonObject object) {
      if (!(object.get("id").orElse(null) instanceof JsonString id)) {
        return Optional.empty();
      }
      Optional<JsonNode> required = object.get("required");
      if (required.isPresent() && !(required.get() instanceof JsonBoolean)) {
        return Optional.empty();
      }
      boolean flag = required.map(n -> ((JsonBoolean) n).value()).orElse(true);
      return reference(id.value(), flag);
    }
    return Optional.empty();
  }

  private static Optional<TagEntry> reference(String text, boolean required) {
    boolean tag = text.startsWith("#");
    return Identifier.tryParse(tag ? text.substring(1) : text)
        .map(id -> new TagEntry(id, tag, required));
  }

  @Test
  @Timeout(60)
  void provisionalRandomJsonIsAcceptedExactlyWhenTheModelAcceptsIt() {
    // spec: R32 (validity), R54 (an invalid file is absent), R62 g (errors reported)
    Random random = new Random(0x7A65L);
    RandomJson generator = new RandomJson(random, KEYS, SCALARS, 4);
    int accepted = 0;
    for (int i = 0; i < 20_000; i++) {
      String text = generator.next();
      if (text.contains("\"id\":\"#")) {
        continue; // a tag reference written as the id of an object entry is not pinned down by R32
      }
      JsonNode root = Jx.parseLenient(text);
      Optional<List<TagEntry>> expected = model(root);
      Outcome<TagFile> outcome = TagFileReader.read(root);
      if (expected.isPresent()) {
        accepted++;
        assertThat(outcome.diagnostics()).as(text).isEmpty();
        assertThat(outcome.value().orElseThrow().entries()).as(text).isEqualTo(expected.get());
      } else {
        assertThat(outcome.value()).as(text).isEmpty();
        assertThat(outcome.hasErrors()).as(text).isTrue();
      }
    }
    assertThat(accepted).as("valid samples drawn").isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalAValidFileWithManySeveralEntriesKeepsAllOfThem() {
    // spec: R32
    StringBuilder text = new StringBuilder("{\"values\":[");
    for (int i = 0; i < 50_000; i++) {
      text.append(i == 0 ? "" : ",");
      text.append(i % 3 == 0 ? "\"#t" + i + "\"" : "{\"id\":\"e" + i + "\",\"required\":false}");
    }
    Outcome<TagFile> outcome = TagFileReader.read(Jx.parse(text.append("]}").toString()));
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value().orElseThrow().entries()).hasSize(50_000);
  }
}
