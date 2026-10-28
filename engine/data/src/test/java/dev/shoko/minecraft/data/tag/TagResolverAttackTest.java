package dev.shoko.minecraft.data.tag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.TagLayers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R33 to R38 (adversarial): random tag graphs checked against a model
// of the rules written here, long chains, wide fan-out and insertion-order independence. Fixed
// seeds.
class TagResolverAttackTest {

  /** A raw entry of a layer: its text in the notation of TagLayers, layer number and position. */
  private record Raw(String text, int layer, int index) {

    boolean isTag() {
      return text.startsWith("#");
    }

    boolean optional() {
      return text.endsWith("?");
    }

    String target() {
      return isTag() ? text.substring(1) : text.substring(0, text.length() - (optional() ? 1 : 0));
    }
  }

  /** Random tags t0..tN, each with layers of entries, over elements e0..e7 (some not existing). */
  private static final class Graph {
    final Map<String, List<List<String>>> files = new LinkedHashMap<>();
    final Map<String, List<Boolean>> replaces = new LinkedHashMap<>();
    final Set<String> universe = new TreeSet<>();

    static Graph random(Random random) {
      Graph graph = new Graph();
      int tags = 1 + random.nextInt(8);
      for (int e = 0; e < 8; e++) {
        if (random.nextInt(4) != 0) {
          graph.universe.add("e" + e);
        }
      }
      for (int t = 0; t < tags; t++) {
        List<List<String>> layers = new ArrayList<>();
        List<Boolean> replace = new ArrayList<>();
        int layerCount = 1 + random.nextInt(3);
        for (int l = 0; l < layerCount; l++) {
          List<String> entries = new ArrayList<>();
          int count = random.nextInt(5);
          for (int k = 0; k < count; k++) {
            if (random.nextInt(3) == 0) {
              entries.add("#t" + random.nextInt(tags + 1));
            } else {
              entries.add("e" + random.nextInt(9) + (random.nextInt(5) == 0 ? "?" : ""));
            }
          }
          layers.add(entries);
          replace.add(random.nextInt(5) == 0);
        }
        graph.files.put("t" + t, layers);
        graph.replaces.put("t" + t, replace);
      }
      return graph;
    }

    TagLayers build(List<String> tagOrder) {
      TagLayers layers = TagLayers.create();
      for (String tag : tagOrder) {
        List<List<String>> layerFiles = files.get(tag);
        for (int i = 0; i < layerFiles.size(); i++) {
          String[] entries = layerFiles.get(i).toArray(String[]::new);
          layers.add(tag, "P" + i, TagLayers.file(replaces.get(tag).get(i), entries));
        }
      }
      return layers;
    }

    Outcome<TagResolution> resolve(List<String> tagOrder) {
      return build(tagOrder).resolve(universe.toArray(String[]::new));
    }

    /** The entries that survive the merge of R34, in order. */
    List<Raw> merged(String tag) {
      List<Raw> result = new ArrayList<>();
      List<List<String>> layers = files.get(tag);
      for (int layer = 0; layer < layers.size(); layer++) {
        if (replaces.get(tag).get(layer)) {
          result.clear();
        }
        for (int index = 0; index < layers.get(layer).size(); index++) {
          result.add(new Raw(layers.get(layer).get(index), layer, index));
        }
      }
      return result;
    }

    @Override
    public String toString() {
      return files + " replace=" + replaces + " universe=" + universe;
    }
  }

  /** The rules R34 to R37, as a reachability computation, independent of the implementation. */
  private static final class Model {
    final Graph graph;
    final Set<String> failed = new TreeSet<>();
    final Map<String, List<String>> members = new LinkedHashMap<>();
    final Set<String> errors = new TreeSet<>();

    Model(Graph graph) {
      this.graph = graph;
      Set<String> bad = new HashSet<>();
      for (String tag : graph.files.keySet()) {
        for (Raw entry : graph.merged(tag)) {
          boolean missingElement =
              !entry.isTag() && !entry.optional() && !graph.universe.contains(entry.target());
          boolean unknownTag = entry.isTag() && !graph.files.containsKey(entry.target());
          if (missingElement || unknownTag) {
            bad.add(tag);
          }
        }
        if (reaches(tag, Set.of(tag))) {
          bad.add(tag);
        }
      }
      for (String tag : graph.files.keySet()) {
        if (bad.contains(tag) || reaches(tag, bad)) {
          failed.add(tag);
        }
      }
      for (String tag : graph.files.keySet()) {
        if (!failed.contains(tag)) {
          members.put(tag, expand(tag));
        }
        for (Raw entry : graph.merged(tag)) {
          boolean missing =
              entry.isTag()
                  ? !graph.files.containsKey(entry.target()) || failed.contains(entry.target())
                  : !graph.universe.contains(entry.target());
          if (missing && !entry.optional()) {
            errors.add("P" + entry.layer() + "|" + tag + "|" + entry.index());
          }
        }
      }
    }

    private List<String> targets(String tag) {
      List<String> result = new ArrayList<>();
      if (graph.files.containsKey(tag)) {
        for (Raw entry : graph.merged(tag)) {
          if (entry.isTag()) {
            result.add(entry.target());
          }
        }
      }
      return result;
    }

    /** Whether a tag of {@code goals} can be reached from {@code from} by one reference or more. */
    private boolean reaches(String from, Set<String> goals) {
      Set<String> visited = new HashSet<>();
      List<String> stack = new ArrayList<>(targets(from));
      while (!stack.isEmpty()) {
        String next = stack.remove(stack.size() - 1);
        if (goals.contains(next)) {
          return true;
        }
        if (visited.add(next)) {
          stack.addAll(targets(next));
        }
      }
      return false;
    }

    private List<String> expand(String tag) {
      List<String> out = new ArrayList<>();
      for (Raw entry : graph.merged(tag)) {
        List<String> added =
            entry.isTag()
                ? expand(entry.target())
                : graph.universe.contains(entry.target()) ? List.of(entry.target()) : List.of();
        for (String element : added) {
          if (!out.contains(element)) {
            out.add(element);
          }
        }
      }
      return out;
    }
  }

  private static String errorKey(Diagnostic d) {
    String file = d.origin().path();
    String tag = file.substring(file.lastIndexOf('/') + 1, file.length() - ".json".length());
    String path = d.path().toString();
    String index = path.substring("$.values[".length(), path.length() - 1);
    return d.origin().pack() + "|" + tag + "|" + index;
  }

  @Test
  @Timeout(60)
  void provisionalRandomGraphsAgreeWithTheModelOfTheRules() {
    // spec: R34, R35, R36, R37 (model: merge, depth-first expansion, failure by reachability)
    Random random = new Random(0x7A6L);
    for (int i = 0; i < 1500; i++) {
      Graph graph = Graph.random(random);
      Model model = new Model(graph);
      Outcome<TagResolution> outcome = graph.resolve(new ArrayList<>(graph.files.keySet()));
      TagResolution result = outcome.value().orElseThrow();
      String context = "graph #" + i + " " + graph;
      assertThat(result.failed().stream().map(Identifier::path).toList())
          .as(context)
          .containsExactlyElementsOf(model.failed);
      assertThat(result.tags().stream().map(Identifier::path).toList())
          .as(context)
          .containsExactlyElementsOf(new TreeSet<>(model.members.keySet()));
      model.members.forEach(
          (tag, expected) ->
              assertThat(
                      result.members(Identifier.parse(tag)).orElseThrow().stream()
                          .map(Identifier::path)
                          .toList())
                  .as(context + " tag " + tag)
                  .isEqualTo(expected));
      assertThat(outcome.diagnostics())
          .as(context)
          .allSatisfy(
              d -> assertThat(d.code()).isEqualTo(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE));
      assertThat(outcome.diagnostics().stream().map(TagResolverAttackTest::errorKey).toList())
          .as(context)
          .containsExactlyInAnyOrderElementsOf(model.errors);
    }
  }

  @Test
  @Timeout(60)
  void provisionalTheOrderTagsAreGivenInDoesNotChangeTheResult() {
    // spec: determinism, R30 (results in code point order whatever the input order)
    Random random = new Random(0x0DE5L);
    for (int i = 0; i < 300; i++) {
      Graph graph = Graph.random(random);
      List<String> forward = new ArrayList<>(graph.files.keySet());
      List<String> shuffled = new ArrayList<>(forward);
      Collections.shuffle(shuffled, random);
      Outcome<TagResolution> one = graph.resolve(forward);
      Outcome<TagResolution> two = graph.resolve(shuffled);
      assertThat(two.value().orElseThrow().failed())
          .as("graph #%s %s", i, graph)
          .containsExactlyElementsOf(one.value().orElseThrow().failed());
      assertThat(two.value().orElseThrow().tags())
          .containsExactlyElementsOf(one.value().orElseThrow().tags());
      for (Identifier tag : one.value().orElseThrow().tags()) {
        assertThat(two.value().orElseThrow().members(tag))
            .isEqualTo(one.value().orElseThrow().members(tag));
      }
      assertThat(Diags.summary(two)).containsExactlyInAnyOrderElementsOf(Diags.summary(one));
    }
  }

  @Test
  @Timeout(60)
  void provisionalAChainOfThousandsOfTagsResolvesWithoutExhaustingTheStack() {
    // spec: R33 (resolution is bounded: no unbounded recursion on a hostile pack)
    int length = 3000;
    TagLayers layers = TagLayers.create();
    for (int i = 0; i < length; i++) {
      layers.add(
          "t" + i, "P", i + 1 < length ? new String[] {"#t" + (i + 1), "a"} : new String[] {"a"});
    }
    Outcome<TagResolution> outcome = layers.resolve("a");
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value().orElseThrow().members(Identifier.parse("t0")).orElseThrow())
        .containsExactly(Identifier.parse("a"));
    assertThat(outcome.value().orElseThrow().tags()).hasSize(length);
  }

  @Test
  @Timeout(60)
  void provisionalALongCycleTerminatesAndFailsEveryMember() {
    // spec: R37 (a hostile cycle never loops forever)
    int length = 2000;
    TagLayers layers = TagLayers.create();
    for (int i = 0; i < length; i++) {
      layers.add("c" + i, "P", "#c" + ((i + 1) % length));
    }
    Outcome<TagResolution> outcome = layers.resolve("a");
    assertThat(outcome.value().orElseThrow().failed()).hasSize(length);
    assertThat(outcome.value().orElseThrow().tags()).isEmpty();
    assertThat(outcome.diagnostics()).hasSize(length);
  }

  @Test
  @Timeout(60)
  void provisionalExponentialFanOutIsResolvedOnceNotPerPath() {
    // spec: R35 (each tag is resolved once; 40 levels of diamonds would be 2^40 paths)
    int levels = 40;
    TagLayers layers = TagLayers.create();
    for (int i = 0; i < levels; i++) {
      layers.add("l" + i, "P", "#m" + i + "a", "#m" + i + "b");
      layers.add("m" + i + "a", "P", "#l" + (i + 1));
      layers.add("m" + i + "b", "P", "#l" + (i + 1));
    }
    layers.add("l" + levels, "P", "a");
    Outcome<TagResolution> outcome = layers.resolve("a");
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value().orElseThrow().members(Identifier.parse("l0")).orElseThrow())
        .containsExactly(Identifier.parse("a"));
  }

  @Test
  @Timeout(60)
  void provisionalAWideTagWithManyEntriesAndDuplicatesKeepsFirstOccurrences() {
    // spec: R35
    String[] entries = new String[20_000];
    for (int i = 0; i < entries.length; i++) {
      entries[i] = "e" + (i % 500);
    }
    String[] universe = new String[500];
    for (int i = 0; i < universe.length; i++) {
      universe[i] = "e" + i;
    }
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P", entries).resolve(universe);
    List<Identifier> members =
        outcome.value().orElseThrow().members(Identifier.parse("t")).orElseThrow();
    assertThat(members).hasSize(500);
    assertThat(members.get(0)).isEqualTo(Identifier.parse("e0"));
    assertThat(members.get(499)).isEqualTo(Identifier.parse("e499"));
  }
}
