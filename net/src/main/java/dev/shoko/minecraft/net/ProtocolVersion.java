package dev.shoko.minecraft.net;

/** Identity of the protocol implemented by this module (R1, extracted version report). */
public final class ProtocolVersion {

  /** Protocol number sent in the handshake and in the status response. */
  public static final int PROTOCOL = 777;

  /** Game version name reported in the status response. */
  public static final String GAME_VERSION = "26.3";

  private ProtocolVersion() {}
}
