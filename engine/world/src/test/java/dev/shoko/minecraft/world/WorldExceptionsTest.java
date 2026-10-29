package dev.shoko.minecraft.world;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.block.BlockReportException;
import dev.shoko.minecraft.world.chunk.ChunkFormatException;
import dev.shoko.minecraft.world.palette.PaletteFormatException;
import dev.shoko.minecraft.world.storage.CorruptStreamException;
import dev.shoko.minecraft.world.storage.OutputLimitExceededException;
import dev.shoko.minecraft.world.storage.WriteOnRefusedPositionException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** The exception types of the module keep what they are given and have the declared supertypes. */
class WorldExceptionsTest {

  @Test
  void blockReportFaultOfTheWholeReportHasNoBlock() {
    // spec: BlockReportException.blockId contract (empty for a fault of the whole report)
    BlockReportException fault = new BlockReportException("35722 states instead of 35723");

    assertThat(fault.blockId()).isEmpty();
    assertThat(fault.getMessage()).isEqualTo("35722 states instead of 35723");
  }

  @Test
  void blockReportFaultOfOneBlockNamesIt() {
    // spec: BlockReportException (the fault names the block)
    BlockReportException fault =
        new BlockReportException("minecraft:chest", "state id 5551 contradicts the formula");

    assertThat(fault.blockId()).contains("minecraft:chest");
    assertThat(fault.getMessage()).isEqualTo("state id 5551 contradicts the formula");
    assertThat(fault).isInstanceOf(Exception.class);
  }

  @Test
  void paletteFormatFaultKeepsItsMessage() {
    // spec: PaletteFormatException (the message names the faulty element)
    PaletteFormatException fault = new PaletteFormatException("data has 255 longs, 256 expected");

    assertThat(fault).hasMessage("data has 255 longs, 256 expected");
  }

  @Test
  void chunkFormatFaultKeepsReasonPathMessageAndCause() {
    // spec: ChunkFormatException contract
    IOException cause = new IOException("boom");
    ChunkFormatException plain =
        new ChunkFormatException(
            ChunkFormatException.Reason.MISSING_FIELD, "Sections.-4.Records", "no records");
    ChunkFormatException caused =
        new ChunkFormatException(
            ChunkFormatException.Reason.WRONG_TYPE, "Position", "not an int array", cause);

    assertThat(plain.reason()).isEqualTo(ChunkFormatException.Reason.MISSING_FIELD);
    assertThat(plain.path()).isEqualTo("Sections.-4.Records");
    assertThat(plain.getMessage()).isEqualTo("no records");
    assertThat(caused.reason()).isEqualTo(ChunkFormatException.Reason.WRONG_TYPE);
    assertThat(caused.path()).isEqualTo("Position");
    assertThat(caused.getCause()).isSameAs(cause);
  }

  @Test
  void corruptStreamFaultKeepsMessageAndCause() {
    // spec: CorruptStreamException (not an input or output failure)
    IOException cause = new IOException("bad block");

    CorruptStreamException fault = new CorruptStreamException("bad trailer", cause);

    assertThat(fault).hasMessage("bad trailer").hasCause(cause);
    assertThat(new CorruptStreamException("only a message").getCause()).isNull();
    assertThat(fault).isNotInstanceOf(IOException.class);
  }

  @Test
  void outputLimitFaultKeepsTheLimit() {
    // spec: OutputLimitExceededException.limit contract
    OutputLimitExceededException fault = new OutputLimitExceededException(134_217_728L);

    assertThat(fault.limit()).isEqualTo(134_217_728L);
    assertThat(fault.getMessage()).isNotBlank();
    assertThat(fault).isNotInstanceOf(IOException.class);
  }

  @Test
  void writeOnRefusedPositionIsAnUncheckedStateError() {
    // spec: AR-R28 (an unchecked error, a write error and not a read cause)
    WriteOnRefusedPositionException fault =
        new WriteOnRefusedPositionException("r.-1.-1.mca index 1023 refused: TOO_LARGE");

    assertThat(fault)
        .isInstanceOf(IllegalStateException.class)
        .isInstanceOf(RuntimeException.class);
    assertThat(fault).hasMessage("r.-1.-1.mca index 1023 refused: TOO_LARGE");
  }

  @Test
  void causesOfTheSpecAreClosedLists() {
    // spec: AR-R26 (absent: 5 causes, refused: 9 causes; anything else is a defect of the spec)
    assertThat(AbsentCause.values())
        .extracting(Enum::name)
        .containsExactlyInAnyOrder(
            "NO_REGION_FILE",
            "EMPTY_SLOT",
            "INVALID_LOCATION",
            "UNREADABLE_RECORD",
            "POSITION_MISMATCH");
    assertThat(RefusedCause.values())
        .extracting(Enum::name)
        .containsExactlyInAnyOrder(
            "UNSUPPORTED_COMPRESSION",
            "MISSING_EXTERNAL_FILE",
            "TOO_LARGE",
            "TOO_DEEP",
            "UNSUPPORTED_VERSION",
            "UNSUPPORTED_STATUS",
            "INVALID_CONTAINER",
            "INVALID_SECTION",
            "INVALID_FIELD");
  }

  @Test
  void readResultIsOneOfThreeIssues() {
    // spec: AR-R26 (exactly three exclusive issues)
    ReadResult<String> present = new ReadResult.Present<>("x", java.util.List.of());
    ReadResult<String> absent =
        new ReadResult.Absent<>(AbsentCause.EMPTY_SLOT, java.util.List.of());
    ReadResult<String> refused = new ReadResult.Refused<>(RefusedCause.TOO_LARGE, "too large");

    assertThat(present).isInstanceOf(ReadResult.Present.class);
    assertThat(absent).isInstanceOf(ReadResult.Absent.class);
    assertThat(refused).isInstanceOf(ReadResult.Refused.class);
    assertThat(new ReadWarning(WarningCode.UNKNOWN_BLOCK, "m").code())
        .isEqualTo(WarningCode.UNKNOWN_BLOCK);
  }
}
