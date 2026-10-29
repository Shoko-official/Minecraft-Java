package dev.shoko.minecraft.net.testing;

import java.util.Arrays;
import java.util.List;

/**
 * The uncompressed frames of the vectors file, as written there (reference script, sub-command
 * frames). Lengths and ids of these frames fit in one byte.
 */
public final class FrameVectors {

  /**
   * One vector: its name, the full frame bytes in hexadecimal, and the id and payload they hold.
   */
  public record Vector(String name, String frameHex) {

    /** The packet id. */
    public int id() {
      return Hex.bytes(frameHex)[1] & 0xff;
    }

    /** The payload after the id. */
    public byte[] payload() {
      byte[] frame = Hex.bytes(frameHex);
      return Arrays.copyOfRange(frame, 2, frame.length);
    }

    /** The full frame bytes. */
    public byte[] frame() {
      return Hex.bytes(frameHex);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /** F01 to F08, F15 and F16. */
  public static List<Vector> uncompressed() {
    return List.of(
        new Vector("F01 handshake status", "10 00 89 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 01"),
        new Vector("F02 status_request", "01 00"),
        new Vector("F03 ping_request", "09 01 01 23 45 67 89 ab cd ef"),
        new Vector("F04 handshake login", "10 00 89 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 02"),
        new Vector(
            "F05 login hello Notch",
            "17 00 05 4e 6f 74 63 68 b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f"),
        new Vector("F06 login_acknowledged", "01 03"),
        new Vector("F07 login_compression 256", "03 03 80 02"),
        new Vector("F08 finish_configuration", "01 03"),
        new Vector("F15 keep_alive", "09 04 11 22 33 44 55 66 77 88"),
        new Vector(
            "F16 status_response",
            "2c 00 2a 7b 22 76 65 72 73 69 6f 6e 22 3a 7b 22 6e 61 6d 65 22 3a 22 32 36 2e 33 22"
                + " 2c 22 70 72 6f 74 6f 63 6f 6c 22 3a 37 37 37 7d 7d"));
  }

  private FrameVectors() {}
}
