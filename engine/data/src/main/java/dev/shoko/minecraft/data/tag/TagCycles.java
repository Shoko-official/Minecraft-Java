package dev.shoko.minecraft.data.tag;

import java.util.Arrays;

/**
 * Strongly connected components of the reference graph between tags, found without recursion so
 * that a chain of any length is safe (R37). A tag is cyclic when its component holds several tags
 * or when it references itself.
 */
final class TagCycles {

  private final int[] component;
  private final boolean[] cyclic;

  private TagCycles(int[] component, boolean[] cyclic) {
    this.component = component;
    this.cyclic = cyclic;
  }

  boolean isCyclic(int tag) {
    return cyclic[tag];
  }

  /** Tells whether two tags are in the same component. */
  boolean sameComponent(int a, int b) {
    return component[a] == component[b];
  }

  /**
   * Computes the components (Tarjan, with an explicit stack).
   *
   * @param edges for each tag, the tags it references
   */
  static TagCycles of(int[][] edges) {
    int n = edges.length;
    int[] order = new int[n];
    int[] low = new int[n];
    int[] component = new int[n];
    int[] nextEdge = new int[n];
    boolean[] onStack = new boolean[n];
    Arrays.fill(order, -1);
    Arrays.fill(component, -1);
    int[] tarjan = new int[n];
    int[] calls = new int[n];
    int tarjanSize = 0;
    int counter = 0;
    int components = 0;
    for (int root = 0; root < n; root++) {
      if (order[root] != -1) {
        continue;
      }
      int depth = 0;
      calls[depth++] = root;
      order[root] = counter;
      low[root] = counter++;
      tarjan[tarjanSize++] = root;
      onStack[root] = true;
      while (depth > 0) {
        int node = calls[depth - 1];
        if (nextEdge[node] < edges[node].length) {
          int next = edges[node][nextEdge[node]++];
          if (order[next] == -1) {
            order[next] = counter;
            low[next] = counter++;
            tarjan[tarjanSize++] = next;
            onStack[next] = true;
            calls[depth++] = next;
          } else if (onStack[next]) {
            low[node] = Math.min(low[node], order[next]);
          }
          continue;
        }
        depth--;
        if (depth > 0) {
          int parent = calls[depth - 1];
          low[parent] = Math.min(low[parent], low[node]);
        }
        if (low[node] == order[node]) {
          int member;
          do {
            member = tarjan[--tarjanSize];
            onStack[member] = false;
            component[member] = components;
          } while (member != node);
          components++;
        }
      }
    }
    return new TagCycles(component, markCyclic(edges, component, components));
  }

  private static boolean[] markCyclic(int[][] edges, int[] component, int components) {
    int[] size = new int[components];
    for (int c : component) {
      size[c]++;
    }
    boolean[] cyclic = new boolean[edges.length];
    for (int tag = 0; tag < edges.length; tag++) {
      cyclic[tag] = size[component[tag]] > 1;
      for (int target : edges[tag]) {
        if (target == tag) {
          cyclic[tag] = true;
        }
      }
    }
    return cyclic;
  }
}
