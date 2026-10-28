package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Merges the tag files of the packs and resolves the result (R33 to R37). */
public final class TagResolver {

  private final RegistryKey<?> registry;
  private final Set<Identifier> universe;
  private final List<Identifier> ids;
  private final List<MergedTag> tags;
  // Lookup only: never iterated, so its order is not observable.
  private final Map<Identifier, Integer> position = new HashMap<>();
  private int[][] references;
  private TagCycles cycles;
  private boolean[] failed;

  private TagResolver(
      RegistryKey<?> registry, Set<Identifier> universe, Map<Identifier, MergedTag> merged) {
    this.registry = registry;
    this.universe = universe;
    this.ids = new ArrayList<>(merged.keySet());
    this.tags = new ArrayList<>(merged.values());
    for (int i = 0; i < ids.size(); i++) {
      position.put(ids.get(i), i);
    }
  }

  /**
   * Merges and resolves the tags of one registry.
   *
   * <ul>
   *   <li>Merge (R34): for a tag, the layers go from the lowest to the highest priority; a layer
   *       with {@code replace} drops everything accumulated before it, any other adds its entries
   *       after the previous ones.
   *   <li>Resolution (R35): entries are visited in order; a {@code #tag} entry is replaced by the
   *       resolved content of that tag, depth first; an element already present is not repeated.
   *   <li>Failure (R36): a required element missing from the universe, or a required tag that does
   *       not exist or failed, makes the tag fail. Each such entry gives one {@code
   *       CODEC_UNRESOLVED_REFERENCE} error, in the file of the layer that holds the entry, at
   *       {@code $.values[i]} with {@code i} the position of the entry in that file. An entry with
   *       {@code required} false is simply skipped. The other tags keep loading.
   *   <li>Cycles (R37): every tag of a cycle, and every tag that requires one, fails. The entry
   *       that requires a tag of the cycle, or a tag that failed, gives the same error as above;
   *       the R61 table has no code of its own for a cycle (provisional).
   * </ul>
   *
   * The result is always present: failures are in {@link TagResolution#failed()} and in the
   * diagnostics. The resolved lists are fully expanded (R35), so a chain of n tags that each add one
   * element has an output of the order of n squared entries; nothing here bounds it.
   *
   * @param registry the registry the tags belong to, used in messages
   * @param layers the layers of each tag identifier, from the lowest to the highest priority; a tag
   *     file that was invalid is simply absent from its list (R54)
   * @param universe the identifiers of the elements a tag may contain (R33)
   * @return the resolution and its diagnostics
   */
  public static Outcome<TagResolution> resolve(
      RegistryKey<?> registry,
      SequencedMap<Identifier, List<TagSource>> layers,
      Set<Identifier> universe) {
    TagResolver run = new TagResolver(registry, universe, MergedTag.mergeAll(layers));
    run.findFailures();
    return Outcome.success(run.result(), run.diagnostics());
  }

  private void findFailures() {
    int count = ids.size();
    references = new int[count][];
    for (int i = 0; i < count; i++) {
      references[i] =
          tags.get(i).items().stream()
              .filter(item -> item.entry().tag() && position.containsKey(item.entry().id()))
              .mapToInt(item -> position.get(item.entry().id()))
              .toArray();
    }
    cycles = TagCycles.of(references);
    failed = new boolean[count];
    List<List<Integer>> dependents = new ArrayList<>();
    Deque<Integer> pending = new ArrayDeque<>();
    for (int i = 0; i < count; i++) {
      dependents.add(new ArrayList<>());
    }
    for (int i = 0; i < count; i++) {
      for (MergedTag.Item item : tags.get(i).items()) {
        Integer target = item.entry().tag() ? position.get(item.entry().id()) : null;
        if (target != null && item.entry().required()) {
          dependents.get(target).add(i);
        }
      }
      if (cycles.isCyclic(i) || hasMissingRequired(i)) {
        failed[i] = true;
        pending.add(i);
      }
    }
    while (!pending.isEmpty()) {
      for (int dependent : dependents.get(pending.poll())) {
        if (!failed[dependent]) {
          failed[dependent] = true;
          pending.add(dependent);
        }
      }
    }
  }

  private boolean hasMissingRequired(int tag) {
    for (MergedTag.Item item : tags.get(tag).items()) {
      TagEntry entry = item.entry();
      boolean missing =
          entry.tag() ? !position.containsKey(entry.id()) : !universe.contains(entry.id());
      if (missing && entry.required()) {
        return true;
      }
    }
    return false;
  }

  private TagResolution result() {
    Map<Identifier, List<Identifier>> resolved = new TreeMap<>();
    TreeSet<Identifier> failures = new TreeSet<>();
    List<List<Identifier>> members = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      members.add(null);
      if (failed[i]) {
        failures.add(ids.get(i));
      }
    }
    for (int tag : postOrder()) {
      List<Identifier> expanded = expand(tag, members);
      members.set(tag, expanded);
      resolved.put(ids.get(tag), expanded);
    }
    return new TagResolution(resolved, failures);
  }

  /** Orders the tags that did not fail so that every tag comes after the tags it expands. */
  private List<Integer> postOrder() {
    int count = ids.size();
    List<Integer> order = new ArrayList<>();
    boolean[] seen = new boolean[count];
    int[] cursor = new int[count];
    Deque<Integer> stack = new ArrayDeque<>();
    for (int root = 0; root < count; root++) {
      if (failed[root] || seen[root]) {
        continue;
      }
      seen[root] = true;
      stack.push(root);
      while (!stack.isEmpty()) {
        int node = stack.peek();
        int[] targets = references[node];
        if (cursor[node] < targets.length) {
          int next = targets[cursor[node]++];
          if (!failed[next] && !seen[next]) {
            seen[next] = true;
            stack.push(next);
          }
        } else {
          stack.pop();
          order.add(node);
        }
      }
    }
    return order;
  }

  private List<Identifier> expand(int tag, List<List<Identifier>> done) {
    Set<Identifier> out = new LinkedHashSet<>();
    for (MergedTag.Item item : tags.get(tag).items()) {
      TagEntry entry = item.entry();
      if (!entry.tag()) {
        if (universe.contains(entry.id())) {
          out.add(entry.id());
        }
      } else {
        Integer target = position.get(entry.id());
        if (target != null && !failed[target]) {
          out.addAll(done.get(target));
        }
      }
    }
    return List.copyOf(out);
  }

  private List<Diagnostic> diagnostics() {
    List<Diagnostic> found = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      for (MergedTag.Item item : tags.get(i).items()) {
        String problem = problem(i, item.entry());
        if (problem != null) {
          JsonPath path = JsonPath.root().field("values").index(item.index());
          String message = "tag " + registry.name() + " #" + ids.get(i) + ": " + problem;
          found.add(
              new Diagnostic(
                  DiagnosticCode.CODEC_UNRESOLVED_REFERENCE,
                  item.source().origin(),
                  path,
                  message));
        }
      }
    }
    return found;
  }

  /** Describes why an entry cannot be satisfied, or returns null when it can or may be skipped. */
  private String problem(int tag, TagEntry entry) {
    if (!entry.tag()) {
      boolean missing = !universe.contains(entry.id());
      return missing && entry.required() ? "unknown element " + entry.id() : null;
    }
    Integer target = position.get(entry.id());
    if (target == null) {
      return entry.required() ? "unknown tag #" + entry.id() : null;
    }
    if (!failed[target]) {
      return null;
    }
    // An optional reference is skipped, except when it closes a cycle (R37).
    boolean closesCycle = cycles.isCyclic(tag) && cycles.sameComponent(tag, target);
    return entry.required() || closesCycle ? "tag #" + entry.id() + " failed" : null;
  }
}
