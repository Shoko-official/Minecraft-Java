package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/** Readers of the matchers that are not entity predicates (LT-R34, LT-R58 to LT-R60). */
final class MatchReaders {

  private static final Identifier BLOCK = Identifier.minecraft("block");
  private static final Identifier ITEM = Identifier.minecraft("item");
  static final Identifier ENTITY_TYPE = Identifier.minecraft("entity_type");
  static final Identifier BIOME = Identifier.minecraft("biome");

  private MatchReaders() {}

  static IdOrTagSet unrestricted(Identifier registry) {
    return new IdOrTagSet(registry, List.of(), List.of(), true);
  }

  static NumberRange anyNumber() {
    return new NumberRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  static LootDecoding<NumberRange> range(JsonNode node) {
    Findings findings = new Findings();
    NumberRange range = range(node, findings);
    return findings.result(range);
  }

  static NumberRange range(JsonNode node, Findings findings) {
    if (node instanceof JsonNumber number) {
      double value = number.doubleValue();
      return new NumberRange(value, value);
    }
    if (!(node instanceof JsonObject)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number or an object");
      return anyNumber();
    }
    Fields fields = Fields.of(node, findings);
    double min = bound(fields, "min", Double.NEGATIVE_INFINITY, findings);
    double max = bound(fields, "max", Double.POSITIVE_INFINITY, findings);
    fields.finish();
    return new NumberRange(min, max);
  }

  private static double bound(Fields fields, String key, double fallback, Findings findings) {
    JsonNode node = fields.optional(key);
    if (node == null) {
      return fallback;
    }
    if (node instanceof JsonNumber number) {
      return number.doubleValue();
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number");
    return fallback;
  }

  static LootDecoding<IdOrTagSet> idOrTagSet(
      JsonNode node, Identifier registry, RegistryAccess access) {
    Findings findings = new Findings();
    IdOrTagSet set = idOrTagSet(node, registry, access, findings);
    return findings.result(set);
  }

  static IdOrTagSet idOrTagSet(
      JsonNode node, Identifier registry, RegistryAccess access, Findings findings) {
    List<Identifier> ids = new ArrayList<>();
    List<Identifier> tags = new ArrayList<>();
    if (node instanceof JsonArray array) {
      for (JsonNode element : array.elements()) {
        entry(element, registry, access, findings, ids, tags);
      }
    } else {
      entry(node, registry, access, findings, ids, tags);
    }
    return new IdOrTagSet(registry, List.copyOf(ids), List.copyOf(tags), false);
  }

  private static void entry(
      JsonNode node,
      Identifier registry,
      RegistryAccess access,
      Findings findings,
      List<Identifier> ids,
      List<Identifier> tags) {
    if (!(node instanceof JsonString string)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an identifier or a tag");
      return;
    }
    String text = string.value();
    boolean isTag = text.startsWith("#");
    Identifier id = Identifier.tryParse(isTag ? text.substring(1) : text).orElse(null);
    if (id == null) {
      findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "invalid identifier");
    } else if (!isTag) {
      ids.add(id);
    } else if (access.tagMembers(new TagKey<Object>(new RegistryKey<>(registry), id)).isEmpty()) {
      findings.add(LootFindingCode.UNRESOLVED_TAG, node, "tag is not loaded: " + id);
    } else {
      tags.add(id);
    }
  }

  static LootDecoding<BlockMatch> block(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    BlockMatch match = block(node, access, findings);
    return findings.result(match);
  }

  static BlockMatch block(JsonNode node, RegistryAccess access, Findings findings) {
    Fields fields = Fields.of(node, findings);
    JsonNode blocksNode = fields.optional("blocks");
    IdOrTagSet blocks =
        blocksNode == null ? unrestricted(BLOCK) : idOrTagSet(blocksNode, BLOCK, access, findings);
    SequencedMap<String, String> state = new LinkedHashMap<>();
    JsonObject stateNode = fields.optionalObject("state");
    if (stateNode != null) {
      for (Map.Entry<String, JsonNode> member : stateNode.members().entrySet()) {
        state.put(member.getKey(), Fields.stringOf(member.getValue(), findings));
      }
    }
    fields.finish();
    return new BlockMatch(blocks, state);
  }

  static LootDecoding<ItemMatch> item(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    ItemMatch match = item(node, access, findings);
    return findings.result(match);
  }

  static ItemMatch item(JsonNode node, RegistryAccess access, Findings findings) {
    Fields fields = Fields.of(node, findings);
    JsonNode itemsNode = fields.optional("items");
    IdOrTagSet items =
        itemsNode == null ? unrestricted(ITEM) : idOrTagSet(itemsNode, ITEM, access, findings);
    JsonNode countNode = fields.optional("count");
    NumberRange count = countNode == null ? anyNumber() : range(countNode, findings);
    SequencedMap<Identifier, JsonNode> predicates =
        identifiedMembers(fields.optionalObject("predicates"), findings);
    SequencedMap<Identifier, JsonNode> components =
        identifiedMembers(fields.optionalObject("components"), findings);
    fields.finish();
    return new ItemMatch(items, count, predicates, components);
  }

  private static SequencedMap<Identifier, JsonNode> identifiedMembers(
      JsonObject object, Findings findings) {
    SequencedMap<Identifier, JsonNode> map = new LinkedHashMap<>();
    if (object == null) {
      return map;
    }
    for (Map.Entry<String, JsonNode> member : object.members().entrySet()) {
      Identifier id = Identifier.tryParse(member.getKey()).orElse(null);
      if (id == null) {
        findings.add(LootFindingCode.INVALID_IDENTIFIER, member.getValue(), "invalid identifier");
      } else {
        map.put(id, member.getValue());
      }
    }
    return map;
  }

  static LootDecoding<LocationPredicate> location(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    LocationPredicate predicate = location(node, access, findings);
    return findings.result(predicate);
  }

  static LocationPredicate location(JsonNode node, RegistryAccess access, Findings findings) {
    List<LocationPredicate.Requirement> requirements = new ArrayList<>();
    if (!(node instanceof JsonObject object)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an object");
      return new LocationPredicate(requirements);
    }
    for (Map.Entry<String, JsonNode> member : object.members().entrySet()) {
      JsonNode value = member.getValue();
      switch (member.getKey()) {
        case "block" ->
            requirements.add(
                new LocationPredicate.Requirement.Block(block(value, access, findings)));
        case "biomes" ->
            requirements.add(
                new LocationPredicate.Requirement.Biomes(
                    idOrTagSet(value, BIOME, access, findings)));
        case "can_see_sky" ->
            requirements.add(
                new LocationPredicate.Requirement.CanSeeSky(Fields.boolOf(value, findings)));
        default ->
            findings.add(LootFindingCode.UNKNOWN_FIELD, value, "unknown field " + member.getKey());
      }
    }
    return new LocationPredicate(requirements);
  }

  static LootDecoding<DamageSourcePredicate> damageSource(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    List<DamageSourcePredicate.Requirement> requirements = new ArrayList<>();
    if (!(node instanceof JsonObject object)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an object");
      return findings.failure();
    }
    for (Map.Entry<String, JsonNode> member : object.members().entrySet()) {
      JsonNode value = member.getValue();
      switch (member.getKey()) {
        case "tags" -> requirements.add(tags(value, findings));
        case "source_entity" ->
            requirements.add(
                new DamageSourcePredicate.Requirement.SourceEntity(
                    EntityReader.entity(value, access, findings)));
        case "direct_entity" ->
            requirements.add(
                new DamageSourcePredicate.Requirement.DirectEntity(
                    EntityReader.entity(value, access, findings)));
        case "is_direct" ->
            requirements.add(
                new DamageSourcePredicate.Requirement.IsDirect(Fields.boolOf(value, findings)));
        default ->
            findings.add(LootFindingCode.UNKNOWN_FIELD, value, "unknown field " + member.getKey());
      }
    }
    return findings.result(new DamageSourcePredicate(requirements));
  }

  private static DamageSourcePredicate.Requirement tags(JsonNode node, Findings findings) {
    List<DamageSourcePredicate.TagExpectation> tags = new ArrayList<>();
    JsonArray array = Fields.arrayOf(node, findings);
    if (array != null) {
      for (JsonNode element : array.elements()) {
        Fields fields = Fields.of(element, findings);
        JsonNode idNode = fields.required("id");
        Identifier tag = idNode == null ? null : tagId(idNode, findings);
        JsonNode expectedNode = fields.required("expected");
        boolean expected = expectedNode != null && Fields.boolOf(expectedNode, findings);
        fields.finish();
        tags.add(new DamageSourcePredicate.TagExpectation(tag, expected));
      }
    }
    return new DamageSourcePredicate.Requirement.Tags(tags);
  }

  private static Identifier tagId(JsonNode node, Findings findings) {
    if (node instanceof JsonString string && string.value().startsWith("#")) {
      return Identifier.tryParse(string.value().substring(1))
          .orElseGet(() -> invalid(node, findings));
    }
    return Fields.idOf(node, findings);
  }

  private static Identifier invalid(JsonNode node, Findings findings) {
    findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "invalid identifier");
    return null;
  }
}
