package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;
import dev.shoko.minecraft.loot.random.ChosenRandom;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import dev.shoko.minecraft.loot.random.LootRandom;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unpacks a container that carries a loot table (LT-R68 to LT-R70). The caller removes {@code
 * LootTable} and {@code LootTableSeed} from the container when it stores the result; this class
 * only computes the content.
 */
public final class ContainerUnpacker {

  private ContainerUnpacker() {}

  /**
   * Draws the table of the binding with a context of type {@code chest} (LT-R69), then places the
   * stacks. The seed of the binding is the explicit seed of LT-R63 (0 or absent means none). The
   * stacks, split by LT-R54, go to distinct slots chosen uniformly among the free ones; the
   * multi-set of stacks is exact, the slot order is provisional (LT-R70, probe PL5). The choice of
   * slots is reported with the reason {@code CONTAINER_PLACEMENT}.
   *
   * @param binding the table and the seed read from the container
   * @param containerSize the number of slots of the container
   * @param context the chest context: {@code origin} is the center of the block, {@code
   *     this_entity} the player who opens, with their luck
   * @param sequences the persistent named sequences
   * @return the stacks by slot; all slots empty with a {@code TABLE_NOT_FOUND} finding when the
   *     table is not loaded
   */
  public static UnpackResult unpack(
      LootBinding.Bound binding,
      int containerSize,
      LootContext context,
      RandomSequenceStore sequences) {
    LootTable table = context.data().tables().find(binding.table()).orElse(null);
    List<ItemStack> slots = new ArrayList<>(Collections.nCopies(containerSize, ItemStack.empty()));
    if (table == null) {
      return new UnpackResult(
          List.copyOf(slots),
          List.of(
              new LootFinding(
                  LootFindingCode.TABLE_NOT_FOUND,
                  "$",
                  "table is not loaded: " + binding.table())));
    }
    long seed = binding.seeded() ? binding.seed() : 0L;
    ChosenRandom chosen =
        LootRandom.choose(seed, table.randomSequence(), sequences, context.random());
    LootContext drawn = context.withRandom(chosen.source());
    LootResult result = LootEvaluator.evaluate(table, drawn);
    place(result.stacks(), slots, drawn);
    return new UnpackResult(List.copyOf(slots), result.findings());
  }

  /** Puts each stack in a free slot chosen uniformly, in order; stacks beyond the size are lost. */
  private static void place(List<ItemStack> stacks, List<ItemStack> slots, LootContext context) {
    List<Integer> free = new ArrayList<>();
    for (int slot = 0; slot < slots.size(); slot++) {
      free.add(slot);
    }
    for (ItemStack stack : stacks) {
      if (free.isEmpty()) {
        return;
      }
      int index =
          Draws.observe(
              context,
              DrawReason.CONTAINER_PLACEMENT,
              "container",
              () -> context.random().nextInt(free.size()));
      int position = index;
      int slot = free.get(position);
      free.set(position, free.getLast());
      free.removeLast();
      slots.set(slot, stack);
    }
  }
}
