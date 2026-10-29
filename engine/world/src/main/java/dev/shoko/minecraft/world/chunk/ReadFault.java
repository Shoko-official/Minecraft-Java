package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.world.RefusedCause;

/** Internal: a fault that refuses the chunk being read, carried up to the codec entry point. */
final class ReadFault extends Exception {

  private static final long serialVersionUID = 1L;

  private final RefusedCause cause;

  ReadFault(RefusedCause cause, String message) {
    super(message);
    this.cause = cause;
  }

  RefusedCause refusal() {
    return cause;
  }
}
