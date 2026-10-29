package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.world.block.BlockProperty;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockType;
import java.util.List;

/** Made-up block states, built straight from the records of {@code world}. */
public final class Blocks {

  private static final BlockType STONE = new BlockType("minecraft:stone", 1, List.of(), 10, 10);

  private static final BlockType WHEAT =
      new BlockType(
          "minecraft:wheat",
          2,
          List.of(new BlockProperty("age", List.of("0", "1", "2", "3", "4", "5", "6", "7"))),
          20,
          20);

  private static final BlockType POT =
      new BlockType(
          "minecraft:decorated_pot",
          3,
          List.of(new BlockProperty("cracked", List.of("false", "true"))),
          40,
          40);

  private Blocks() {}

  public static BlockState stone() {
    return new BlockState(STONE, STONE.firstStateId());
  }

  /** A wheat block at the given age, 0 to 7. */
  public static BlockState wheat(int age) {
    return new BlockState(WHEAT, WHEAT.firstStateId() + age);
  }

  /** A decorated pot, cracked or not. */
  public static BlockState pot(boolean cracked) {
    return new BlockState(POT, POT.firstStateId() + (cracked ? 1 : 0));
  }

  /** A block of the given path without properties. */
  public static BlockState plain(String path, int protocolId, int stateId) {
    BlockType type = new BlockType("minecraft:" + path, protocolId, List.of(), stateId, stateId);
    return new BlockState(type, stateId);
  }
}
