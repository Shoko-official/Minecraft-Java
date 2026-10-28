package dev.shoko.minecraft.tools.oracle;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line: starts the reference server for a seed, runs RCON commands, prints their responses
 * and stops the server. Region files stay in the run directory under the cache.
 */
public final class OracleMain {

  /** The only way to accept the Minecraft EULA from the command line. */
  public static final String ACCEPT_EULA_FLAG = "--i-accept-the-minecraft-eula";

  static final int EXIT_OK = 0;
  static final int EXIT_FAILURE = 1;
  static final int EXIT_USAGE = 2;

  private static final String USAGE =
      "usage: OracleMain --name NAME --seed SEED "
          + ACCEPT_EULA_FLAG
          + " [--cache-dir DIR] [--level-name NAME] [--difficulty D] [--gamemode G]"
          + " [--command CMD]...";

  private OracleMain() {}

  public static void main(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  /** Runs the command line and returns the process exit code. */
  static int run(String[] args, PrintStream out, PrintStream err) {
    Arguments parsed;
    try {
      parsed = Arguments.parse(args);
    } catch (IllegalArgumentException e) {
      err.println("oracle: " + e.getMessage());
      err.println(USAGE);
      return EXIT_USAGE;
    }
    if (!parsed.acceptEula) {
      err.println("oracle: " + new EulaNotAcceptedException().getMessage());
      err.println(USAGE);
      return EXIT_USAGE;
    }
    OracleOptions options =
        OracleOptions.defaults(parsed.cacheDir, parsed.name, parsed.settings(), true);
    try (OracleServer server = OracleServer.start(options, out::println)) {
      for (String command : parsed.commands) {
        out.println("> " + command);
        out.println(server.command(command));
      }
      return EXIT_OK;
    } catch (IOException | IllegalArgumentException e) {
      err.println("oracle: " + e.getMessage());
      return EXIT_FAILURE;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return EXIT_FAILURE;
    }
  }

  /** Parsed options; parsing writes nothing and starts nothing. */
  private static final class Arguments {
    private Path cacheDir = Path.of(System.getenv().getOrDefault("MINECRAFT_CACHE_DIR", ".cache"));
    private String name;
    private Long seed;
    private String levelName = "world";
    private String difficulty = "easy";
    private String gamemode = "survival";
    private boolean acceptEula;
    private final List<String> commands = new ArrayList<>();

    static Arguments parse(String[] args) {
      Arguments parsed = new Arguments();
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        switch (arg) {
          case ACCEPT_EULA_FLAG -> parsed.acceptEula = true;
          case "--cache-dir" -> parsed.cacheDir = Path.of(value(args, ++i, arg));
          case "--name" -> parsed.name = value(args, ++i, arg);
          case "--seed" -> parsed.seed = parseSeed(value(args, ++i, arg));
          case "--level-name" -> parsed.levelName = value(args, ++i, arg);
          case "--difficulty" -> parsed.difficulty = value(args, ++i, arg);
          case "--gamemode" -> parsed.gamemode = value(args, ++i, arg);
          case "--command" -> parsed.commands.add(value(args, ++i, arg));
          default -> throw new IllegalArgumentException("unknown argument: " + arg);
        }
      }
      if (parsed.name == null || parsed.seed == null) {
        throw new IllegalArgumentException("--name and --seed are required");
      }
      OracleOptions.runDir(parsed.cacheDir, parsed.name);
      parsed.settings();
      return parsed;
    }

    ServerSettings settings() {
      return ServerSettings.of(seed)
          .withLevelName(levelName)
          .withDifficulty(difficulty)
          .withGamemode(gamemode);
    }

    private static String value(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException(option + " needs a value");
      }
      return args[index];
    }

    private static long parseSeed(String text) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("--seed must be a 64-bit integer: " + text, e);
      }
    }
  }
}
