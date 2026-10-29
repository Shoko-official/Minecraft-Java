package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EntityMotion;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.port.TypeSpecific;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A hand-made {@link EntityView}; every value is set by the test. */
public final class FakeEntity implements EntityView {

  private final Identifier type;
  private Vec3 position = new Vec3(0, 64, 0);
  private final Set<EntityFlag> flags = EnumSet.noneOf(EntityFlag.class);
  private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
  private final Map<Identifier, ComponentValue> components = new HashMap<>();
  private EntityView vehicle;
  private EntityMotion motion = new EntityMotion(0, 0);
  private int tickCount;
  private TypeSpecific typeSpecific;

  private FakeEntity(Identifier type) {
    this.type = type;
  }

  /** An entity of a type given by its path in the {@code minecraft} namespace. */
  public static FakeEntity of(String path) {
    return new FakeEntity(Identifier.minecraft(path));
  }

  public FakeEntity at(Vec3 where) {
    this.position = where;
    return this;
  }

  public FakeEntity flag(EntityFlag flag) {
    flags.add(flag);
    return this;
  }

  public FakeEntity wearing(EquipmentSlot slot, ItemStack stack) {
    equipment.put(slot, stack);
    return this;
  }

  public FakeEntity component(String id, ComponentValue value) {
    components.put(Identifier.parse(id), value);
    return this;
  }

  public FakeEntity riding(EntityView mount) {
    this.vehicle = mount;
    return this;
  }

  public FakeEntity moving(double horizontalSpeed, double fallDistance) {
    this.motion = new EntityMotion(horizontalSpeed, fallDistance);
    return this;
  }

  public FakeEntity ticks(int ticks) {
    this.tickCount = ticks;
    return this;
  }

  public FakeEntity specific(TypeSpecific state) {
    this.typeSpecific = state;
    return this;
  }

  @Override
  public Identifier type() {
    return type;
  }

  @Override
  public Vec3 position() {
    return position;
  }

  @Override
  public boolean hasFlag(EntityFlag flag) {
    return flags.contains(flag);
  }

  @Override
  public ItemStack equipment(EquipmentSlot slot) {
    return equipment.getOrDefault(slot, ItemStack.empty());
  }

  @Override
  public Optional<ComponentValue> component(Identifier component) {
    return Optional.ofNullable(components.get(component));
  }

  @Override
  public Optional<EntityView> vehicle() {
    return Optional.ofNullable(vehicle);
  }

  @Override
  public EntityMotion motion() {
    return motion;
  }

  @Override
  public int tickCount() {
    return tickCount;
  }

  @Override
  public Optional<TypeSpecific> typeSpecific() {
    return Optional.ofNullable(typeSpecific);
  }
}
