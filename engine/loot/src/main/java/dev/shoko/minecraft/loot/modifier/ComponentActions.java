package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.chunk.BlockEntityData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/** The modifiers that write components (LT-R44a to LT-R44j). */
final class ComponentActions {

  private static final Identifier CUSTOM_NAME = Identifier.minecraft("custom_name");
  private static final Identifier ITEM_NAME = Identifier.minecraft("item_name");
  private static final Identifier BLOCK_STATE = Identifier.minecraft("block_state");
  private static final Identifier INSTRUMENT = Identifier.minecraft("instrument");
  private static final Identifier OMINOUS_AMPLIFIER =
      Identifier.minecraft("ominous_bottle_amplifier");
  private static final Identifier STEW_EFFECTS = Identifier.minecraft("suspicious_stew_effects");
  private static final int MAX_LEVEL = 255;
  private static final int MAX_AMPLIFIER = 4;
  private static final int TICKS_PER_SECOND = 20;

  private ComponentActions() {}

  static ItemStack edit(ModifierAction action, ItemStack stack, LootContext context) {
    return switch (action) {
      case ModifierAction.SetComponents set -> setComponents(set, stack);
      case ModifierAction.SetName name -> setName(name, stack, context);
      case ModifierAction.SetPotion potion -> setPotion(potion, stack);
      case ModifierAction.SetDamage damage -> setDamage(damage, stack, context);
      case ModifierAction.SetEnchantments enchantments ->
          setEnchantments(enchantments, stack, context);
      case ModifierAction.SetInstrument instrument -> setInstrument(instrument, stack, context);
      case ModifierAction.SetOminousBottleAmplifier amplifier ->
          stack.withComponent(
              OMINOUS_AMPLIFIER,
              new ComponentValue(
                  new IntTag(Math.clamp(amplifier.amplifier().sample(context), 0, MAX_AMPLIFIER))));
      case ModifierAction.SetStewEffect stew -> setStew(stew, stack, context);
      default -> stack;
    };
  }

  private static ItemStack setComponents(ModifierAction.SetComponents action, ItemStack stack) {
    ItemStack result = stack;
    for (Map.Entry<Identifier, ComponentChange> change : action.patch().changes().entrySet()) {
      result =
          switch (change.getValue()) {
            case ComponentChange.Set set -> result.withComponent(change.getKey(), set.value());
            case ComponentChange.Removed removed -> result.withoutComponent(change.getKey());
          };
    }
    return result;
  }

  private static ItemStack setName(
      ModifierAction.SetName action, ItemStack stack, LootContext context) {
    boolean custom = action.target() == ModifierAction.NameTarget.CUSTOM_NAME;
    Identifier component = custom ? CUSTOM_NAME : ITEM_NAME;
    ComponentCodec codec = ComponentCodec.of(context.data().items().components());
    if (codec.valueFromJson(component, action.name())
        instanceof Decoding.Success<ComponentValue> ok) {
      return stack.withComponent(component, ok.value());
    }
    return stack;
  }

  private static ItemStack setPotion(ModifierAction.SetPotion action, ItemStack stack) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    stack
        .component(WellKnownComponents.POTION_CONTENTS)
        .map(ComponentValue::tag)
        .filter(CompoundTag.class::isInstance)
        .ifPresent(tag -> entries.putAll(((CompoundTag) tag).entries()));
    if (action.potion().equals(Identifier.minecraft("empty"))) {
      entries.remove("potion");
    } else {
      entries.put("potion", new StringTag(action.potion().toString()));
    }
    return stack.withComponent(
        WellKnownComponents.POTION_CONTENTS, new ComponentValue(CompoundTag.of(entries)));
  }

  private static ItemStack setDamage(
      ModifierAction.SetDamage action, ItemStack stack, LootContext context) {
    if (!stack.isDamageable() || stack.maxDamage() <= 0) {
      return stack;
    }
    float fraction = action.damage().sample(context);
    int max = stack.maxDamage();
    if (action.add()) {
      fraction += 1.0f - (float) stack.damage() / max;
    }
    int remaining = (int) Math.floor(Math.clamp(fraction, 0f, 1f) * max);
    return stack.withComponent(
        WellKnownComponents.DAMAGE, new ComponentValue(new IntTag(max - remaining)));
  }

  private static ItemStack setEnchantments(
      ModifierAction.SetEnchantments action, ItemStack stack, LootContext context) {
    Identifier component =
        stack.components().contains(WellKnownComponents.STORED_ENCHANTMENTS)
            ? WellKnownComponents.STORED_ENCHANTMENTS
            : WellKnownComponents.ENCHANTMENTS;
    EnchantmentLevels levels =
        component.equals(WellKnownComponents.ENCHANTMENTS)
            ? ItemEnchantments.of(stack)
            : ItemEnchantments.stored(stack);
    for (Map.Entry<Identifier, dev.shoko.minecraft.loot.number.IntProvider> entry :
        action.enchantments().entrySet()) {
      long level = entry.getValue().sample(context);
      if (action.add()) {
        level += levels.levelOf(entry.getKey());
      }
      levels = levels.with(entry.getKey(), Math.clamp(level, 0, MAX_LEVEL));
    }
    return stack.withComponent(component, levels.toValue());
  }

  private static ItemStack setInstrument(
      ModifierAction.SetInstrument action, ItemStack stack, LootContext context) {
    List<Identifier> members = action.options().members(context.data().registries());
    if (members.isEmpty()) {
      return stack;
    }
    Identifier chosen = members.get(context.random().nextInt(members.size()));
    return stack.withComponent(INSTRUMENT, new ComponentValue(new StringTag(chosen.toString())));
  }

  private static ItemStack setStew(
      ModifierAction.SetStewEffect action, ItemStack stack, LootContext context) {
    List<NbtTag> effects = new ArrayList<>();
    for (ModifierAction.SetStewEffect.StewEffect effect : action.effects()) {
      SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
      entry.put("id", new StringTag(effect.effect().toString()));
      entry.put("duration", new IntTag(effect.duration().sample(context) * TICKS_PER_SECOND));
      effects.add(CompoundTag.of(entry));
    }
    return stack.withComponent(STEW_EFFECTS, new ComponentValue(ListTag.of(effects)));
  }

  static ModifierResult copyState(
      ModifierAction.CopyState action, ItemStack stack, LootContext context) {
    Optional<BlockState> state = context.blockState();
    if (state.isEmpty()) {
      return new ModifierResult(stack, ModifierCause.PARAMETER_ABSENT);
    }
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    BlockState block = state.get();
    if (Identifier.tryParse(block.block().id()).filter(action.block()::equals).isPresent()) {
      for (String property : action.properties()) {
        if (block.block().property(property).isPresent()) {
          entries.put(property, new StringTag(block.value(property)));
        }
      }
    }
    if (entries.isEmpty()) {
      return new ModifierResult(stack, ModifierCause.APPLIED);
    }
    return new ModifierResult(
        stack.withComponent(BLOCK_STATE, new ComponentValue(CompoundTag.of(entries))),
        ModifierCause.APPLIED);
  }

  static ModifierResult copyComponents(
      ModifierAction.CopyComponents action, ItemStack stack, LootContext context) {
    Optional<BlockEntityData> blockEntity = context.blockEntity();
    if (blockEntity.isEmpty()) {
      return new ModifierResult(stack, ModifierCause.PARAMETER_ABSENT);
    }
    CompoundTag data = blockEntity.get().data();
    Optional<CompoundTag> components =
        data.get("components").filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast);
    CompoundTag nested = components.orElse(data);
    ItemStack result = stack;
    for (Identifier include : action.include()) {
      if (action.exclude().contains(include)) {
        continue;
      }
      Optional<NbtTag> value = nested.get(include.toString());
      if (value.isEmpty() && components.isPresent()) {
        value = data.get(include.toString());
      }
      if (value.isPresent()) {
        result = result.withComponent(include, new ComponentValue(value.get()));
      }
    }
    return new ModifierResult(result, ModifierCause.APPLIED);
  }
}
