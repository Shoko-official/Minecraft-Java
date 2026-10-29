package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.item.Decoding;

/**
 * The NBT layout of the block entities that hold items, as found in the structure templates
 * (IC-R31). Reading is lenient where the spec says so and answers with findings; an absent field
 * takes the default of its type (0, empty list), which is provisional. Keys this class does not
 * know are not an error; writing produces only the keys of the layout, in the order of the
 * templates.
 *
 * <p>The layouts of the hopper ({@code TransferCooldown}), the decorated pot and the vault are not
 * typed here: {@link #readContents} reads their {@code Items}.
 */
public final class InventoryLayouts {

  private InventoryLayouts() {}

  /**
   * Creates the layouts reader and writer.
   *
   * @param items the codec of {@code Items} lists
   * @return the layouts
   */
  public static InventoryLayouts of(ContainerItemsCodec items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the {@code Items}, {@code LootTable} and {@code LootTableSeed} of a block entity (IC-R31,
   * IC-R32).
   *
   * @param blockEntity the block entity compound
   * @param size the number of slots of the block entity
   * @return the contents
   */
  public Decoding<ContainerContents> readContents(CompoundTag blockEntity, int size) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the contents into a block entity compound.
   *
   * @param contents the contents
   * @return {@code Items} then, when bound, {@code LootTable} and {@code LootTableSeed} when seeded
   */
  public CompoundTag writeContents(ContainerContents contents) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a furnace, blast furnace or smoker (3 slots, IC-R31, IC-R33).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<FurnaceData> readFurnace(CompoundTag blockEntity) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a furnace.
   *
   * @param furnace the state
   * @return {@code Items}, the four Short fields, then {@code RecipesUsed}
   */
  public CompoundTag writeFurnace(FurnaceData furnace) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a brewing stand (5 slots, IC-R31).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<BrewingStandData> readBrewingStand(CompoundTag blockEntity) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a brewing stand.
   *
   * @param stand the state
   * @return {@code Items}, {@code BrewTime} Short and {@code Fuel} Byte
   */
  public CompoundTag writeBrewingStand(BrewingStandData stand) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a campfire (4 slots, IC-R31).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<CampfireData> readCampfire(CompoundTag blockEntity) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a campfire.
   *
   * @param campfire the state
   * @return {@code Items}, {@code CookingTimes} and {@code CookingTotalTimes} as Int_Array
   */
  public CompoundTag writeCampfire(CampfireData campfire) {
    return Skeleton.throwNotImplemented();
  }
}
