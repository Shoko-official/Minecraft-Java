package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Refuses the login with a reason, clientbound (R49). The server closes after sending it.
 *
 * @param reasonJson the reason as a JSON text component, String(262144) (R17)
 */
public record LoginDisconnectPacket(String reasonJson) implements Packet {

  @Override
  public PacketType type() {
    return LoginClientbound.LOGIN_DISCONNECT;
  }
}
