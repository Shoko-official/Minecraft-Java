package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R17 (network NBT: a type byte then the payload, no root name, absent is
// 00)
class PacketNbtTest {

  private static CompoundTag compound(Object... keysAndValues) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      entries.put((String) keysAndValues[i], (NbtTag) keysAndValues[i + 1]);
    }
    return CompoundTag.of(entries);
  }

  @Test
  void aTextComponentThatIsPlainTextIsAStringTagWithoutRootName() throws ProtocolException {
    // spec: R17 (a String tag when the component is plain text), core-nbt R20 (no root name)
    byte[] written = Buffers.write(out -> out.writeNbt(new StringTag("hi")));
    assertThat(Hex.format(written)).isEqualTo("08 00 02 68 69");
    assertThat(Buffers.readAll(written, PacketReader::readNbt)).isEqualTo(new StringTag("hi"));
  }

  @Test
  void anEmptyCompoundIsTheCompoundTypeThenEnd() throws ProtocolException {
    // spec: R17, core-nbt R20 (no "00 00" name after 0a)
    byte[] written = Buffers.write(out -> out.writeNbt(CompoundTag.empty()));
    assertThat(Hex.format(written)).isEqualTo("0a 00");
    assertThat(Buffers.readAll(written, PacketReader::readNbt)).isEqualTo(CompoundTag.empty());
  }

  @Test
  void aCompoundKeepsItsEntriesInOrder() throws ProtocolException {
    // spec: R17 (a Compound for a styled component)
    CompoundTag tag = compound("a", new ByteTag((byte) 42), "text", new StringTag("x"));
    byte[] written = Buffers.write(out -> out.writeNbt(tag));
    assertThat(Hex.format(written)).isEqualTo("0a 01 00 01 61 2a 08 00 04 74 65 78 74 00 01 78 00");
    assertThat(Buffers.readAll(written, PacketReader::readNbt)).isEqualTo(tag);
  }

  @Test
  void numbersAndListsRoundTrip() throws ProtocolException {
    // spec: R17
    NbtTag tag =
        compound(
            "n",
            new IntTag(-7),
            "l",
            ListTag.of(List.of(new IntTag(1), new IntTag(2))),
            "s",
            new StringTag(""));
    assertThat(Buffers.readAll(Buffers.write(out -> out.writeNbt(tag)), PacketReader::readNbt))
        .isEqualTo(tag);
    assertThat(Hex.format(Buffers.write(out -> out.writeNbt(new IntTag(5)))))
        .isEqualTo("03 00 00 00 05");
  }

  @Test
  void anAbsentValueIsTheEndByte() throws ProtocolException {
    // spec: R17 (an absent NBT is the byte 00, tag End)
    assertThat(Hex.format(Buffers.write(PacketWriter::writeAbsentNbt))).isEqualTo("00");
    assertThat(Buffers.readAll("00", PacketReader::readNbtOrAbsent)).isEqualTo(Optional.empty());
  }

  @Test
  void readNbtOrAbsentReadsAPresentValue() throws ProtocolException {
    // spec: R17
    assertThat(Buffers.readAll("08 00 02 68 69", PacketReader::readNbtOrAbsent))
        .isEqualTo(Optional.of(new StringTag("hi")));
  }

  @Test
  void provisionalTheEndByteAloneIsNotAValueForReadNbt() {
    // spec: R17, API contract of readNbt (INVALID_NBT for the End byte alone)
    ProtocolException failure = failure(() -> Buffers.reader("00").readNbt());
    assertThat(failure.reason()).isEqualTo(Reason.INVALID_NBT);
  }

  @Test
  void provisionalAnUnknownTagTypeIsInvalidNbtCausedByTheNbtException() {
    // spec: R17, API contract of readNbt and readNbtOrAbsent
    ProtocolException failure = failure(() -> Buffers.reader("7f 00 00").readNbt());
    assertThat(failure.reason()).isEqualTo(Reason.INVALID_NBT);
    assertThat(failure.getCause()).isInstanceOf(NbtException.class);
    ProtocolException orAbsent = failure(() -> Buffers.reader("7f 00 00").readNbtOrAbsent());
    assertThat(orAbsent.reason()).isEqualTo(Reason.INVALID_NBT);
    assertThat(orAbsent.getCause()).isInstanceOf(NbtException.class);
  }

  @Test
  void readNbtLeavesTheBytesAfterTheValue() throws ProtocolException {
    // spec: R17, R90
    PacketReader in = Buffers.reader("08 00 02 68 69 aa bb");
    assertThat(in.readNbt()).isEqualTo(new StringTag("hi"));
    assertThat(in.remaining()).isEqualTo(2);
  }
}
