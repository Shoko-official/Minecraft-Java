package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: net-protocol-777 R12 (Position, vectors P01 to P06), R18 (LpVec3, vectors L01 to L05)
class PacketPositionTest {

  @ParameterizedTest
  @CsvSource({
    "18357644, 831, -20882616, 46 07 63 2c 15 b4 83 3f",
    "0, 0, 0, 00 00 00 00 00 00 00 00",
    "-1, -1, -1, ff ff ff ff ff ff ff ff",
    "33554431, 2047, 33554431, 7f ff ff df ff ff f7 ff",
    "-33554432, -2048, -33554432, 80 00 00 20 00 00 08 00",
    "100, 64, -100, 00 00 19 3f ff f9 c0 40"
  })
  void positionsMatchTheVectorsInBothDirections(int x, int y, int z, String hex)
      throws ProtocolException {
    // spec: R12, vectors P01 to P06 (P01 is the example of the public documentation)
    BlockPos pos = new BlockPos(x, y, z);
    assertThat(Hex.format(Buffers.write(out -> out.writePosition(pos)))).isEqualTo(hex);
    assertThat(Buffers.readAll(hex, PacketReader::readPosition)).isEqualTo(pos);
  }

  @Test
  void positionFieldsAreSignExtendedOnTheirWidth() throws ProtocolException {
    // spec: R12 (x and z on 26 bits, y on 12 bits, all signed)
    assertThat(Buffers.readAll("00 00 00 00 00 00 08 00", PacketReader::readPosition))
        .isEqualTo(new BlockPos(0, -2048, 0));
    assertThat(Buffers.readAll("00 00 00 00 00 00 07 ff", PacketReader::readPosition))
        .isEqualTo(new BlockPos(0, 2047, 0));
    assertThat(Buffers.readAll("00 00 00 00 00 00 00 01", PacketReader::readPosition))
        .isEqualTo(new BlockPos(0, 1, 0));
    assertThat(Buffers.readAll("80 00 00 00 00 00 00 00", PacketReader::readPosition))
        .isEqualTo(new BlockPos(-33554432, 0, 0));
    assertThat(Buffers.readAll("00 00 00 20 00 00 00 00", PacketReader::readPosition))
        .isEqualTo(new BlockPos(0, 0, -33554432));
  }

  @Test
  void provisionalAPositionOutsideTheFieldsIsRefusedOnWrite() throws ProtocolException {
    // spec: R12 (ranges: x and z from -33554432 to 33554431, y from -2048 to 2047)
    for (BlockPos outside :
        new BlockPos[] {
          new BlockPos(33554432, 0, 0),
          new BlockPos(-33554433, 0, 0),
          new BlockPos(0, 0, 33554432),
          new BlockPos(0, 0, -33554433),
          new BlockPos(0, 2048, 0),
          new BlockPos(0, -2049, 0),
          new BlockPos(Integer.MAX_VALUE, 0, Integer.MIN_VALUE)
        }) {
      assertFails(
          () -> Buffers.write(out -> out.writePosition(outside)), Reason.VALUE_OUT_OF_RANGE);
    }
  }

  @Test
  void lpVec3ZeroIsASingleZeroByte() throws ProtocolException {
    // spec: R18, vector L01
    assertThat(Hex.format(Buffers.write(out -> out.writeLpVec3(new Vec3(0, 0, 0)))))
        .isEqualTo("00");
    assertThat(Buffers.readAll("00", PacketReader::readLpVec3)).isEqualTo(new Vec3(0, 0, 0));
  }

  @Test
  void provisionalAVectorBelowOneOver32766IsTheZeroVector() throws ProtocolException {
    // spec: R18 (null when the maximum component is below 1/32766)
    assertThat(Hex.format(Buffers.write(out -> out.writeLpVec3(new Vec3(1e-6, -1e-6, 0)))))
        .isEqualTo("00");
  }

  @ParameterizedTest
  @CsvSource({
    "1.0, 0.0, -1.0, f1 ff 00 00 ff ff",
    "10.0, 0.2, -5.0, f6 ff 40 01 05 1f 02",
    "123457.0, 15.071, 0.0, f5 ff 7f ff 00 07 90 f1 01",
    "0.1, -0.2, 0.3, 29 33 a6 64 cc ca",
    "2.0, -2.0, 0.0, f2 ff 7f fe 00 03",
    "-1.0, -1.0, -1.0, 01 00 00 00 00 00",
    "4.0, 0.0, 0.0, f4 ff 7f fe ff ff 01",
    "5.0, -5.0, 5.0, f5 ff ff fc 00 03 01"
  })
  void provisionalLpVec3EncodingMatchesTheVectors(double x, double y, double z, String hex)
      throws ProtocolException {
    // spec: R18, vectors L02 to L05 (L02 to L04 are the examples of the public documentation);
    // the last three rows are computed with the reference script, scales 2, 4 and 5
    Vec3 vector = new Vec3(x, y, z);
    assertThat(Hex.format(Buffers.write(out -> out.writeLpVec3(vector)))).isEqualTo(hex);
  }

  @ParameterizedTest
  @CsvSource({
    "f1 ff 00 00 ff ff, 1.0, 0.0, -1.0, 1",
    "f6 ff 40 01 05 1f 02, 10.0, 0.2, -5.0, 10",
    "f5 ff 7f ff 00 07 90 f1 01, 123457.0, 15.071, 0.0, 123457",
    "29 33 a6 64 cc ca, 0.1, -0.2, 0.3, 1",
    "f4 ff 7f fe ff ff 01, 4.0, 0.0, 0.0, 4"
  })
  void provisionalLpVec3DecodingIsWithinOneThousandthOfTheScale(
      String hex, double x, double y, double z, int scale) throws ProtocolException {
    // spec: R18 (decoding is compared within 1e-3 * s: quantization on 15 bits), vectors L02 to L05
    Vec3 decoded = Buffers.readAll(hex, PacketReader::readLpVec3);
    double tolerance = 1e-3 * scale;
    assertThat(decoded.x()).isCloseTo(x, within(tolerance));
    assertThat(decoded.y()).isCloseTo(y, within(tolerance));
    assertThat(decoded.z()).isCloseTo(z, within(tolerance));
  }

  @Test
  void provisionalLpVec3ExactVectorsDecodeExactly() throws ProtocolException {
    // spec: R18, vectors L02 and L01 (decode (1.0, 0.0, -1.0))
    Vec3 decoded = Buffers.readAll("f1 ff 00 00 ff ff", PacketReader::readLpVec3);
    assertThat(decoded.x()).isCloseTo(1.0, within(1e-9));
    assertThat(decoded.y()).isCloseTo(0.0, within(1e-9));
    assertThat(decoded.z()).isCloseTo(-1.0, within(1e-9));
  }

  @Test
  void provisionalLpVec3RoundTripStaysWithinTheQuantization() throws ProtocolException {
    // spec: R18 (a component is quantized on 15 bits of the range -s to s)
    Vec3 vector = new Vec3(0.75, -0.33, 0.1);
    byte[] bytes = Buffers.write(out -> out.writeLpVec3(vector));
    assertThat(bytes).hasSize(6);
    Vec3 back = Buffers.readAll(bytes, PacketReader::readLpVec3);
    assertThat(back.x()).isCloseTo(vector.x(), within(1e-3));
    assertThat(back.y()).isCloseTo(vector.y(), within(1e-3));
    assertThat(back.z()).isCloseTo(vector.z(), within(1e-3));
  }

  @Test
  void lpVec3TakesAVarIntOnlyWhenTheScaleExceedsThree() throws ProtocolException {
    // spec: R18 (6 bytes plus a VarInt when the scale is above 3)
    assertThat(Buffers.write(out -> out.writeLpVec3(new Vec3(3, 0, 0)))).hasSize(6);
    assertThat(Buffers.write(out -> out.writeLpVec3(new Vec3(4, 0, 0)))).hasSize(7);
    assertThat(Buffers.write(out -> out.writeLpVec3(new Vec3(123457, 15.071, 0)))).hasSize(9);
  }

  @Test
  void lpVec3ReadsOnlyItsBytes() throws ProtocolException {
    // spec: R18 (consumed 1 of 1, 6 of 6, 7 of 7, 9 of 9 for the documented bytes)
    String[] hexes = {
      "00", "f1 ff 00 00 ff ff", "f6 ff 40 01 05 1f 02", "f5 ff 7f ff 00 07 90 f1 01"
    };
    for (String hex : hexes) {
      PacketReader in = Buffers.reader(hex + " aa");
      in.readLpVec3();
      assertThat(in.remaining()).as(hex).isEqualTo(1);
    }
  }

  @Test
  void lpVec3TruncatedInputIsAnUnexpectedEnd() {
    // spec: R18, R90
    assertFails(() -> Buffers.reader("").readLpVec3(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("f1 ff 00 00 ff").readLpVec3(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("f5 ff 7f ff 00 07").readLpVec3(), Reason.UNEXPECTED_END);
  }

  @Test
  void lpVec3ContinuationVarIntTooBigIsRefused() {
    // spec: R18, R8 (the scale VarInt follows the VarInt rules)
    assertFails(
        () -> Buffers.reader("f5 ff 7f ff 00 07 ff ff ff ff ff ff").readLpVec3(),
        Reason.VAR_INT_TOO_BIG);
  }

  @Test
  void lpVec3NotANumberOrInfiniteComponentsAreRefusedOnWrite() {
    // spec: API contract of writeLpVec3
    for (double bad :
        new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
      assertFails(
          () -> Buffers.write(out -> out.writeLpVec3(new Vec3(bad, 0, 0))),
          Reason.VALUE_OUT_OF_RANGE);
      assertFails(
          () -> Buffers.write(out -> out.writeLpVec3(new Vec3(0, 1, bad))),
          Reason.VALUE_OUT_OF_RANGE);
    }
  }
}
