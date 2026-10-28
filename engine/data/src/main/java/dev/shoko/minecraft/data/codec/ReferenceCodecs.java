package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The codecs of references to registry entries and tags (R64, K3). */
final class ReferenceCodecs {

  private ReferenceCodecs() {}

  static <T> Codec<Holder<T>> reference(RegistryKey<T> registry) {
    return new FunctionCodec<>(
        (node, access) -> {
          if (!(node instanceof JsonString text)) {
            return Failures.mismatch(node, "an identifier");
          }
          Optional<Identifier> id = Identifier.tryParse(text.value());
          if (id.isEmpty()) {
            return ScalarCodecs.invalidIdentifier(node);
          }
          if (!access.contains(registry, id.get())) {
            return unresolved(node, "unknown entry " + id.get());
          }
          return Outcome.success(Holder.reference(registry, id.get(), access));
        });
  }

  static <T> Codec<HolderSet<T>> set(RegistryKey<T> registry) {
    return new FunctionCodec<>(
        (node, access) -> {
          if (node instanceof JsonString text) {
            return text.value().startsWith("#")
                ? tag(registry, text, access)
                : single(registry, text, access);
          }
          if (node instanceof JsonArray array) {
            return inline(registry, array, access);
          }
          return Failures.mismatch(node, "a string or a list of strings");
        });
  }

  private static <T> Outcome<HolderSet<T>> tag(
      RegistryKey<T> registry, JsonString text, RegistryAccess access) {
    Optional<Identifier> id = Identifier.tryParse(text.value().substring(1));
    if (id.isEmpty()) {
      return ScalarCodecs.invalidIdentifier(text);
    }
    TagKey<T> tag = new TagKey<>(registry, id.get());
    if (access.tagMembers(tag).isEmpty()) {
      return unresolved(text, "unknown tag #" + id.get());
    }
    return Outcome.success(HolderSet.tag(tag, access));
  }

  private static <T> Outcome<HolderSet<T>> single(
      RegistryKey<T> registry, JsonString text, RegistryAccess access) {
    Optional<Identifier> id = Identifier.tryParse(text.value());
    if (id.isEmpty()) {
      return ScalarCodecs.invalidIdentifier(text);
    }
    if (!access.contains(registry, id.get())) {
      return unresolved(text, "unknown entry " + id.get());
    }
    return Outcome.success(HolderSet.list(List.of(Holder.reference(registry, id.get(), access))));
  }

  private static <T> Outcome<HolderSet<T>> inline(
      RegistryKey<T> registry, JsonArray array, RegistryAccess access) {
    Set<Identifier> distinct = new LinkedHashSet<>();
    List<Diagnostic> errors = new ArrayList<>();
    for (JsonNode element : array.elements()) {
      Outcome<Identifier> entry = entry(registry, element, access);
      entry.value().ifPresent(distinct::add);
      errors.addAll(entry.diagnostics());
    }
    if (!errors.isEmpty()) {
      return Outcome.failure(errors);
    }
    List<Holder<T>> holders = new ArrayList<>(distinct.size());
    for (Identifier id : distinct) {
      holders.add(Holder.reference(registry, id, access));
    }
    return Outcome.success(HolderSet.list(holders));
  }

  private static Outcome<Identifier> entry(
      RegistryKey<?> registry, JsonNode element, RegistryAccess access) {
    if (!(element instanceof JsonString text)) {
      return Failures.mismatch(element, "a string");
    }
    Optional<Identifier> id = Identifier.tryParse(text.value());
    if (id.isEmpty()) {
      return ScalarCodecs.invalidIdentifier(element);
    }
    if (!access.contains(registry, id.get())) {
      return unresolved(element, "unknown entry " + id.get());
    }
    return Outcome.success(id.get());
  }

  private static <T> Outcome<T> unresolved(JsonNode node, String message) {
    return Failures.failure(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE, node, message);
  }
}
