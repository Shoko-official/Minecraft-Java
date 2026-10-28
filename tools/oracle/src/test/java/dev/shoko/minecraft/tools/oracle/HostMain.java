package dev.shoko.minecraft.tools.oracle;

import java.nio.file.Path;
import java.time.Duration;

/**
 * A JVM that starts a server which never exits by itself, prints its pid, then exits without
 * stopping it: only the shutdown hook of {@link OracleServer} can end the server.
 */
public final class HostMain {

  private HostMain() {}

  public static void main(String[] args) throws Exception {
    OracleOptions options =
        OracleServerTest.fake(
            Path.of(args[0]), "stubborn", true, Duration.ofSeconds(30), Duration.ofSeconds(5));
    OracleServer server = OracleServer.start(options, line -> {});
    System.out.println("PID " + server.pid());
    System.out.flush();
    System.exit(0);
  }
}
