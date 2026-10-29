package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.loot.assembly.References.Ref;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the cycles of the references between named elements (LT-R76) with an iterative search of
 * strongly connected components, so that a long chain of references cannot overflow the stack.
 */
final class ReferenceGraph {

  private final Map<Ref, List<Ref>> edges;
  private final Map<Ref, Integer> index = new HashMap<>();
  private final Map<Ref, Integer> low = new HashMap<>();
  private final Deque<Ref> stack = new ArrayDeque<>();
  private final Set<Ref> onStack = new HashSet<>();
  private final Set<Ref> cyclic = new HashSet<>();
  private int counter;

  private ReferenceGraph(Map<Ref, List<Ref>> edges) {
    this.edges = edges;
  }

  /**
   * Lists the nodes that lie on a cycle: members of a component of two or more nodes, or a node
   * that references itself. Edges to nodes that are not keys of the map are ignored.
   */
  static Set<Ref> cyclic(Map<Ref, List<Ref>> edges) {
    ReferenceGraph graph = new ReferenceGraph(edges);
    for (Ref root : edges.keySet()) {
      if (!graph.index.containsKey(root)) {
        graph.visit(root);
      }
    }
    return graph.cyclic;
  }

  /** One frame of the explicit call stack: a node and the position in its edges. */
  private static final class Frame {
    final Ref node;
    final List<Ref> targets;
    int position;

    Frame(Ref node, List<Ref> targets) {
      this.node = node;
      this.targets = targets;
    }
  }

  private void visit(Ref root) {
    Deque<Frame> calls = new ArrayDeque<>();
    open(root, calls);
    while (!calls.isEmpty()) {
      Frame frame = calls.peek();
      if (frame.position < frame.targets.size()) {
        Ref target = frame.targets.get(frame.position++);
        if (!edges.containsKey(target)) {
          continue;
        }
        if (target.equals(frame.node)) {
          cyclic.add(target);
        }
        if (!index.containsKey(target)) {
          open(target, calls);
        } else if (onStack.contains(target)) {
          low.merge(frame.node, index.get(target), Math::min);
        }
      } else {
        calls.pop();
        close(frame.node);
        if (!calls.isEmpty()) {
          low.merge(calls.peek().node, low.get(frame.node), Math::min);
        }
      }
    }
  }

  private void open(Ref node, Deque<Frame> calls) {
    index.put(node, counter);
    low.put(node, counter);
    counter++;
    stack.push(node);
    onStack.add(node);
    calls.push(new Frame(node, edges.get(node)));
  }

  private void close(Ref node) {
    if (!low.get(node).equals(index.get(node))) {
      return;
    }
    List<Ref> component = new ArrayList<>();
    Ref member;
    do {
      member = stack.pop();
      onStack.remove(member);
      component.add(member);
    } while (!member.equals(node));
    if (component.size() > 1) {
      cyclic.addAll(component);
    }
  }
}
