package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.buffer.PacketWriter;

/** Maximums shared by several packet codecs (R9, R14, R17). */
final class FieldLimits {

  /** String(32767), the default maximum of a string field (R14). */
  static final int STRING = 32767;

  /** A JSON text component, the only string above 32767 characters (R17). */
  static final int JSON_COMPONENT = 262144;

  /** Bytes with no documented maximum are bounded by the frame (R27, R90). */
  static final int BYTES = PacketWriter.DEFAULT_MAX_SIZE;

  /**
   * Elements of one array of a registry or tags packet, which have no documented maximum: a project
   * choice (R90, R91) far above real data (35 723 block states, R61) that keeps a small frame from
   * announcing millions of boxed elements.
   */
  static final int ARRAY = 1_000_000;

  private FieldLimits() {}
}
