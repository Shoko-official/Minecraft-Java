package dev.shoko.minecraft.net.state;

/** Which way a packet travels. Ids are numbered separately for each direction (R2). */
public enum PacketDirection {
  /** Sent by the server, received by the client. */
  CLIENTBOUND,
  /** Sent by the client, received by the server. */
  SERVERBOUND
}
