package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.port.TypeSpecific;
import java.util.Map;
import java.util.Optional;

/** Evaluates one key of an entity predicate (LT-R57a). */
final class EntityChecks {

  private EntityChecks() {}

  static boolean holds(EntityPredicate.Check check, EntityView entity, LootContext context) {
    RegistryAccess access = context.data().registries();
    return switch (check) {
      case EntityPredicate.Check.EntityType type -> type.types().contains(entity.type(), access);
      case EntityPredicate.Check.Flags flags -> flagsHold(flags.flags(), entity);
      case EntityPredicate.Check.Components components ->
          componentsHold(components.expected(), entity);
      case EntityPredicate.Check.Equipment equipment ->
          equipmentHolds(equipment.slots(), entity, access);
      case EntityPredicate.Check.Vehicle vehicle ->
          entity.vehicle().map(mount -> vehicle.vehicle().matches(mount, context)).orElse(false);
      case EntityPredicate.Check.MovementAffectedBy affected ->
          blockUnderHolds(affected.block(), entity, context);
      case EntityPredicate.Check.Movement movement ->
          movement.horizontalSpeed().contains(entity.motion().horizontalSpeed())
              && movement.fallDistance().contains(entity.motion().fallDistance());
      case EntityPredicate.Check.PeriodicTick tick ->
          tick.period() > 0 && entity.tickCount() % tick.period() == 0;
      case EntityPredicate.Check.Location location ->
          location.location().matches(context, blockOf(entity.position()));
      case EntityPredicate.Check.Sheep sheep ->
          entity.typeSpecific().orElse(null) instanceof TypeSpecific.Sheep state
              && tristate(sheep.sheared(), state.sheared());
      case EntityPredicate.Check.CubeMob cube ->
          entity.typeSpecific().orElse(null) instanceof TypeSpecific.CubeMob state
              && cube.size().contains(state.size());
      case EntityPredicate.Check.Raider raider ->
          entity.typeSpecific().orElse(null) instanceof TypeSpecific.Raider state
              && tristate(raider.captain(), state.captain());
      case EntityPredicate.Check.FishingHook hook ->
          entity.typeSpecific().orElse(null) instanceof TypeSpecific.FishingHook state
              && tristate(hook.inOpenWater(), state.inOpenWater());
      case EntityPredicate.Check.Player player ->
          entity.typeSpecific().orElse(null) instanceof TypeSpecific.Player;
      case EntityPredicate.Check.Other other -> false;
    };
  }

  private static boolean tristate(Tristate expected, boolean actual) {
    return switch (expected) {
      case UNSET -> true;
      case TRUE -> actual;
      case FALSE -> !actual;
    };
  }

  private static boolean flagsHold(Map<EntityFlag, Boolean> flags, EntityView entity) {
    for (Map.Entry<EntityFlag, Boolean> flag : flags.entrySet()) {
      if (entity.hasFlag(flag.getKey()) != flag.getValue()) {
        return false;
      }
    }
    return true;
  }

  private static boolean equipmentHolds(
      Map<EquipmentSlot, ItemMatch> slots, EntityView entity, RegistryAccess access) {
    for (Map.Entry<EquipmentSlot, ItemMatch> slot : slots.entrySet()) {
      if (!slot.getValue().matches(entity.equipment(slot.getKey()), access)) {
        return false;
      }
    }
    return true;
  }

  private static boolean componentsHold(Map<Identifier, JsonNode> expected, EntityView entity) {
    for (Map.Entry<Identifier, JsonNode> component : expected.entrySet()) {
      Optional<ComponentValue> value = entity.component(component.getKey());
      if (value.isEmpty() || !same(component.getValue(), value.get().tag())) {
        return false;
      }
    }
    return true;
  }

  private static boolean blockUnderHolds(BlockMatch block, EntityView entity, LootContext context) {
    Vec3 at = entity.position();
    BlockPos below = blockOf(new Vec3(at.x(), at.y() - 0.5, at.z()));
    return context
        .chunkAt(below)
        .filter(chunk -> chunk.height().contains(below.y()))
        .map(chunk -> block.matches(chunk.blockState(below), context.data().registries()))
        .orElse(false);
  }

  private static BlockPos blockOf(Vec3 position) {
    return new BlockPos(
        (int) Math.floor(position.x()),
        (int) Math.floor(position.y()),
        (int) Math.floor(position.z()));
  }

  /** Compares an expected JSON value with an NBT value read from the entity. */
  static boolean same(JsonNode expected, NbtTag actual) {
    return switch (expected) {
      case JsonString text -> actual instanceof StringTag tag && tag.value().equals(text.value());
      case JsonNumber number ->
          actual instanceof NumericTag tag && tag.doubleValue() == number.doubleValue();
      case JsonBoolean bool ->
          actual instanceof NumericTag tag && (tag.doubleValue() != 0) == bool.value();
      case JsonArray array -> listSame(array, actual);
      case JsonObject object -> compoundSame(object, actual);
      default -> false;
    };
  }

  private static boolean listSame(JsonArray expected, NbtTag actual) {
    if (!(actual instanceof ListTag list) || list.elements().size() != expected.elements().size()) {
      return false;
    }
    for (int i = 0; i < expected.elements().size(); i++) {
      if (!same(expected.elements().get(i), list.elements().get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean compoundSame(JsonObject expected, NbtTag actual) {
    if (!(actual instanceof CompoundTag compound)) {
      return false;
    }
    for (Map.Entry<String, JsonNode> member : expected.members().entrySet()) {
      Optional<NbtTag> value = compound.get(member.getKey());
      if (value.isEmpty() || !same(member.getValue(), value.get())) {
        return false;
      }
    }
    return true;
  }
}
