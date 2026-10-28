package dev.shoko.minecraft.tools.oracle;

import dev.shoko.minecraft.tools.oracle.rcon.FakeRconServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Stand-in for the reference server, started as a separate process by the lifecycle tests. It reads
 * {@code server.properties} from its working directory like the real server does.
 *
 * <p>Modes: {@code ready} prints the ready line, serves RCON and exits on {@code stop}; {@code
 * ignore-stop} does the same but never exits by itself; {@code stubborn} is ready, then blocks
 * without reading its console, so only a kill ends it; {@code never-ready} prints startup noise and
 * waits forever; {@code crash} exits with code 3 before being ready.
 */
public final class FakeServerMain {

  private FakeServerMain() {}

  public static void main(String[] args) throws IOException, InterruptedException {
    String mode = args[0];
    System.out.println("[00:00:00] [Server thread/INFO]: Starting fake server");
    if (mode.equals("crash")) {
      System.out.println("[00:00:00] [Server thread/ERROR]: boom");
      System.exit(3);
    }
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(Path.of("server.properties"), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    FakeRconServer rcon = null;
    if (!mode.equals("never-ready")) {
      rcon =
          new FakeRconServer(
              Integer.parseInt(properties.getProperty("rcon.port")),
              properties.getProperty("rcon.password"),
              command -> "ran: " + command,
              FakeRconServer.Behavior.STANDARD);
      System.out.println("[00:00:01] [Server thread/INFO]: Done (0.123s)! For help, type \"help\"");
    }
    if (mode.equals("stubborn")) {
      new CountDownLatch(1).await();
    }
    BufferedReader console =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    String line;
    while ((line = console.readLine()) != null) {
      System.out.println("[00:00:02] [Server thread/INFO]: console: " + line);
      if (line.equals("stop") && mode.equals("ready")) {
        System.out.println("[00:00:03] [Server thread/INFO]: Stopping server");
        rcon.close();
        System.exit(0);
      }
    }
  }
}
