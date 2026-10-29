package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.packet.status.PingRequestPacket;
import dev.shoko.minecraft.net.packet.status.PongResponsePacket;
import dev.shoko.minecraft.net.packet.status.StatusRequestPacket;
import dev.shoko.minecraft.net.packet.status.StatusResponsePacket;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.registry.StatusClientbound;
import dev.shoko.minecraft.net.registry.StatusServerbound;
import java.util.Map;

/** Fields of the status packets (R37). */
final class StatusCodecs {

  private StatusCodecs() {}

  static Map<PacketType, FieldDecoder> decoders() {
    return Map.of(
        StatusClientbound.STATUS_RESPONSE,
        in -> new StatusResponsePacket(in.readString(FieldLimits.STRING)),
        StatusClientbound.PONG_RESPONSE,
        in -> new PongResponsePacket(in.readLong()),
        StatusServerbound.STATUS_REQUEST,
        in -> new StatusRequestPacket(),
        StatusServerbound.PING_REQUEST,
        in -> new PingRequestPacket(in.readLong()));
  }

  static void encode(Packet packet, PacketWriter out) throws ProtocolException {
    switch (packet) {
      case StatusResponsePacket response -> out.writeString(response.json(), FieldLimits.STRING);
      case PongResponsePacket pong -> out.writeLong(pong.timestamp());
      case StatusRequestPacket request -> {}
      case PingRequestPacket ping -> out.writeLong(ping.timestamp());
      default -> throw new IllegalArgumentException("not a status packet: " + packet.type());
    }
  }
}
