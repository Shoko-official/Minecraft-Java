package dev.shoko.minecraft.tools.oracle;

import java.util.List;

/**
 * Process that runs the server, and the check its jar must pass before any launch.
 *
 * @param args program and arguments, run from the run directory
 * @param reference check of the reference jar; {@link ReferenceJar#NONE} for test doubles
 */
public record ServerCommand(List<String> args, ReferenceJar reference) {

  public ServerCommand {
    args = List.copyOf(args);
    if (args.isEmpty()) {
      throw new IllegalArgumentException("command must not be empty");
    }
  }

  /** A command that does not run the reference jar, such as a test double. */
  public static ServerCommand unchecked(List<String> args) {
    return new ServerCommand(args, ReferenceJar.NONE);
  }
}
