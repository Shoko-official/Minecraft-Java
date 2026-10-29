package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.registry.PacketRegistry;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Allocation;
import dev.shoko.minecraft.net.testing.Fuzz;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Samples.Sample;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R49, R90 (adversarial): fixed seeds, typed failures only (a protocol
// error, or an illegal argument for a packet outside the first batch), no allocation sized from an
// announced length
class PacketCodecsAttackTest {

  private static final long ALLOCATION_BUDGET = 50_000;

  /**
   * Decodes and, when it succeeds, checks that the packet survives a second round trip. Returns
   * quietly on a typed failure. Anything else fails the test.
   */
  private static void decodeSafely(ConnectionState state, PacketDirection direction, Frame frame) {
    try {
      Packet packet = PacketCodecs.decode(state, direction, frame);
      Frame again = PacketCodecs.encode(packet);
      assertThat(PacketCodecs.decode(state, direction, again)).isEqualTo(packet);
    } catch (ProtocolException typed) {
      assertThat(typed.reason()).isNotNull();
    }
  }

  @Test
  @Timeout(60)
  void randomFramesOnlyEverFailWithTypedErrors() {
    // spec: R90 (a decoder never fails with anything but a protocol error), R49
    Random random = new Random(0xF4A3L);
    ConnectionState[] states = ConnectionState.values();
    PacketDirection[] directions = PacketDirection.values();
    for (int i = 0; i < 30_000; i++) {
      ConnectionState state = states[random.nextInt(states.length)];
      PacketDirection direction = directions[random.nextInt(directions.length)];
      int size = PacketRegistry.all(state, direction).size();
      int id = random.nextInt(size + 3);
      decodeSafely(state, direction, Frame.of(id, Fuzz.bytes(random, 40)));
    }
  }

  @Test
  @Timeout(60)
  void mutatedValidPayloadsOnlyEverFailWithTypedErrors() throws ProtocolException {
    // spec: R90 (bit flips, truncations and extra bytes on valid payloads)
    Random random = new Random(0xBADF00DL);
    List<Sample> samples = Samples.all();
    for (int i = 0; i < 30_000; i++) {
      Sample sample = samples.get(random.nextInt(samples.size()));
      byte[] payload = PacketCodecs.encode(sample.packet()).payload();
      Frame mutated = Frame.of(sample.packet().type().id(), Fuzz.mutate(random, payload));
      decodeSafely(sample.state(), sample.direction(), mutated);
    }
  }

  private static void assertTinyAndTyped(
      String label, ConnectionState state, PacketDirection direction, int id, byte[] payload) {
    Frame frame = Frame.of(id, payload);
    Allocation.bytesAllocatedBy(() -> PacketCodecs.decode(state, direction, frame));
    long allocated =
        Allocation.bytesAllocatedBy(() -> PacketCodecs.decode(state, direction, frame));
    assertThat(allocated).as(label).isLessThan(ALLOCATION_BUDGET);
    assertThat(failure(() -> PacketCodecs.decode(state, direction, frame)).reason())
        .as(label)
        .isNotNull();
  }

  @Test
  @Timeout(60)
  void provisionalTinyPayloadsAnnouncingHugeLengthsAllocateNothingProportional() {
    // spec: R90 (no allocation depends on an announced length not compared with the bytes left)
    byte[] huge = Hex.bytes("ff ff ff ff 07");
    ConnectionState login = ConnectionState.LOGIN;
    ConnectionState config = ConnectionState.CONFIGURATION;
    PacketDirection clientbound = PacketDirection.CLIENTBOUND;
    PacketDirection serverbound = PacketDirection.SERVERBOUND;
    assertTinyAndTyped("status_response string", ConnectionState.STATUS, clientbound, 0, huge);
    assertTinyAndTyped("login_disconnect string", login, clientbound, 0, huge);
    assertTinyAndTyped("hello name", login, serverbound, 0, huge);
    assertTinyAndTyped("hello key bytes", login, clientbound, 1, Hex.concat(Hex.bytes("00"), huge));
    assertTinyAndTyped("key secret bytes", login, serverbound, 1, huge);
    assertTinyAndTyped(
        "login_finished properties",
        login,
        clientbound,
        2,
        Hex.concat(Wire.uuid(Samples.NOTCH), Wire.string("Notch"), huge));
    assertTinyAndTyped(
        "registry_data entries",
        config,
        clientbound,
        7,
        Hex.concat(Wire.string("minecraft:chat_type"), huge));
    assertTinyAndTyped("update_tags registries", config, clientbound, 14, huge);
    assertTinyAndTyped(
        "update_tags tag ids",
        config,
        clientbound,
        14,
        Hex.concat(
            Wire.varInt(1),
            Wire.string("minecraft:block"),
            Wire.varInt(1),
            Wire.string("minecraft:logs"),
            huge));
    assertTinyAndTyped(
        "disconnect nbt list", config, clientbound, 2, Hex.bytes("09 01 7f ff ff ff"));
  }
}
