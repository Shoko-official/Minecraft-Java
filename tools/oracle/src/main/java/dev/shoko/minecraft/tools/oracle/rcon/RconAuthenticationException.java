package dev.shoko.minecraft.tools.oracle.rcon;

import java.io.IOException;

/** The server refused the RCON password. The message never contains the password. */
public final class RconAuthenticationException extends IOException {

  private static final long serialVersionUID = 1L;

  public RconAuthenticationException(String message) {
    super(message);
  }
}
