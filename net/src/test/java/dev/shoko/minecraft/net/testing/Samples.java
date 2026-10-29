package dev.shoko.minecraft.net.testing;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.packet.configuration.DisconnectPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.PingPacket;
import dev.shoko.minecraft.net.packet.configuration.PongPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryDataPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryEntry;
import dev.shoko.minecraft.net.packet.configuration.RegistryTags;
import dev.shoko.minecraft.net.packet.configuration.TagIds;
import dev.shoko.minecraft.net.packet.configuration.UpdateTagsPacket;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.packet.login.HelloClientboundPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.KeyPacket;
import dev.shoko.minecraft.net.packet.login.LoginAcknowledgedPacket;
import dev.shoko.minecraft.net.packet.login.LoginCompressionPacket;
import dev.shoko.minecraft.net.packet.login.LoginDisconnectPacket;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.packet.status.PingRequestPacket;
import dev.shoko.minecraft.net.packet.status.PongResponsePacket;
import dev.shoko.minecraft.net.packet.status.StatusRequestPacket;
import dev.shoko.minecraft.net.packet.status.StatusResponsePacket;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.type.GameProfile;
import dev.shoko.minecraft.net.type.ProfileProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * One instance of each of the 21 packets of the first batch, with the state, direction and name the
 * spec gives them (tables of the section "Paquets").
 */
public final class Samples {

  /** One packet with the identity the spec table gives it. */
  public record Sample(
      ConnectionState state, PacketDirection direction, String name, Packet packet) {
    @Override
    public String toString() {
      return state + " " + direction + " " + name;
    }
  }

  public static final UUID NOTCH = UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f");

  private Samples() {}

  /** The 21 packets of the first batch. */
  public static List<Sample> all() {
    ConnectionState handshake = ConnectionState.HANDSHAKE;
    ConnectionState status = ConnectionState.STATUS;
    ConnectionState login = ConnectionState.LOGIN;
    ConnectionState config = ConnectionState.CONFIGURATION;
    PacketDirection clientbound = PacketDirection.CLIENTBOUND;
    PacketDirection serverbound = PacketDirection.SERVERBOUND;
    return List.of(
        new Sample(
            handshake,
            serverbound,
            "intention",
            new IntentionPacket(777, "localhost", 25565, ConnectionIntent.LOGIN)),
        new Sample(status, clientbound, "status_response", new StatusResponsePacket("{\"a\":1}")),
        new Sample(status, clientbound, "pong_response", new PongResponsePacket(-5L)),
        new Sample(status, serverbound, "status_request", new StatusRequestPacket()),
        new Sample(status, serverbound, "ping_request", new PingRequestPacket(0x0123456789abcdefL)),
        new Sample(
            login,
            clientbound,
            "login_disconnect",
            new LoginDisconnectPacket("{\"text\":\"bye\"}")),
        new Sample(
            login,
            clientbound,
            "hello",
            new HelloClientboundPacket(
                "", ByteBlob.of(Hex.ramp(162)), ByteBlob.of(Hex.bytes("01 02 03 04")), true)),
        new Sample(
            login,
            clientbound,
            "login_finished",
            new LoginFinishedPacket(
                new GameProfile(
                    NOTCH,
                    "Notch",
                    List.of(
                        new ProfileProperty.Unsigned("a", "b"),
                        new ProfileProperty.Signed("textures", "value", "signature"))),
                new UUID(1, 2))),
        new Sample(login, clientbound, "login_compression", new LoginCompressionPacket(256)),
        new Sample(login, serverbound, "hello", new HelloServerboundPacket("Notch", NOTCH)),
        new Sample(
            login,
            serverbound,
            "key",
            new KeyPacket(ByteBlob.of(Hex.ramp(128)), ByteBlob.of(Hex.filled(7, 128)))),
        new Sample(login, serverbound, "login_acknowledged", new LoginAcknowledgedPacket()),
        new Sample(config, clientbound, "disconnect", new DisconnectPacket(new StringTag("bye"))),
        new Sample(
            config,
            clientbound,
            "finish_configuration",
            new FinishConfigurationClientboundPacket()),
        new Sample(config, clientbound, "keep_alive", new KeepAliveClientboundPacket(-1L)),
        new Sample(config, clientbound, "ping", new PingPacket(-7)),
        new Sample(config, clientbound, "registry_data", registryData()),
        new Sample(config, clientbound, "update_tags", updateTags()),
        new Sample(
            config,
            serverbound,
            "finish_configuration",
            new FinishConfigurationServerboundPacket()),
        new Sample(config, serverbound, "keep_alive", new KeepAliveServerboundPacket(42L)),
        new Sample(config, serverbound, "pong", new PongPacket(99)));
  }

  /** A registry data packet with an entry with data and an entry without. */
  public static RegistryDataPacket registryData() {
    SequencedMap<String, ByteTag> entries = new LinkedHashMap<>();
    entries.put("x", new ByteTag((byte) 1));
    return new RegistryDataPacket(
        Identifier.of("minecraft", "dimension_type"),
        List.of(
            new RegistryEntry.WithData(
                Identifier.of("minecraft", "overworld"), CompoundTag.of(entries)),
            new RegistryEntry.WithoutData(Identifier.of("minecraft", "the_end"))));
  }

  /** An update tags packet with two registries. */
  public static UpdateTagsPacket updateTags() {
    return new UpdateTagsPacket(
        List.of(
            new RegistryTags(
                Identifier.of("minecraft", "block"),
                List.of(
                    new TagIds(Identifier.of("minecraft", "logs"), List.of(1, 300, 35722)),
                    new TagIds(Identifier.of("minecraft", "empty"), List.of()))),
            new RegistryTags(Identifier.of("minecraft", "item"), List.of())));
  }
}
