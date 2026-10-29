package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;

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
    return Skeleton.throwNotImplemented();
  }
}
