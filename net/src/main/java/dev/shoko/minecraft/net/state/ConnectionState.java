package dev.shoko.minecraft.net.state;

/** The five states of a connection, in the order a session goes through them (R34 to R49). */
public enum ConnectionState {
  /** First state of every connection; accepts only the intention packet (R34). */
  HANDSHAKE,
  /** Server list ping (R37). */
  STATUS,
  /** Authentication and profile exchange (R39, R40). */
  LOGIN,
  /** Registries, tags and features sync; also entered again by a reconfiguration (R43, R45). */
  CONFIGURATION,
  /** The game itself. */
  PLAY
}
