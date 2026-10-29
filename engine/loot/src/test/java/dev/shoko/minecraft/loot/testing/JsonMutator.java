package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNull;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mutates JSON documents with a seeded random source: members deleted, values replaced by odd ones
 * (huge numbers, wrong kinds, odd identifiers), values copied from another place, wrapped, nested
 * deeply, strings altered, unknown members added. The output is text; a mutant that the JSON reader
 * refuses is for the caller to skip.
 */
public final class JsonMutator {

  private static final List<String> POOL =
      List.of(
          "null",
          "true",
          "false",
          "0",
          "-1",
          "1",
          "2",
          "3",
          "64",
          "99",
          "255",
          "256",
          "2147483647",
          "2147483648",
          "-2147483649",
          "9223372036854775807",
          "99999999999999999999",
          "1e308",
          "-1e308",
          "1e400",
          "4.9e-324",
          "0.5",
          "-0.0",
          "\"\"",
          "\" \"",
          "\"#\"",
          "\"#minecraft:\"",
          "\"minecraft:\"",
          "\"minecraft:stone\"",
          "\"#minecraft:test_gems\"",
          "\"minecraft:no_such\"",
          "\"A:B\"",
          "\"..\"",
          "\"minecraft:stone:extra\"",
          "\"\\u00e9#\"",
          "\"\\u0000\"",
          "[]",
          "{}");

  private static final List<String> STRING_EDITS = List.of("#", " ", ":x", "/", "", "\\t");

  /** A mutable JSON tree: a scalar kept as its text, an object or an array. */
  private static final class Tree {
    private int kind; // 0 scalar, 1 object, 2 array
    private String raw = "null";
    private Map<String, Tree> members = new LinkedHashMap<>();
    private List<Tree> elements = new ArrayList<>();

    static Tree scalar(String raw) {
      Tree tree = new Tree();
      tree.raw = raw;
      return tree;
    }

    Tree copy() {
      Tree tree = new Tree();
      tree.kind = kind;
      tree.raw = raw;
      members.forEach((key, value) -> tree.members.put(key, value.copy()));
      elements.forEach(value -> tree.elements.add(value.copy()));
      return tree;
    }

    void become(Tree other) {
      kind = other.kind;
      raw = other.raw;
      members = other.members;
      elements = other.elements;
    }

    void write(StringBuilder out) {
      switch (kind) {
        case 1 -> {
          out.append('{');
          boolean first = true;
          for (Map.Entry<String, Tree> member : members.entrySet()) {
            if (!first) {
              out.append(',');
            }
            first = false;
            out.append('"').append(escape(member.getKey())).append("\":");
            member.getValue().write(out);
          }
          out.append('}');
        }
        case 2 -> {
          out.append('[');
          for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
              out.append(',');
            }
            elements.get(i).write(out);
          }
          out.append(']');
        }
        default -> out.append(raw);
      }
    }
  }

  private JsonMutator() {}

  private static String escape(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static Tree fromNode(JsonNode node) {
    Tree tree = new Tree();
    switch (node) {
      case JsonObject object -> {
        tree.kind = 1;
        object.members().forEach((key, value) -> tree.members.put(key, fromNode(value)));
      }
      case JsonArray array -> {
        tree.kind = 2;
        array.elements().forEach(value -> tree.elements.add(fromNode(value)));
      }
      case JsonString string -> tree.raw = "\"" + escape(string.value()) + "\"";
      case JsonNumber number -> tree.raw = number.literal();
      case JsonBoolean bool -> tree.raw = Boolean.toString(bool.value());
      case JsonNull ignored -> tree.raw = "null";
    }
    return tree;
  }

  private static void collect(Tree tree, List<Tree> nodes, IdentityHashMap<Tree, Tree> parents) {
    nodes.add(tree);
    for (Tree child : tree.members.values()) {
      parents.put(child, tree);
      collect(child, nodes, parents);
    }
    for (Tree child : tree.elements) {
      parents.put(child, tree);
      collect(child, nodes, parents);
    }
  }

  private static void nest(Tree target, int depth) {
    for (int i = 0; i < depth; i++) {
      Tree inner = new Tree();
      inner.become(target);
      Tree outer = new Tree();
      outer.kind = 2;
      outer.elements.add(inner);
      target.become(outer);
    }
  }

  private static void editString(Tree target, Random random) {
    if (target.kind != 0 || !target.raw.startsWith("\"") || target.raw.length() < 2) {
      target.become(Tree.scalar(POOL.get(random.nextInt(POOL.size()))));
      return;
    }
    String inner = target.raw.substring(1, target.raw.length() - 1);
    String edit = STRING_EDITS.get(random.nextInt(STRING_EDITS.size()));
    String edited =
        switch (random.nextInt(4)) {
          case 0 -> edit + inner;
          case 1 -> inner + edit;
          case 2 -> inner + inner;
          default -> edit;
        };
    target.raw = "\"" + edited + "\"";
  }

  private static void mutateOnce(Tree root, Random random) {
    List<Tree> nodes = new ArrayList<>();
    IdentityHashMap<Tree, Tree> parents = new IdentityHashMap<>();
    collect(root, nodes, parents);
    Tree target = nodes.get(random.nextInt(nodes.size()));
    Tree parent = parents.get(target);
    switch (random.nextInt(8)) {
      case 0 -> {
        if (parent == null) {
          target.become(Tree.scalar("null"));
        } else {
          parent.members.values().removeIf(child -> child == target);
          parent.elements.removeIf(child -> child == target);
        }
      }
      case 1 -> target.become(Tree.scalar(POOL.get(random.nextInt(POOL.size()))));
      case 2 -> target.become(nodes.get(random.nextInt(nodes.size())).copy());
      case 3 -> nest(target, 1);
      case 4 -> {
        Tree inner = new Tree();
        inner.become(target);
        Tree outer = new Tree();
        outer.kind = 1;
        outer.members.put("x", inner);
        target.become(outer);
      }
      case 5 -> nest(target, 20 + random.nextInt(100));
      case 6 -> editString(target, random);
      default -> {
        Tree host = target.kind == 1 ? target : root;
        if (host.kind == 1) {
          host.members.put("unknown_" + random.nextInt(3), Tree.scalar(POOL.get(1)));
        }
      }
    }
  }

  /** One mutant: one to three mutations of the document. */
  public static String mutate(JsonNode document, Random random) {
    Tree root = fromNode(document);
    int rounds = 1 + random.nextInt(3);
    for (int i = 0; i < rounds; i++) {
      mutateOnce(root, random);
    }
    StringBuilder out = new StringBuilder();
    root.write(out);
    return out.toString();
  }
}
