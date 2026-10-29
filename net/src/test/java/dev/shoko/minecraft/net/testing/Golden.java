package dev.shoko.minecraft.net.testing;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the golden list of the 260 packets of protocol 777 (golden/packets-777.txt). */
public final class Golden {

  /** One row of the list: the pair, the id and the name. */
  public record PacketRow(ConnectionState state, PacketDirection direction, int id, String name) {
    @Override
    public String toString() {
      return state + " " + direction + " " + id + " " + name;
    }
  }

  private Golden() {}

  /** Returns every row, in the order of the file. */
  public static List<PacketRow> packets() {
    try (InputStream in = Golden.class.getResourceAsStream("/golden/packets-777.txt")) {
      if (in == null) {
        throw new IllegalStateException("golden/packets-777.txt is missing");
      }
      List<PacketRow> rows = new ArrayList<>();
      for (String line :
          new String(in.readAllBytes(), StandardCharsets.US_ASCII).lines().toList()) {
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] parts = line.split(" ", 4);
        rows.add(
            new PacketRow(
                ConnectionState.valueOf(parts[0].toUpperCase(Locale.ROOT)),
                PacketDirection.valueOf(parts[1].toUpperCase(Locale.ROOT)),
                Integer.parseInt(parts[2]),
                parts[3]));
      }
      return rows;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
