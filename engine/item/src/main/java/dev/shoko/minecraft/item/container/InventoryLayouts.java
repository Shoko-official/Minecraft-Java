package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

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

  private static final String ITEMS = "Items";
  private static final String LOOT_TABLE = "LootTable";
  private static final String LOOT_TABLE_SEED = "LootTableSeed";
  private static final String RECIPES_USED = "RecipesUsed";
  private static final String BREW_TIME = "BrewTime";
  private static final String FUEL = "Fuel";
  private static final String COOKING_TIMES = "CookingTimes";
  private static final String COOKING_TOTAL_TIMES = "CookingTotalTimes";
  private static final int FURNACE_SLOTS = 3;
  private static final int BREWING_SLOTS = 5;
  private static final int CAMPFIRE_SLOTS = 4;

  private final ContainerItemsCodec items;

  private InventoryLayouts(ContainerItemsCodec items) {
    this.items = items;
  }

  /**
   * Creates the layouts reader and writer.
   *
   * @param items the codec of {@code Items} lists
   * @return the layouts
   */
  public static InventoryLayouts of(ContainerItemsCodec items) {
    return new InventoryLayouts(items);
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
    return read(
        warnings -> new ContainerContents(slots(blockEntity, size, warnings), loot(blockEntity)));
  }

  /**
   * Writes the contents into a block entity compound.
   *
   * @param contents the contents
   * @return {@code Items} then, when bound, {@code LootTable} and {@code LootTableSeed} when seeded
   */
  public CompoundTag writeContents(ContainerContents contents) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put(ITEMS, items.write(contents.slots()));
    if (contents.loot() instanceof LootBinding.Bound bound) {
      entries.put(LOOT_TABLE, new StringTag(bound.table().toString()));
      if (bound.seeded()) {
        entries.put(LOOT_TABLE_SEED, new LongTag(bound.seed()));
      }
    }
    return CompoundTag.of(entries);
  }

  /**
   * Reads a furnace, blast furnace or smoker (3 slots, IC-R31, IC-R33).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<FurnaceData> readFurnace(CompoundTag blockEntity) {
    return read(
        warnings ->
            new FurnaceData(
                slots(blockEntity, FURNACE_SLOTS, warnings),
                blockEntity.getShort("lit_time_remaining"),
                blockEntity.getShort("lit_total_time"),
                blockEntity.getShort("cooking_time_spent"),
                blockEntity.getShort("cooking_total_time"),
                recipesUsed(blockEntity)));
  }

  /**
   * Writes a furnace.
   *
   * @param furnace the state
   * @return {@code Items}, the four Short fields, then {@code RecipesUsed}
   */
  public CompoundTag writeFurnace(FurnaceData furnace) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put(ITEMS, items.write(furnace.slots()));
    entries.put("lit_time_remaining", new ShortTag((short) furnace.litTimeRemaining()));
    entries.put("lit_total_time", new ShortTag((short) furnace.litTotalTime()));
    entries.put("cooking_time_spent", new ShortTag((short) furnace.cookingTimeSpent()));
    entries.put("cooking_total_time", new ShortTag((short) furnace.cookingTotalTime()));
    SequencedMap<String, NbtTag> used = new LinkedHashMap<>();
    furnace.recipesUsed().forEach((id, count) -> used.put(id.toString(), new IntTag(count)));
    entries.put(RECIPES_USED, CompoundTag.of(used));
    return CompoundTag.of(entries);
  }

  /**
   * Reads a brewing stand (5 slots, IC-R31).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<BrewingStandData> readBrewingStand(CompoundTag blockEntity) {
    return read(
        warnings ->
            new BrewingStandData(
                slots(blockEntity, BREWING_SLOTS, warnings),
                blockEntity.getShort(BREW_TIME),
                blockEntity.getByte(FUEL)));
  }

  /**
   * Writes a brewing stand.
   *
   * @param stand the state
   * @return {@code Items}, {@code BrewTime} Short and {@code Fuel} Byte
   */
  public CompoundTag writeBrewingStand(BrewingStandData stand) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put(ITEMS, items.write(stand.slots()));
    entries.put(BREW_TIME, new ShortTag((short) stand.brewTime()));
    entries.put(FUEL, new ByteTag((byte) stand.fuel()));
    return CompoundTag.of(entries);
  }

  /**
   * Reads a campfire (4 slots, IC-R31).
   *
   * @param blockEntity the block entity compound
   * @return the state
   */
  public Decoding<CampfireData> readCampfire(CompoundTag blockEntity) {
    return read(
        warnings ->
            new CampfireData(
                slots(blockEntity, CAMPFIRE_SLOTS, warnings),
                fourInts(blockEntity.getIntArray(COOKING_TIMES)),
                fourInts(blockEntity.getIntArray(COOKING_TOTAL_TIMES))));
  }

  /**
   * Writes a campfire.
   *
   * @param campfire the state
   * @return {@code Items}, {@code CookingTimes} and {@code CookingTotalTimes} as Int_Array
   */
  public CompoundTag writeCampfire(CampfireData campfire) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put(ITEMS, items.write(campfire.slots()));
    entries.put(COOKING_TIMES, intArray(campfire.cookingTimes()));
    entries.put(COOKING_TOTAL_TIMES, intArray(campfire.cookingTotalTimes()));
    return CompoundTag.of(entries);
  }

  private static IntArrayTag intArray(List<Integer> values) {
    return IntArrayTag.of(values.stream().mapToInt(Integer::intValue).toArray());
  }

  /** Four values: the array padded with zeros or cut (provisional: the template has four). */
  private static List<Integer> fourInts(int[] array) {
    List<Integer> values = new ArrayList<>(CAMPFIRE_SLOTS);
    for (int i = 0; i < CAMPFIRE_SLOTS; i++) {
      values.add(i < array.length ? array[i] : 0);
    }
    return List.copyOf(values);
  }

  private List<ItemStack> slots(CompoundTag blockEntity, int size, List<Finding> warnings) {
    Decoding<List<ItemStack>> read = items.read(blockEntity.getList(ITEMS), size);
    if (read instanceof Decoding.Success<List<ItemStack>> success) {
      warnings.addAll(success.findings());
      return success.value();
    }
    throw new FindingException(read.findings());
  }

  private static LootBinding loot(CompoundTag blockEntity) {
    if (blockEntity.get(LOOT_TABLE).isEmpty()) {
      return new LootBinding.None();
    }
    if (!(blockEntity.get(LOOT_TABLE).get() instanceof StringTag table)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, LOOT_TABLE, "expected a string");
    }
    Identifier id = identifier(table.value(), LOOT_TABLE);
    boolean seeded = blockEntity.get(LOOT_TABLE_SEED).orElse(null) instanceof NumericTag;
    return new LootBinding.Bound(id, seeded, blockEntity.getLong(LOOT_TABLE_SEED));
  }

  private static SequencedMap<Identifier, Integer> recipesUsed(CompoundTag blockEntity) {
    SequencedMap<Identifier, Integer> used = new LinkedHashMap<>();
    for (Map.Entry<String, NbtTag> entry :
        blockEntity.getCompound(RECIPES_USED).entries().entrySet()) {
      String path = RECIPES_USED + "." + entry.getKey();
      if (!(entry.getValue() instanceof NumericTag count)) {
        throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected an integer");
      }
      used.put(identifier(entry.getKey(), path), count.intValue());
    }
    return used;
  }

  private static Identifier identifier(String text, String path) {
    return Identifier.tryParse(text)
        .orElseThrow(
            () ->
                new FindingException(
                    FindingCode.INVALID_IDENTIFIER, path, "not an identifier: " + text));
  }

  private static <T> Decoding<T> read(Function<List<Finding>, T> body) {
    List<Finding> warnings = new ArrayList<>();
    try {
      return new Decoding.Success<>(body.apply(warnings), List.copyOf(warnings));
    } catch (FindingException refusal) {
      List<Finding> all = new ArrayList<>(warnings);
      all.addAll(refusal.findings());
      return new Decoding.Failure<>(List.copyOf(all));
    }
  }
}
