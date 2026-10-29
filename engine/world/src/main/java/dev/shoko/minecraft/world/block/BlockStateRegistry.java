package dev.shoko.minecraft.world.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * All blocks and global block states of a loaded block report (R3, R5 to R7, R10).
 *
 * <p>Built from a {@link BlockReportSource}, which it checks: the state identifiers recomputed from
 * the property lists alone (R5, R6) must equal the reported ones, cover {@code 0} to {@code
 * stateCount() - 1} exactly once, and each block must have exactly one default state. The reference
 * data has 1286 blocks and 35 723 states. Immutable once built.
 */
public final class BlockStateRegistry {

  private static final String AIR_ID = "minecraft:air";

  private final List<BlockType> blocks;
  private final Map<String, BlockType> byId;
  private final BlockState[] states;

  private BlockStateRegistry(List<BlockType> blocks, BlockState[] states) {
    this.blocks = List.copyOf(blocks);
    this.states = states;
    Map<String, BlockType> index = new HashMap<>();
    for (BlockType block : blocks) {
      index.put(block.id(), block);
    }
    this.byId = index;
  }

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
    List<BlockReportEntry> entries = source.blocks();
    if (entries.isEmpty() || !entries.getFirst().id().equals(AIR_ID)) {
      throw new BlockReportException("the first block of the registry must be " + AIR_ID);
    }
    List<BlockType> blocks = new ArrayList<>(entries.size());
    List<BlockState> states = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    int nextStateId = 0;
    for (int protocolId = 0; protocolId < entries.size(); protocolId++) {
      BlockReportEntry entry = entries.get(protocolId);
      if (!seen.add(entry.id())) {
        throw new BlockReportException(entry.id(), "duplicate block identifier");
      }
      BlockType block = checkedBlock(entry, protocolId, nextStateId);
      blocks.add(block);
      for (int k = 0; k < block.stateCount(); k++) {
        states.add(new BlockState(block, block.firstStateId() + k));
      }
      nextStateId += block.stateCount();
    }
    return new BlockStateRegistry(blocks, states.toArray(new BlockState[0]));
  }

  private static BlockType checkedBlock(BlockReportEntry entry, int protocolId, int firstStateId)
      throws BlockReportException {
    List<BlockProperty> sorted =
        entry.properties().stream().sorted(Comparator.comparing(BlockProperty::name)).toList();
    checkProperties(entry.id(), sorted);
    BlockType draft = new BlockType(entry.id(), protocolId, sorted, firstStateId, firstStateId);
    int count = draft.stateCount();
    if (entry.states().size() != count) {
      throw new BlockReportException(
          entry.id(), entry.states().size() + " states reported, the properties give " + count);
    }
    boolean[] covered = new boolean[count];
    int defaultStateId = -1;
    for (ReportedState state : entry.states()) {
      int expected = expectedId(draft, state);
      if (state.id() != expected) {
        throw new BlockReportException(
            entry.id(),
            "state "
                + state.properties()
                + " has id "
                + state.id()
                + ", the formula gives "
                + expected);
      }
      if (covered[expected - firstStateId]) {
        throw new BlockReportException(entry.id(), "duplicate state " + state.properties());
      }
      covered[expected - firstStateId] = true;
      if (state.isDefault()) {
        if (defaultStateId >= 0) {
          throw new BlockReportException(entry.id(), "more than one default state");
        }
        defaultStateId = expected;
      }
    }
    if (defaultStateId < 0) {
      throw new BlockReportException(entry.id(), "no default state");
    }
    return new BlockType(entry.id(), protocolId, sorted, firstStateId, defaultStateId);
  }

  private static void checkProperties(String blockId, List<BlockProperty> sorted)
      throws BlockReportException {
    String previous = null;
    for (BlockProperty property : sorted) {
      if (property.name().equals(previous)) {
        throw new BlockReportException(blockId, "duplicate property " + property.name());
      }
      previous = property.name();
      if (property.values().isEmpty()
          || property.values().size() != Set.copyOf(property.values()).size()) {
        throw new BlockReportException(
            blockId, "property " + property.name() + " has no values or repeated ones");
      }
    }
  }

  private static int expectedId(BlockType draft, ReportedState state) throws BlockReportException {
    try {
      return draft.stateId(state.properties());
    } catch (IllegalArgumentException e) {
      throw new BlockReportException(
          draft.id(),
          "state " + state.properties() + " does not match the properties: " + e.getMessage());
    }
  }

  /**
   * Returns the number of blocks.
   *
   * @return the block count, 1286 for the reference data
   */
  public int blockCount() {
    return blocks.size();
  }

  /**
   * Returns the number of global states.
   *
   * @return the state count, 35 723 for the reference data
   */
  public int stateCount() {
    return states.length;
  }

  /**
   * Returns all blocks by increasing protocol id.
   *
   * @return an unmodifiable list
   */
  public List<BlockType> blocks() {
    return blocks;
  }

  /**
   * Finds a block by identifier.
   *
   * @param id identifier with namespace
   * @return the block, or empty when unknown
   */
  public Optional<BlockType> block(String id) {
    return Optional.ofNullable(byId.get(id));
  }

  /**
   * Returns the block at a protocol id.
   *
   * @param protocolId position in the block registry
   * @return the block
   * @throws IllegalArgumentException if out of range
   */
  public BlockType blockByProtocolId(int protocolId) {
    if (protocolId < 0 || protocolId >= blocks.size()) {
      throw new IllegalArgumentException("no block with protocol id " + protocolId);
    }
    return blocks.get(protocolId);
  }

  /**
   * Returns the state with a global identifier (R5).
   *
   * @param id identifier in {@code [0, stateCount())}
   * @return the state
   * @throws IllegalArgumentException if out of range, negative included
   */
  public BlockState state(int id) {
    if (id < 0 || id >= states.length) {
      throw new IllegalArgumentException("no state with id " + id);
    }
    return states[id];
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
    return state(block.stateId(values));
  }

  /**
   * Returns the default state of a block (R10): not always the first state.
   *
   * @param block a block of this registry
   * @return the default state
   */
  public BlockState defaultState(BlockType block) {
    return state(block.defaultStateId());
  }

  /**
   * Returns the default state of {@code minecraft:air}, the default value of the block registry
   * (R4, R11): identifier 0.
   *
   * @return the air state
   */
  public BlockState air() {
    return states[0];
  }
}
