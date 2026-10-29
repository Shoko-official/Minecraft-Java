package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.registry.HandshakeServerbound;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import java.util.Map;

/** Fields of the handshake packets (R34). */
final class HandshakeCodecs {

  private static final int ADDRESS_CHARS = 255;

  private HandshakeCodecs() {}

  static Map<PacketType, FieldDecoder> decoders() {
    return Map.of(HandshakeServerbound.INTENTION, HandshakeCodecs::intention);
  }

  private static Packet intention(PacketReader in) throws ProtocolException {
    int protocolVersion = in.readVarInt();
    String address = in.readString(ADDRESS_CHARS);
    int port = in.readUnsignedShort();
    ConnectionIntent intent = ConnectionIntent.fromId(in.readVarInt());
    return new IntentionPacket(protocolVersion, address, port, intent);
  }

  static void encode(Packet packet, PacketWriter out) throws ProtocolException {
    if (!(packet instanceof IntentionPacket intention)) {
      throw new IllegalArgumentException("not a handshake packet: " + packet.type());
    }
    out.writeVarInt(intention.protocolVersion());
    out.writeString(intention.serverAddress(), ADDRESS_CHARS);
    out.writeShort(intention.serverPort());
    out.writeVarInt(intention.intent().id());
  }
}
