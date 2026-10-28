package dev.shoko.minecraft.world.block;

import java.util.Optional;

/**
 * The block report is inconsistent: a count, an identifier or a default state contradicts the rules
 * of {@code world-block-states.md} (R3, R5 to R7). Loading data never falls back to a default
 * value: the fault names the block (architecture: errors).
 */
public final class BlockReportException extends Exception {

  private static final long serialVersionUID = 1L;

  // Empty string stands for "no block": a serializable field cannot be an Optional.
  private final String blockId;

  /**
   * Creates a fault that concerns the whole report, such as a wrong total of states.
   *
   * @param message a description for humans
   */
  public BlockReportException(String message) {
    super(message);
    this.blockId = "";
  }

  /**
   * Creates a fault that concerns one block.
   *
   * @param blockId identifier of the faulty block
   * @param message a description for humans
   */
  public BlockReportException(String blockId, String message) {
    super(message);
    this.blockId = blockId;
  }

  /**
   * Returns the faulty block.
   *
   * @return the block identifier, or empty for a fault of the whole report
   */
  public Optional<String> blockId() {
    return blockId.isEmpty() ? Optional.empty() : Optional.of(blockId);
  }
}
