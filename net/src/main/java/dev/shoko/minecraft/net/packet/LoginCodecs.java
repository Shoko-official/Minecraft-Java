package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.packet.login.HelloClientboundPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.KeyPacket;
import dev.shoko.minecraft.net.packet.login.LoginAcknowledgedPacket;
import dev.shoko.minecraft.net.packet.login.LoginCompressionPacket;
import dev.shoko.minecraft.net.packet.login.LoginDisconnectPacket;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.LoginServerbound;
import dev.shoko.minecraft.net.registry.PacketType;
import java.util.Map;

/** Fields of the login packets (R28, R39, R40, R50, R51). */
final class LoginCodecs {

  private static final int NAME_CHARS = 16;
  private static final int SERVER_ID_CHARS = 20;

  private LoginCodecs() {}

  static Map<PacketType, FieldDecoder> decoders() {
    return Map.of(
        LoginClientbound.LOGIN_DISCONNECT,
        in -> new LoginDisconnectPacket(in.readString(FieldLimits.JSON_COMPONENT)),
        LoginClientbound.HELLO,
        LoginCodecs::helloClientbound,
        LoginClientbound.LOGIN_FINISHED,
        LoginCodecs::loginFinished,
        LoginClientbound.LOGIN_COMPRESSION,
        in -> new LoginCompressionPacket(in.readVarInt()),
        LoginServerbound.HELLO,
        in -> new HelloServerboundPacket(in.readString(NAME_CHARS), in.readUuid()),
        LoginServerbound.KEY,
        LoginCodecs::key,
        LoginServerbound.LOGIN_ACKNOWLEDGED,
        in -> new LoginAcknowledgedPacket());
  }

  private static Packet helloClientbound(PacketReader in) throws ProtocolException {
    String serverId = in.readString(SERVER_ID_CHARS);
    var publicKey = in.readPrefixedBytes(FieldLimits.BYTES);
    var verifyToken = in.readPrefixedBytes(FieldLimits.BYTES);
    return new HelloClientboundPacket(serverId, publicKey, verifyToken, in.readBoolean());
  }

  private static Packet loginFinished(PacketReader in) throws ProtocolException {
    var profile = ProfileCodec.read(in);
    return new LoginFinishedPacket(profile, in.readUuid());
  }

  private static Packet key(PacketReader in) throws ProtocolException {
    var sharedSecret = in.readPrefixedBytes(FieldLimits.BYTES);
    return new KeyPacket(sharedSecret, in.readPrefixedBytes(FieldLimits.BYTES));
  }

  static void encode(Packet packet, PacketWriter out) throws ProtocolException {
    switch (packet) {
      case LoginDisconnectPacket disconnect ->
          out.writeString(disconnect.reasonJson(), FieldLimits.JSON_COMPONENT);
      case HelloClientboundPacket hello -> {
        out.writeString(hello.serverId(), SERVER_ID_CHARS);
        out.writePrefixedBytes(hello.publicKey(), FieldLimits.BYTES);
        out.writePrefixedBytes(hello.verifyToken(), FieldLimits.BYTES);
        out.writeBoolean(hello.shouldAuthenticate());
      }
      case LoginFinishedPacket finished -> {
        ProfileCodec.write(out, finished.profile());
        out.writeUuid(finished.sessionId());
      }
      case LoginCompressionPacket compression -> out.writeVarInt(compression.threshold());
      case HelloServerboundPacket hello -> {
        out.writeString(hello.name(), NAME_CHARS);
        out.writeUuid(hello.playerUuid());
      }
      case KeyPacket key -> {
        out.writePrefixedBytes(key.sharedSecret(), FieldLimits.BYTES);
        out.writePrefixedBytes(key.verifyToken(), FieldLimits.BYTES);
      }
      case LoginAcknowledgedPacket acknowledged -> {}
      default -> throw new IllegalArgumentException("not a login packet: " + packet.type());
    }
  }
}
