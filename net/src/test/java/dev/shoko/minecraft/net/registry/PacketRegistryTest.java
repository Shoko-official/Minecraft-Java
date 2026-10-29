package dev.shoko.minecraft.net.registry;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Golden;
import dev.shoko.minecraft.net.testing.Golden.PacketRow;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// spec: net-protocol-777 R2 (260 packets, counts per state and direction, contiguous ids), R49
// (unknown id), tables of the section "Paquets" (golden/packets-777.txt), vector I06
class PacketRegistryTest {

  private record Pair(ConnectionState state, PacketDirection direction, int count) {
    @Override
    public String toString() {
      return state + " " + direction + " " + count;
    }
  }

  private static List<Pair> pairs() {
    return List.of(
        new Pair(ConnectionState.HANDSHAKE, PacketDirection.CLIENTBOUND, 0),
        new Pair(ConnectionState.HANDSHAKE, PacketDirection.SERVERBOUND, 1),
        new Pair(ConnectionState.STATUS, PacketDirection.CLIENTBOUND, 2),
        new Pair(ConnectionState.STATUS, PacketDirection.SERVERBOUND, 2),
        new Pair(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 6),
        new Pair(ConnectionState.LOGIN, PacketDirection.SERVERBOUND, 5),
        new Pair(ConnectionState.CONFIGURATION, PacketDirection.CLIENTBOUND, 21),
        new Pair(ConnectionState.CONFIGURATION, PacketDirection.SERVERBOUND, 10),
        new Pair(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, 144),
        new Pair(ConnectionState.PLAY, PacketDirection.SERVERBOUND, 69));
  }

  static Stream<Pair> pairStream() {
    return pairs().stream();
  }

  static Stream<PacketRow> rows() {
    return Golden.packets().stream();
  }

  @Test
  void theGoldenListHoldsTheTwoHundredAndSixtyPackets() {
    // spec: R2
    assertThat(Golden.packets()).hasSize(260);
    assertThat(pairs().stream().mapToInt(Pair::count).sum()).isEqualTo(260);
  }

  @ParameterizedTest
  @MethodSource("pairStream")
  void eachPairHasTheCountOfTheSpec(Pair pair) {
    // spec: R2 (1; 2 and 2; 6 and 5; 21 and 10; 144 and 69)
    assertThat(PacketRegistry.all(pair.state(), pair.direction())).hasSize(pair.count());
  }

  @Test
  void theTotalOverTheNinePairsIsTwoHundredAndSixty() {
    // spec: R2
    int total = 0;
    for (ConnectionState state : ConnectionState.values()) {
      for (PacketDirection direction : PacketDirection.values()) {
        total += PacketRegistry.all(state, direction).size();
      }
    }
    assertThat(total).isEqualTo(260);
  }

  @ParameterizedTest
  @MethodSource("pairStream")
  void idsAreContiguousFromZeroAndOrderedInEachPair(Pair pair) {
    // spec: R2 ("les identifiants sont contigus de 0 a n-1 dans chaque couple")
    List<PacketType> all = PacketRegistry.all(pair.state(), pair.direction());
    for (int index = 0; index < all.size(); index++) {
      PacketType type = all.get(index);
      assertThat(type.id()).as("position %d", index).isEqualTo(index);
      assertThat(type.state()).isEqualTo(pair.state());
      assertThat(type.direction()).isEqualTo(pair.direction());
    }
  }

  @ParameterizedTest
  @MethodSource("pairStream")
  void namesAreDistinctWithinAPair(Pair pair) {
    // spec: R2, API contract of byName (a name designates one packet of a pair)
    Set<String> names = new HashSet<>();
    for (PacketType type : PacketRegistry.all(pair.state(), pair.direction())) {
      assertThat(names.add(type.packetName())).as(type.packetName()).isTrue();
    }
  }

  @ParameterizedTest
  @MethodSource("rows")
  void everyPacketOfTheSpecTableHasItsIdAndName(PacketRow row) throws ProtocolException {
    // spec: R2, R4 (ids and names are extracted data), tables of the section "Paquets"
    PacketType type = PacketRegistry.lookup(row.state(), row.direction(), row.id());
    assertThat(type.packetName()).isEqualTo(row.name());
    assertThat(type.id()).isEqualTo(row.id());
    assertThat(type.state()).isEqualTo(row.state());
    assertThat(type.direction()).isEqualTo(row.direction());
  }

  @ParameterizedTest
  @MethodSource("rows")
  void byNameFindsTheSamePacketAsLookup(PacketRow row) throws ProtocolException {
    // spec: API contract of byName, round trip of the registry
    Optional<PacketType> byName = PacketRegistry.byName(row.state(), row.direction(), row.name());
    assertThat(byName).isPresent();
    assertThat(byName.orElseThrow())
        .isEqualTo(PacketRegistry.lookup(row.state(), row.direction(), row.id()));
    assertThat(PacketRegistry.all(row.state(), row.direction()).get(row.id()))
        .isEqualTo(byName.orElseThrow());
  }

  @Test
  void theClientboundHandshakeHasNoPacket() {
    // spec: R2 (handshake 1 serverbound), API contract of lookup and all
    assertThat(PacketRegistry.all(ConnectionState.HANDSHAKE, PacketDirection.CLIENTBOUND))
        .isEmpty();
    for (int id : new int[] {0, 1, 5, 255}) {
      assertFails(
          () -> PacketRegistry.lookup(ConnectionState.HANDSHAKE, PacketDirection.CLIENTBOUND, id),
          Reason.UNKNOWN_PACKET_ID);
    }
    assertThat(
            PacketRegistry.byName(
                ConnectionState.HANDSHAKE, PacketDirection.CLIENTBOUND, "intention"))
        .isEmpty();
  }

  @Test
  void provisionalAnIdOutsideTheTableIsUnknown() {
    // spec: R49, R2 (an identifier outside the table is refused), vector I06 (id 5 in handshake)
    assertFails(
        () -> PacketRegistry.lookup(ConnectionState.HANDSHAKE, PacketDirection.SERVERBOUND, 5),
        Reason.UNKNOWN_PACKET_ID);
    for (Pair pair : pairs()) {
      for (int id :
          new int[] {
            pair.count(), pair.count() + 1, 255, Integer.MAX_VALUE, -1, Integer.MIN_VALUE
          }) {
        assertFails(
            () -> PacketRegistry.lookup(pair.state(), pair.direction(), id),
            Reason.UNKNOWN_PACKET_ID);
      }
    }
  }

  @Test
  void theSameIdMeansDifferentPacketsInDifferentPairs() throws ProtocolException {
    // spec: R2 (ids are numbered separately for each direction and state)
    assertThat(
            PacketRegistry.lookup(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 0)
                .packetName())
        .isEqualTo("login_disconnect");
    assertThat(
            PacketRegistry.lookup(ConnectionState.LOGIN, PacketDirection.SERVERBOUND, 0)
                .packetName())
        .isEqualTo("hello");
    assertThat(
            PacketRegistry.lookup(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 1)
                .packetName())
        .isEqualTo("hello");
    assertThat(
            PacketRegistry.lookup(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 3)
                .packetName())
        .isEqualTo("login_compression");
    assertThat(
            PacketRegistry.lookup(ConnectionState.LOGIN, PacketDirection.SERVERBOUND, 3)
                .packetName())
        .isEqualTo("login_acknowledged");
  }

  @Test
  void theIdsThatTheSpecQuotesByNumberAreThose() throws ProtocolException {
    // spec: R4 (level_chunk_with_light is 46, not 0x2C), R45 (play 16 and 120), vector CT01 (0x10)
    assertThat(lookupName(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, 46))
        .isEqualTo("level_chunk_with_light");
    assertThat(lookupName(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, 120))
        .isEqualTo("start_configuration");
    assertThat(lookupName(ConnectionState.PLAY, PacketDirection.SERVERBOUND, 16))
        .isEqualTo("configuration_acknowledged");
    assertThat(lookupName(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, 16))
        .isEqualTo("commands");
    assertThat(lookupName(ConnectionState.CONFIGURATION, PacketDirection.SERVERBOUND, 3))
        .isEqualTo("finish_configuration");
  }

  private static String lookupName(ConnectionState state, PacketDirection direction, int id)
      throws ProtocolException {
    return PacketRegistry.lookup(state, direction, id).packetName();
  }

  @Test
  void byNameRefusesUnknownNamesAndNamesOfAnotherPair() {
    // spec: API contract of byName (empty when the pair has no packet of this name)
    assertThat(
            PacketRegistry.byName(ConnectionState.LOGIN, PacketDirection.SERVERBOUND, "intention"))
        .isEmpty();
    assertThat(
            PacketRegistry.byName(
                ConnectionState.LOGIN, PacketDirection.SERVERBOUND, "login_finished"))
        .isEmpty();
    assertThat(PacketRegistry.byName(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, ""))
        .isEmpty();
    assertThat(
            PacketRegistry.byName(
                ConnectionState.PLAY, PacketDirection.CLIENTBOUND, "no_such_packet"))
        .isEmpty();
    assertThat(PacketRegistry.byName(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, "Login"))
        .isEmpty();
  }

  @Test
  void theRegistryIsDeterministic() {
    // spec: ARCHITECTURE determinism (same call, same answer)
    for (Pair pair : pairs()) {
      assertThat(PacketRegistry.all(pair.state(), pair.direction()))
          .isEqualTo(PacketRegistry.all(pair.state(), pair.direction()));
    }
  }
}
