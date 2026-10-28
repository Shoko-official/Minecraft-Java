package dev.shoko.minecraft.world.block;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All blocks and global block states of a loaded block report (R3, R5 to R7, R10).
 *
 * <p>Built from a {@link BlockReportSource}, which it checks: the state identifiers recomputed from
 * the property lists alone (R5, R6) must equal the reported ones, cover {@code 0} to {@code
 * stateCount() - 1} exactly once, and each block must have exactly one default state. The reference
 * data has 1286 blocks and 35 723 states. Immutable once built.
 */
public final class BlockStateRegistry {

  private BlockStateRegistry() {}

  /**
   * Builds and verifies the registry (R3, R5 to R7).
   *
   * @param source the report, blocks in protocol id order
   * @return the registry
   * @throws BlockReportException on any inconsistency: a missing, duplicate or contradicting state
   *     identifier, a block without exactly one default state, a state whose values are not a point
   *     of the product of the property lists, or a duplicate block identifier
   */
  public static BlockStateRegistry load(BlockReportSource source) throws BlockReportException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of blocks.
   *
   * @return the block count, 1286 for the reference data
   */
  public int blockCount() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of global states.
   *
   * @return the state count, 35 723 for the reference data
   */
  public int stateCount() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns all blocks by increasing protocol id.
   *
   * @return an unmodifiable list
   */
  public List<BlockType> blocks() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds a block by identifier.
   *
   * @param id identifier with namespace
   * @return the block, or empty when unknown
   */
  public Optional<BlockType> block(String id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the block at a protocol id.
   *
   * @param protocolId position in the block registry
   * @return the block
   * @throws IllegalArgumentException if out of range
   */
  public BlockType blockByProtocolId(int protocolId) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the state with a global identifier (R5).
   *
   * @param id identifier in {@code [0, stateCount())}
   * @return the state
   * @throws IllegalArgumentException if out of range, negative included
   */
  public BlockState state(int id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the state of a block with exactly these values (R6).
   *
   * @param block a block of this registry
   * @param values one value per property, by name, in any order
   * @return the state
   * @throws IllegalArgumentException as {@link BlockType#stateId(Map)}
   */
  public BlockState state(BlockType block, Map<String, String> values) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the default state of a block (R10): not always the first state.
   *
   * @param block a block of this registry
   * @return the default state
   */
  public BlockState defaultState(BlockType block) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the default state of {@code minecraft:air}, the default value of the block registry
   * (R4, R11): identifier 0.
   *
   * @return the air state
   */
  public BlockState air() {
    return Skeleton.throwNotImplemented();
  }
}
