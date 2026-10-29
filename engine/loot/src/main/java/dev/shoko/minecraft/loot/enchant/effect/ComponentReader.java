package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;

/** Reads the value of one effect component (EN-R12). */
final class ComponentReader {

  /** Registries {@code enchantment_entity_effect_type} and location type, plus value types. */
  private static final Set<String> ENTITY_EFFECT_TYPES =
      Set.of(
          "all_of",
          "apply_exhaustion",
          "apply_impulse",
          "apply_mob_effect",
          "change_item_damage",
          "damage_entity",
          "explode",
          "ignite",
          "play_sound",
          "replace_block",
          "replace_disk",
          "run_function",
          "set_block_properties",
          "spawn_particles",
          "summon_entity",
          "attribute",
          "add",
          "set",
          "multiply",
          "remove_binomial",
          "exponential");

  private final RegistryAccess access;
  private final Findings findings;

  ComponentReader(RegistryAccess access, Findings findings) {
    this.access = access;
    this.findings = findings;
  }

  EffectComponentValue read(Identifier component, JsonNode value) {
    return switch (ComponentForms.form(component.path())) {
      case VALUE_LIST -> new EffectComponentValue.ValueEffects(valueList(value));
      case DIRECT_VALUE ->
          new EffectComponentValue.DirectValue(EffectCodec.readValueEffect(value, findings));
      case ENTITY_LIST -> new EffectComponentValue.EntityEffects(entityList(value));
      case FLAG -> flag(value);
      case SOUNDS -> new EffectComponentValue.Sounds(sounds(value));
      case ATTRIBUTES -> new EffectComponentValue.Attributes(attributes(value));
      case RAW -> new EffectComponentValue.Raw(value);
    };
  }

  private EffectComponentValue flag(JsonNode value) {
    Fields fields = Fields.of(value, findings);
    fields.finish();
    return new EffectComponentValue.Flag();
  }

  private List<ConditionalValueEffect> valueList(JsonNode value) {
    List<ConditionalValueEffect> entries = new ArrayList<>();
    JsonArray array = Fields.arrayOf(value, findings);
    if (array == null) {
      return entries;
    }
    for (JsonNode element : array.elements()) {
      Fields fields = Fields.of(element, findings);
      JsonNode effect = fields.required("effect");
      ValueEffect decoded = effect == null ? null : EffectCodec.readValueEffect(effect, findings);
      List<LootCondition> requirements = requirements(fields);
      fields.finish();
      entries.add(new ConditionalValueEffect(decoded, requirements));
    }
    return entries;
  }

  private List<ConditionalEntityEffect> entityList(JsonNode value) {
    List<ConditionalEntityEffect> entries = new ArrayList<>();
    JsonArray array = Fields.arrayOf(value, findings);
    if (array == null) {
      return entries;
    }
    for (JsonNode element : array.elements()) {
      Fields fields = Fields.of(element, findings);
      JsonNode effect = fields.required("effect");
      EntityEffect decoded = effect == null ? null : entityEffect(effect);
      List<LootCondition> requirements = requirements(fields);
      EnchantedTarget enchanted = enchanted(fields);
      AffectedTarget affected = affected(fields);
      fields.finish();
      entries.add(new ConditionalEntityEffect(decoded, requirements, enchanted, affected));
    }
    return entries;
  }

  private EntityEffect entityEffect(JsonNode node) {
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    if (type == null) {
      return null;
    }
    if (!type.namespace().equals("minecraft") || !ENTITY_EFFECT_TYPES.contains(type.path())) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown effect type " + type);
      return null;
    }
    if (type.path().equals("all_of")) {
      List<EntityEffect> effects = new ArrayList<>();
      JsonArray array = fields.requiredArray("effects");
      if (array != null) {
        for (JsonNode element : array.elements()) {
          effects.add(entityEffect(element));
        }
      }
      fields.finish();
      return new EntityEffect.AllOf(effects);
    }
    JsonObject object = fields.node();
    object.members().keySet().forEach(fields::optional);
    SequencedMap<String, JsonNode> parameters = new LinkedHashMap<>(object.members());
    parameters.remove("type");
    return new EntityEffect.Typed(type, new JsonObject(object.origin(), object.path(), parameters));
  }

  private List<LootCondition> requirements(Fields fields) {
    JsonNode node = fields.optional("requirements");
    if (node == null) {
      return List.of();
    }
    List<LootCondition> list = findings.take(ConditionCodec.decodeList(node, access));
    return list == null ? List.of() : list;
  }

  private EnchantedTarget enchanted(Fields fields) {
    String name = fields.optionalString("enchanted");
    if (name == null) {
      return EnchantedTarget.UNSET;
    }
    return switch (name) {
      case "attacker" -> EnchantedTarget.ATTACKER;
      case "victim" -> EnchantedTarget.VICTIM;
      case "this" -> EnchantedTarget.THIS;
      default -> {
        findings.add(LootFindingCode.VALUE_RANGE, fields.node(), "unknown enchanted " + name);
        yield EnchantedTarget.UNSET;
      }
    };
  }

  private AffectedTarget affected(Fields fields) {
    String name = fields.optionalString("affected");
    if (name == null) {
      return AffectedTarget.UNSET;
    }
    return switch (name) {
      case "attacker" -> AffectedTarget.ATTACKER;
      case "victim" -> AffectedTarget.VICTIM;
      case "damaging_entity" -> AffectedTarget.DAMAGING_ENTITY;
      default -> {
        findings.add(LootFindingCode.VALUE_RANGE, fields.node(), "unknown affected " + name);
        yield AffectedTarget.UNSET;
      }
    };
  }

  private List<Identifier> sounds(JsonNode value) {
    List<Identifier> sounds = new ArrayList<>();
    JsonArray array = Fields.arrayOf(value, findings);
    if (array != null) {
      for (JsonNode element : array.elements()) {
        sounds.add(Fields.idOf(element, findings));
      }
    }
    return sounds;
  }

  private List<AttributeEffect> attributes(JsonNode value) {
    List<AttributeEffect> attributes = new ArrayList<>();
    JsonArray array = Fields.arrayOf(value, findings);
    if (array == null) {
      return attributes;
    }
    for (JsonNode element : array.elements()) {
      Fields fields = Fields.of(element, findings);
      Identifier id = fields.requiredId("id");
      Identifier attribute = fields.requiredId("attribute");
      String operation = fields.requiredString("operation");
      JsonNode amountNode = fields.required("amount");
      LevelBasedValue amount =
          amountNode == null ? null : EffectCodec.readLevelBased(amountNode, findings);
      fields.finish();
      attributes.add(new AttributeEffect(id, attribute, operation, amount));
    }
    return attributes;
  }
}
