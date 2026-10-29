package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/** Reads entity predicates (LT-R57a): an object whose keys are entity sub-predicate types. */
final class EntityReader {

  /** Registered keys that no data of the base uses: kept as read, they match nothing. */
  private static final Set<String> OTHER_KEYS =
      Set.of(
          "stepping_on",
          "distance",
          "effects",
          "nbt",
          "passenger",
          "targeted_entity",
          "team",
          "slots",
          "predicates",
          "entity_tags",
          "type_specific/lightning");

  private EntityReader() {}

  static LootDecoding<EntityPredicate> entity(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    EntityPredicate predicate = entity(node, access, findings);
    return findings.result(predicate);
  }

  static EntityPredicate entity(JsonNode node, RegistryAccess access, Findings findings) {
    List<EntityPredicate.Check> checks = new ArrayList<>();
    if (!(node instanceof JsonObject object)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an object");
      return new EntityPredicate(checks);
    }
    for (Map.Entry<String, JsonNode> member : object.members().entrySet()) {
      EntityPredicate.Check check = check(member.getKey(), member.getValue(), access, findings);
      if (check != null) {
        checks.add(check);
      }
    }
    return new EntityPredicate(checks);
  }

  private static EntityPredicate.Check check(
      String key, JsonNode value, RegistryAccess access, Findings findings) {
    Identifier id = Identifier.tryParse(key).orElse(null);
    if (id == null || !id.namespace().equals("minecraft")) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, value, "unknown entity predicate key " + key);
      return null;
    }
    String name = id.path();
    if (OTHER_KEYS.contains(name)) {
      return new EntityPredicate.Check.Other(id, value);
    }
    return switch (name) {
      case "entity_type" ->
          new EntityPredicate.Check.EntityType(
              MatchReaders.idOrTagSet(value, MatchReaders.ENTITY_TYPE, access, findings));
      case "flags" -> flags(value, findings);
      case "components" -> components(value, findings);
      case "equipment" -> equipment(value, access, findings);
      case "vehicle" -> new EntityPredicate.Check.Vehicle(entity(value, access, findings));
      case "movement_affected_by" -> movementAffectedBy(value, access, findings);
      case "movement" -> movement(value, findings);
      case "periodic_tick" ->
          new EntityPredicate.Check.PeriodicTick(
              Fields.intOf(value, findings, 1, Integer.MAX_VALUE));
      case "location" ->
          new EntityPredicate.Check.Location(MatchReaders.location(value, access, findings));
      default -> typeSpecific(name, value, findings);
    };
  }

  private static EntityPredicate.Check typeSpecific(
      String name, JsonNode value, Findings findings) {
    return switch (name) {
      case "type_specific/sheep" -> {
        Fields fields = Fields.of(value, findings);
        EntityPredicate.Check check =
            new EntityPredicate.Check.Sheep(tristate(fields, "sheared", findings));
        fields.finish();
        yield check;
      }
      case "type_specific/raider" -> {
        Fields fields = Fields.of(value, findings);
        EntityPredicate.Check check =
            new EntityPredicate.Check.Raider(tristate(fields, "is_captain", findings));
        fields.finish();
        yield check;
      }
      case "type_specific/fishing_hook" -> {
        Fields fields = Fields.of(value, findings);
        EntityPredicate.Check check =
            new EntityPredicate.Check.FishingHook(tristate(fields, "in_open_water", findings));
        fields.finish();
        yield check;
      }
      case "type_specific/cube_mob" -> {
        Fields fields = Fields.of(value, findings);
        JsonNode size = fields.optional("size");
        EntityPredicate.Check check =
            new EntityPredicate.Check.CubeMob(
                size == null ? MatchReaders.anyNumber() : MatchReaders.range(size, findings));
        fields.finish();
        yield check;
      }
      case "type_specific/player" -> new EntityPredicate.Check.Player();
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, value, "unknown entity predicate key " + name);
        yield null;
      }
    };
  }

  private static Tristate tristate(Fields fields, String key, Findings findings) {
    JsonNode node = fields.optional(key);
    if (node == null) {
      return Tristate.UNSET;
    }
    return Fields.boolOf(node, findings) ? Tristate.TRUE : Tristate.FALSE;
  }

  private static EntityPredicate.Check flags(JsonNode value, Findings findings) {
    SequencedMap<EntityFlag, Boolean> flags = new LinkedHashMap<>();
    Fields fields = Fields.of(value, findings);
    if (fields.valid()) {
      for (Map.Entry<String, JsonNode> member : fields.node().members().entrySet()) {
        EntityFlag flag = flag(member.getKey());
        if (flag == null) {
          findings.add(
              LootFindingCode.UNKNOWN_FIELD, member.getValue(), "unknown flag " + member.getKey());
        } else {
          flags.put(flag, Fields.boolOf(member.getValue(), findings));
        }
      }
    }
    return new EntityPredicate.Check.Flags(flags);
  }

  private static EntityFlag flag(String name) {
    return switch (name) {
      case "is_on_fire" -> EntityFlag.ON_FIRE;
      case "is_on_ground" -> EntityFlag.ON_GROUND;
      case "is_flying" -> EntityFlag.FLYING;
      default -> null;
    };
  }

  private static EntityPredicate.Check components(JsonNode value, Findings findings) {
    SequencedMap<Identifier, JsonNode> expected = new LinkedHashMap<>();
    Fields fields = Fields.of(value, findings);
    if (fields.valid()) {
      for (Map.Entry<String, JsonNode> member : fields.node().members().entrySet()) {
        Identifier id = Identifier.tryParse(member.getKey()).orElse(null);
        if (id == null) {
          findings.add(LootFindingCode.INVALID_IDENTIFIER, member.getValue(), "invalid identifier");
        } else {
          expected.put(id, member.getValue());
        }
      }
    }
    return new EntityPredicate.Check.Components(expected);
  }

  private static EntityPredicate.Check equipment(
      JsonNode value, RegistryAccess access, Findings findings) {
    SequencedMap<EquipmentSlot, ItemMatch> slots = new LinkedHashMap<>();
    Fields fields = Fields.of(value, findings);
    if (fields.valid()) {
      for (Map.Entry<String, JsonNode> member : fields.node().members().entrySet()) {
        EquipmentSlot slot = slot(member.getKey());
        if (slot == null) {
          findings.add(
              LootFindingCode.UNKNOWN_FIELD, member.getValue(), "unknown slot " + member.getKey());
        } else {
          slots.put(slot, MatchReaders.item(member.getValue(), access, findings));
        }
      }
    }
    return new EntityPredicate.Check.Equipment(slots);
  }

  /** The names of the slots as written in the data: lower case, {@code mainhand}, {@code feet}. */
  static EquipmentSlot slot(String name) {
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      if (slot.name().toLowerCase(Locale.ROOT).equals(name)) {
        return slot;
      }
    }
    return null;
  }

  private static EntityPredicate.Check movementAffectedBy(
      JsonNode value, RegistryAccess access, Findings findings) {
    Fields fields = Fields.of(value, findings);
    JsonNode block = fields.optional("block");
    BlockMatch match =
        block == null
            ? new BlockMatch(
                MatchReaders.unrestricted(Identifier.minecraft("block")), new LinkedHashMap<>())
            : MatchReaders.block(block, access, findings);
    fields.finish();
    return new EntityPredicate.Check.MovementAffectedBy(match);
  }

  private static EntityPredicate.Check movement(JsonNode value, Findings findings) {
    Fields fields = Fields.of(value, findings);
    JsonNode speed = fields.optional("horizontal_speed");
    JsonNode fall = fields.optional("fall_distance");
    EntityPredicate.Check check =
        new EntityPredicate.Check.Movement(
            speed == null ? MatchReaders.anyNumber() : MatchReaders.range(speed, findings),
            fall == null ? MatchReaders.anyNumber() : MatchReaders.range(fall, findings));
    fields.finish();
    return check;
  }
}
