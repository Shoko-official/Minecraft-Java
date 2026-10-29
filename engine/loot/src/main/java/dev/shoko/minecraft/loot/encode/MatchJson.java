package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.predicate.match.NumberRange;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;

/** Encodes the matchers (ranges, sets, block, item, entity, location, damage source). */
final class MatchJson {

  private MatchJson() {}

  static Object range(NumberRange range) {
    if (range.min() == range.max()) {
      return JsonOut.number(range.min());
    }
    SequencedMap<String, Object> object = JsonOut.object();
    if (!Double.isInfinite(range.min())) {
      object.put("min", JsonOut.number(range.min()));
    }
    if (!Double.isInfinite(range.max())) {
      object.put("max", JsonOut.number(range.max()));
    }
    return object;
  }

  /** The value of a set; the caller omits the key of an unrestricted set. */
  static Object set(IdOrTagSet set) {
    List<Object> entries = new ArrayList<>();
    set.ids().forEach(id -> entries.add(id.toString()));
    set.tags().forEach(tag -> entries.add("#" + tag));
    return entries.size() == 1 ? entries.getFirst() : entries;
  }

  static SequencedMap<String, Object> block(BlockMatch match) {
    SequencedMap<String, Object> object = JsonOut.object();
    if (!match.blocks().unrestricted()) {
      object.put("blocks", set(match.blocks()));
    }
    if (!match.state().isEmpty()) {
      SequencedMap<String, Object> state = JsonOut.object();
      state.putAll(match.state());
      object.put("state", state);
    }
    return object;
  }

  static Object item(ItemMatch match) {
    SequencedMap<String, Object> object = JsonOut.object();
    if (!match.items().unrestricted()) {
      object.put("items", set(match.items()));
    }
    if (match.count().min() != Double.NEGATIVE_INFINITY
        || match.count().max() != Double.POSITIVE_INFINITY) {
      object.put("count", range(match.count()));
    }
    if (!match.componentPredicates().isEmpty()) {
      object.put("predicates", nodes(match.componentPredicates()));
    }
    if (!match.exactComponents().isEmpty()) {
      object.put("components", nodes(match.exactComponents()));
    }
    return object;
  }

  private static Object nodes(Map<Identifier, JsonNode> map) {
    SequencedMap<String, Object> object = JsonOut.object();
    map.forEach((id, node) -> object.put(id.toString(), JsonOut.tree(node)));
    return object;
  }

  static Object location(LocationPredicate predicate) {
    SequencedMap<String, Object> object = JsonOut.object();
    for (LocationPredicate.Requirement requirement : predicate.requirements()) {
      switch (requirement) {
        case LocationPredicate.Requirement.Block block -> object.put("block", block(block.block()));
        case LocationPredicate.Requirement.Biomes biomes ->
            object.put("biomes", set(biomes.biomes()));
        case LocationPredicate.Requirement.CanSeeSky sky ->
            object.put("can_see_sky", sky.expected());
      }
    }
    return object;
  }

  static Object damageSource(DamageSourcePredicate predicate) {
    SequencedMap<String, Object> object = JsonOut.object();
    for (DamageSourcePredicate.Requirement requirement : predicate.requirements()) {
      switch (requirement) {
        case DamageSourcePredicate.Requirement.Tags tags -> object.put("tags", tags(tags));
        case DamageSourcePredicate.Requirement.SourceEntity entity ->
            object.put("source_entity", entity(entity.entity()));
        case DamageSourcePredicate.Requirement.DirectEntity entity ->
            object.put("direct_entity", entity(entity.entity()));
        case DamageSourcePredicate.Requirement.IsDirect direct ->
            object.put("is_direct", direct.expected());
      }
    }
    return object;
  }

  private static Object tags(DamageSourcePredicate.Requirement.Tags tags) {
    List<Object> list = new ArrayList<>();
    for (DamageSourcePredicate.TagExpectation tag : tags.tags()) {
      SequencedMap<String, Object> entry = JsonOut.object();
      entry.put("id", "#" + tag.tag());
      entry.put("expected", tag.expected());
      list.add(entry);
    }
    return list;
  }

  static Object entity(EntityPredicate predicate) {
    SequencedMap<String, Object> object = JsonOut.object();
    for (EntityPredicate.Check check : predicate.checks()) {
      switch (check) {
        case EntityPredicate.Check.EntityType type ->
            object.put("minecraft:entity_type", set(type.types()));
        case EntityPredicate.Check.Flags flags -> object.put("minecraft:flags", flags(flags));
        case EntityPredicate.Check.Components components ->
            object.put("minecraft:components", nodes(components.expected()));
        case EntityPredicate.Check.Equipment equipment ->
            object.put("minecraft:equipment", equipment(equipment));
        case EntityPredicate.Check.Vehicle vehicle ->
            object.put("minecraft:vehicle", entity(vehicle.vehicle()));
        case EntityPredicate.Check.MovementAffectedBy affected -> {
          SequencedMap<String, Object> wrapper = JsonOut.object();
          wrapper.put("block", block(affected.block()));
          object.put("minecraft:movement_affected_by", wrapper);
        }
        case EntityPredicate.Check.Movement movement ->
            object.put("minecraft:movement", movement(movement));
        case EntityPredicate.Check.PeriodicTick tick ->
            object.put("minecraft:periodic_tick", JsonOut.number(tick.period()));
        case EntityPredicate.Check.Location location ->
            object.put("minecraft:location", location(location.location()));
        case EntityPredicate.Check.Sheep sheep ->
            object.put("minecraft:type_specific/sheep", tristate("sheared", sheep.sheared()));
        case EntityPredicate.Check.CubeMob cube -> {
          SequencedMap<String, Object> size = JsonOut.object();
          size.put("size", range(cube.size()));
          object.put("minecraft:type_specific/cube_mob", size);
        }
        case EntityPredicate.Check.Raider raider ->
            object.put("minecraft:type_specific/raider", tristate("is_captain", raider.captain()));
        case EntityPredicate.Check.FishingHook hook ->
            object.put(
                "minecraft:type_specific/fishing_hook",
                tristate("in_open_water", hook.inOpenWater()));
        case EntityPredicate.Check.Player player ->
            object.put("minecraft:type_specific/player", JsonOut.object());
        case EntityPredicate.Check.Other other ->
            object.put(other.key().toString(), JsonOut.tree(other.value()));
      }
    }
    return object;
  }

  private static Object flags(EntityPredicate.Check.Flags flags) {
    SequencedMap<String, Object> object = JsonOut.object();
    for (Map.Entry<EntityFlag, Boolean> flag : flags.flags().entrySet()) {
      object.put(flag.getKey().jsonName(), flag.getValue());
    }
    return object;
  }

  private static Object equipment(EntityPredicate.Check.Equipment equipment) {
    SequencedMap<String, Object> object = JsonOut.object();
    equipment
        .slots()
        .forEach((slot, item) -> object.put(slot.name().toLowerCase(Locale.ROOT), item(item)));
    return object;
  }

  private static Object movement(EntityPredicate.Check.Movement movement) {
    SequencedMap<String, Object> object = JsonOut.object();
    if (!isAny(movement.horizontalSpeed())) {
      object.put("horizontal_speed", range(movement.horizontalSpeed()));
    }
    if (!isAny(movement.fallDistance())) {
      object.put("fall_distance", range(movement.fallDistance()));
    }
    return object;
  }

  private static boolean isAny(NumberRange range) {
    return range.min() == Double.NEGATIVE_INFINITY && range.max() == Double.POSITIVE_INFINITY;
  }

  private static Object tristate(String key, Tristate state) {
    SequencedMap<String, Object> object = JsonOut.object();
    if (state != Tristate.UNSET) {
      object.put(key, state == Tristate.TRUE);
    }
    return object;
  }
}
