package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Allocation;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Buffers.Read;
import dev.shoko.minecraft.net.testing.Fuzz;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R8, R9, R14, R90 (adversarial): fixed seeds, typed failures only, and no
// allocation sized from an announced length
class PacketReaderAttackTest {

  private static final long ALLOCATION_BUDGET = 50_000;

  private record Op(String label, Read<?> read) {
    @Override
    public String toString() {
      return label;
    }
  }

  private static List<Op> operations() {
    return List.of(
        new Op("boolean", PacketReader::readBoolean),
        new Op("byte", PacketReader::readByte),
        new Op("short", PacketReader::readShort),
        new Op("int", PacketReader::readInt),
        new Op("long", PacketReader::readLong),
        new Op("float", PacketReader::readFloat),
        new Op("double", PacketReader::readDouble),
        new Op("varint", PacketReader::readVarInt),
        new Op("varlong", PacketReader::readVarLong),
        new Op("string(5)", in -> in.readString(5)),
        new Op("string(32767)", in -> in.readString(32767)),
        new Op("identifier", PacketReader::readIdentifier),
        new Op("uuid", PacketReader::readUuid),
        new Op("bytes(64)", in -> in.readPrefixedBytes(64)),
        new Op("bytes(max)", in -> in.readPrefixedBytes(Integer.MAX_VALUE)),
        new Op("remaining(8)", in -> in.readRemainingBytes(8)),
        new Op("position", PacketReader::readPosition),
        new Op("lpvec3", PacketReader::readLpVec3),
        new Op("bitset", PacketReader::readBitSet),
        new Op("fixed bitset", in -> in.readFixedBitSet(70)),
        new Op("nbt", PacketReader::readNbt),
        new Op("nbt or absent", PacketReader::readNbtOrAbsent),
        new Op(
            "array(max)", in -> in.readPrefixedArray(Integer.MAX_VALUE, PacketReader::readVarInt)),
        new Op("array(5) of strings", in -> in.readPrefixedArray(5, r -> r.readString(3))),
        new Op("optional", in -> in.readPrefixedOptional(PacketReader::readVarLong)));
  }

  @Test
  @Timeout(60)
  void randomBytesOnlyEverFailWithAProtocolException() {
    // spec: R90 (a decoder never fails with anything but a typed error)
    Random random = new Random(0x5EEDL);
    List<Op> operations = operations();
    for (int i = 0; i < 20_000; i++) {
      byte[] data = Fuzz.bytes(random, 24);
      Op op = operations.get(random.nextInt(operations.size()));
      PacketReader in = PacketReader.wrap(data);
      try {
        op.read().accept(in);
        assertThat(in.remaining()).as(op.label()).isBetween(0, data.length);
      } catch (ProtocolException typed) {
        assertThat(typed.reason()).as("%s on %s", op.label(), Hex.format(data)).isNotNull();
      }
    }
  }

  private record VarIntModel(Integer value, int consumed, Set<Reason> reasons) {}

  private static VarIntModel varIntModel(byte[] data) {
    int value = 0;
    for (int i = 0; i < 5; i++) {
      if (i >= data.length) {
        return new VarIntModel(null, 0, Set.of(Reason.UNEXPECTED_END));
      }
      int b = data[i] & 0xff;
      value |= (b & 0x7f) << (7 * i);
      if ((b & 0x80) == 0) {
        return new VarIntModel(value, i + 1, Set.of());
      }
    }
    if (data.length == 5) {
      // The fifth byte still has the continuation bit and nothing follows: the contract names
      // VAR_INT_TOO_BIG, the sixth byte is not there to be read; both readings are typed.
      return new VarIntModel(null, 0, Set.of(Reason.VAR_INT_TOO_BIG, Reason.UNEXPECTED_END));
    }
    return new VarIntModel(null, 0, Set.of(Reason.VAR_INT_TOO_BIG));
  }

  @Test
  @Timeout(60)
  void readVarIntFollowsTheReferenceModelOnRandomBytes() throws ProtocolException {
    // spec: R6, R8, vectors N20 to N22 (overlong accepted, 6 bytes refused, high bits dropped)
    Random random = new Random(0xBA5EL);
    for (int i = 0; i < 20_000; i++) {
      byte[] data = Fuzz.bytes(random, 8);
      VarIntModel model = varIntModel(data);
      PacketReader in = PacketReader.wrap(data);
      if (model.value() != null) {
        assertThat(in.readVarInt()).as(Hex.format(data)).isEqualTo(model.value());
        assertThat(data.length - in.remaining()).isEqualTo(model.consumed());
      } else {
        ProtocolException failure = failure(in::readVarInt);
        assertThat(failure.reason()).as(Hex.format(data)).isIn(model.reasons());
      }
    }
  }

  @Test
  @Timeout(60)
  void varIntAndVarLongRoundTripOnRandomValues() throws ProtocolException {
    // spec: test plan L1 (round trip for any VarInt and VarLong, pseudo-random fixed seed)
    Random random = new Random(0x7A1L);
    for (int i = 0; i < 20_000; i++) {
      int intValue = random.nextInt() >> random.nextInt(32);
      long longValue = random.nextLong() >> random.nextInt(64);
      PacketWriter out = PacketWriter.create();
      out.writeVarInt(intValue);
      out.writeVarLong(longValue);
      PacketReader in = PacketReader.wrap(out.toByteArray());
      assertThat(in.readVarInt()).isEqualTo(intValue);
      assertThat(in.readVarLong()).isEqualTo(longValue);
      in.requireFullyConsumed();
    }
  }

  private static void assertBoundedAllocation(String label, String hex, Read<?> read) {
    // Warm up so that class loading is not counted, then measure.
    Allocation.bytesAllocatedBy(() -> read.accept(Buffers.reader(hex)));
    long allocated = Allocation.bytesAllocatedBy(() -> read.accept(Buffers.reader(hex)));
    assertThat(allocated).as(label).isLessThan(ALLOCATION_BUDGET);
    ProtocolException failure = failure(() -> read.accept(Buffers.reader(hex)));
    assertThat(failure.reason()).as(label).isNotNull();
  }

  @Test
  @Timeout(60)
  void provisionalTinyInputsAnnouncingHugeLengthsAllocateNothingProportional() {
    // spec: R90 (no allocation depends on an announced length not compared with the bytes left)
    assertBoundedAllocation("string, 2^31-1 bytes", "ff ff ff ff 07", in -> in.readString(32767));
    assertBoundedAllocation(
        "string, 98304 bytes announced, none present", "80 80 06", in -> in.readString(32767));
    assertBoundedAllocation(
        "bytes, 2^31-1", "ff ff ff ff 07", in -> in.readPrefixedBytes(Integer.MAX_VALUE));
    assertBoundedAllocation(
        "bytes, 1000000 announced, none present",
        "c0 84 3d",
        in -> in.readPrefixedBytes(Integer.MAX_VALUE));
    assertBoundedAllocation(
        "array, 2^31-1",
        "ff ff ff ff 07",
        in -> in.readPrefixedArray(Integer.MAX_VALUE, PacketReader::readByte));
    assertBoundedAllocation(
        "array, 1000000 announced, none present",
        "c0 84 3d",
        in -> in.readPrefixedArray(Integer.MAX_VALUE, PacketReader::readVarInt));
    assertBoundedAllocation("bit set, 2^31-1 longs", "ff ff ff ff 07", PacketReader::readBitSet);
    assertBoundedAllocation(
        "bit set, 1000000 longs announced", "c0 84 3d", PacketReader::readBitSet);
  }

  @Test
  @Timeout(60)
  void provisionalAListOfTwoBillionBytesInNbtIsRefusedWithoutAllocatingIt() {
    // spec: R90 (NBT quota of core-nbt R32 and R60, checked at the list header)
    assertBoundedAllocation("nbt list of 2^31-1 bytes", "09 01 7f ff ff ff", PacketReader::readNbt);
    assertBoundedAllocation(
        "nbt byte array of 2^31-1", "0a 07 00 01 61 7f ff ff ff", PacketReader::readNbtOrAbsent);
  }
}
