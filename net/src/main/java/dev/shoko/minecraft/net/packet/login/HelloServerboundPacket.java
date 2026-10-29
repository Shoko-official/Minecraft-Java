package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginServerbound;
import dev.shoko.minecraft.net.registry.PacketType;
import java.util.UUID;

/**
 * Starts the login, serverbound (R39, R57, vector F05).
 *
 * @param name the player name, String(16)
 * @param playerUuid the UUID the client claims; the server in offline mode ignores it (R57)
 */
public record HelloServerboundPacket(String name, UUID playerUuid) implements Packet {

  @Override
  public PacketType type() {
    return LoginServerbound.HELLO;
  }
}
