package dev.shoko.minecraft.tools.diff;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comparable state of a subsystem: an ordered list of entries with unique hierarchical keys (see
 * {@link StateKey}) and text values. The order is part of the state: the producer emits entries in
 * a canonical order, and the same entries in another order are a different state.
 */
public final class CanonicalState {

  /** One named value of the state. */
  public record Entry(String key, String value) {}

  private final List<Entry> entries;

  private CanonicalState(List<Entry> entries) {
    this.entries = Collections.unmodifiableList(entries);
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<Entry> entries() {
    return entries;
  }

  public int size() {
    return entries.size();
  }

  /** SHA-256 of the whole state, in hexadecimal. */
  public String digest() {
    return digest(entries);
  }

  /** SHA-256 of the entries at or under {@code prefix}, in order; a missing prefix hashes empty. */
  public String digestOf(String prefix) {
    return digest(entries.stream().filter(e -> StateKey.isUnder(e.key(), prefix)).toList());
  }

  /**
   * SHA-256 of each sub-state made of the keys that share their first {@code depth} segments, in
   * order of first appearance. With depth 1 on world states this gives one digest per chunk.
   */
  public Map<String, String> digestsAt(int depth) {
    if (depth < 1) {
      throw new IllegalArgumentException("depth must be at least 1: " + depth);
    }
    Map<String, List<Entry>> groups = new LinkedHashMap<>();
    for (Entry entry : entries) {
      groups
          .computeIfAbsent(StateKey.prefix(entry.key(), depth), k -> new ArrayList<>())
          .add(entry);
    }
    Map<String, String> digests = new LinkedHashMap<>();
    groups.forEach((prefix, group) -> digests.put(prefix, digest(group)));
    return Collections.unmodifiableMap(digests);
  }

  /** Length-prefixed UTF-8 of every key and value, so that no two states share an encoding. */
  private static String digest(List<Entry> entries) {
    MessageDigest sha256;
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required to be provided by the JDK", e);
    }
    for (Entry entry : entries) {
      update(sha256, entry.key());
      update(sha256, entry.value());
    }
    return HexFormat.of().formatHex(sha256.digest());
  }

  private static void update(MessageDigest digest, String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }

  /** Collects entries in emission order and refuses duplicate or malformed keys. */
  public static final class Builder {

    private final List<Entry> entries = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();

    private Builder() {}

    public Builder add(String key, String value) {
      StateKey.checkKey(key);
      if (!keys.add(key)) {
        throw new IllegalArgumentException("duplicate key in canonical state: " + key);
      }
      entries.add(new Entry(key, value));
      return this;
    }

    public CanonicalState build() {
      return new CanonicalState(new ArrayList<>(entries));
    }
  }
}
