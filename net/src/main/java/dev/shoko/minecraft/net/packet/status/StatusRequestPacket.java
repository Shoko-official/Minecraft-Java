package dev.shoko.minecraft.net.packet.status;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.registry.StatusServerbound;

/** Asks for the server status; no field (R37, vector F02). */
public record StatusRequestPacket() implements Packet {

  @Override
  public PacketType type() {
    return StatusServerbound.STATUS_REQUEST;
  }
}
