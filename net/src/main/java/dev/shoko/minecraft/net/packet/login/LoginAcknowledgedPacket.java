package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginServerbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Acknowledges the end of the login, serverbound; no field. The connection enters the configuration
 * state (R39, vector F06).
 */
public record LoginAcknowledgedPacket() implements Packet {

  @Override
  public PacketType type() {
    return LoginServerbound.LOGIN_ACKNOWLEDGED;
  }
}
