package dev.shoko.minecraft.tools.oracle;

/**
 * Starting the reference server was refused because the caller did not state that the Minecraft
 * EULA was accepted. Accepting it is a personal act of the repository owner: no default, no
 * configuration file and no environment variable can do it on their behalf.
 */
public final class EulaNotAcceptedException extends IllegalStateException {

  public static final String EULA_URL = "https://aka.ms/MinecraftEULA";

  private static final long serialVersionUID = 1L;

  EulaNotAcceptedException() {
    super(
        "Refusing to start the reference server: the Minecraft EULA ("
            + EULA_URL
            + ") has not been accepted. Only the repository owner can accept it, by passing"
            + " acceptEula explicitly (command line: "
            + OracleMain.ACCEPT_EULA_FLAG
            + "). Nothing was written.");
  }
}
