package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.type.GameProfile;
import java.util.UUID;

/**
 * Ends the login, clientbound (R39, R57). The client answers with login acknowledged.
 *
 * @param profile the profile of the player (R20)
 * @param sessionId the session UUID, as written in the table of the spec (R3)
 */
public record LoginFinishedPacket(GameProfile profile, UUID sessionId) implements Packet {

  @Override
  public PacketType type() {
    return LoginClientbound.LOGIN_FINISHED;
  }
}
