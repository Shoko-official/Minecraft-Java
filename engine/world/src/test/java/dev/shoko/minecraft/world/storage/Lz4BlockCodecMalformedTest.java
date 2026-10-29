package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.Lz4Frames;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * AR-R15, AR-R16, AR-R29 and Lz4BlockCodec.decode: malformed blocks are a corrupt stream. Each
 * stream is built so that the damage is the only fault: the checksum is the one of the bytes a
 * decoder that missed the check would produce, so that only the check itself can refuse the block.
 */
class Lz4BlockCodecMalformedTest {

  private static final long BIG = 1L << 30;
  private static final int LZ4 = 0x20;
  private static final int RAW = 0x10;
  private static final int LEVEL = 6;
  private static final byte[] LAST_FIVE = {0x50, 'a', 'a', 'a', 'a', 'a'};

  private static byte[] ascii(String text) {
    return text.getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] lz4Block(byte[] body, byte[] plain) {
    return Hex.concat(
        Lz4Frames.block(LZ4 | LEVEL, body.length, plain.length, Lz4Frames.checksum(plain), body),
        Lz4Frames.endMark(LEVEL));
  }

  private static void assertCorrupt(byte[] stream) {
    Failures.expect(CorruptStreamException.class, () -> Lz4BlockCodec.decode(stream, BIG));
  }

  @Test
  void aCopyAtDistanceZeroIsRefusedEvenWhenTheChecksumMatchesWhatAnUnwrittenCopyGives() {
    // spec: AR-R15 (the format has no distance 0): a decoder that copied "from itself" would
    // produce four zero bytes, whose checksum is the one given here
    byte[] body = Hex.concat(new byte[] {0x10, 'a', 0, 0}, LAST_FIVE);
    byte[] plain = Hex.concat(ascii("a"), new byte[4], ascii("aaaaa"));

    assertCorrupt(lz4Block(body, plain));
  }

  @Test
  void aCopyFromBeforeTheStartOfTheBlockIsRefused() {
    // spec: AR-R15, LZ1 negative tests: one byte is out, distance 2 reaches before it
    byte[] body = Hex.concat(new byte[] {0x10, 'a', 2, 0}, LAST_FIVE);

    assertCorrupt(lz4Block(body, ascii("aaaaaaaaaa")));
  }

  @Test
  void aCopyFromExactlyTheStartOfTheOutputIsAccepted() throws Exception {
    // spec: AR-R15: distance 2 after two bytes is the first byte of the output, a valid copy
    byte[] body = Hex.concat(new byte[] {0x20, 'a', 'b', 2, 0}, new byte[] {0x50}, ascii("xxxxx"));
    byte[] plain = ascii("abababxxxxx");

    assertThat(Lz4BlockCodec.decode(lz4Block(body, plain), BIG)).isEqualTo(plain);
  }

  @Test
  void aBlockThatDecodesToFewerBytesThanAnnouncedIsRefusedEvenWithTheChecksumOfTheZeroFill() {
    // spec: AR-R15 (decompressed length of the header): three bytes decoded, five announced; a
    // decoder that kept the zeros of the unwritten end would find the checksum right
    byte[] body = {0x30, 'a', 'b', 'c'};
    byte[] plain = {'a', 'b', 'c', 0, 0};

    assertCorrupt(lz4Block(body, plain));
  }

  @Test
  void aBlockThatDecodesToMoreBytesThanAnnouncedIsRefused() {
    // spec: AR-R15: five literals for a block announced with three
    byte[] body = {0x50, 'a', 'b', 'c', 'd', 'e'};

    assertCorrupt(lz4Block(body, ascii("abc")));
  }

  @Test
  void aMatchThatRunsPastTheAnnouncedLengthIsRefused() {
    // spec: AR-R15: one literal and a match of 19 bytes cannot fit a block announced with 10
    byte[] body = Hex.concat(new byte[] {0x1F, 'a', 1, 0, 0}, LAST_FIVE);

    assertCorrupt(lz4Block(body, ascii("aaaaaaaaaa")));
  }

  @Test
  void aRawBlockWithUnequalLengthsIsRefusedEvenWhenItsChecksumIsRight() {
    // spec: AR-R15 (raw method: the compressed and decompressed lengths are the same)
    byte[] longer = ascii("abcde");
    byte[] shorter = ascii("abcd");
    byte[] compressedFiveLengthFour =
        Hex.concat(
            Lz4Frames.block(RAW | LEVEL, 5, 4, Lz4Frames.checksum(shorter), longer),
            Lz4Frames.endMark(LEVEL));
    byte[] compressedFourLengthFive =
        Hex.concat(
            Lz4Frames.block(RAW | LEVEL, 4, 5, Lz4Frames.checksum(longer), shorter),
            Lz4Frames.endMark(LEVEL));

    assertCorrupt(compressedFiveLengthFour);
    assertCorrupt(compressedFourLengthFive);
  }

  @Test
  void aBlockOfTheCompressedMethodWithAllZeroLengthsIsNotTheEndMark() {
    // spec: AR-R15 (the end of the stream is a header of the raw method)
    assertCorrupt(Lz4Frames.block(LZ4 | LEVEL, 0, 0, 0, new byte[0]));
    assertCorrupt(
        Hex.concat(Lz4Frames.block(LZ4 | LEVEL, 0, 0, 0, new byte[0]), Lz4Frames.endMark(LEVEL)));
  }

  @Test
  void aRawBlockOfZeroLengthsWithAChecksumIsNotTheEndMark() {
    // spec: AR-R15 (the end mark has three zero ints, the checksum included)
    assertCorrupt(Lz4Frames.block(RAW | LEVEL, 0, 0, 1, new byte[0]));
    assertCorrupt(Lz4Frames.block(RAW | LEVEL, 0, 0, Lz4Frames.checksum(new byte[0]), new byte[0]));
  }

  @Test
  void aBlockOfZeroDecompressedLengthIsRefusedEvenWithTheChecksumOfNothing() {
    // spec: AR-R15 (non-positive length): the block would decode to nothing, with a right checksum
    int ofNothing = Lz4Frames.checksum(new byte[0]);

    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(LZ4 | LEVEL, 1, 0, ofNothing, new byte[] {0}),
            Lz4Frames.endMark(LEVEL)));
    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(RAW | LEVEL, 5, 0, ofNothing, new byte[5]), Lz4Frames.endMark(LEVEL)));
    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(RAW | LEVEL, 0, 5, ofNothing, new byte[0]), Lz4Frames.endMark(LEVEL)));
    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(LZ4 | LEVEL, 0, 5, ofNothing, new byte[0]), Lz4Frames.endMark(LEVEL)));
  }

  @Test
  void aBlockCutShortAfterABlockWithinTheBudgetIsACorruptStream() {
    // spec: AR-R15 (a body cut short), AR-R29: the limit is not reached, the stream is damaged
    byte[] cut = Lz4Frames.block(LZ4 | LEVEL, 1000, 65_536, 0, new byte[10]);
    byte[] stream = Hex.concat(Lz4Frames.rawBlock(ascii("minecraft"), LEVEL), cut);

    Failures.expect(CorruptStreamException.class, () -> Lz4BlockCodec.decode(stream, 70_000));
  }

  @Test
  void aBlockAnnouncingMoreThanTheBudgetLeftIsTheLimitEvenIfItsBodyIsCutShort() {
    // Lz4BlockCodec.decode contract: the limit is raised from the header of a block whose
    // decompressed length exceeds the budget left, before the body is looked at; the same stream
    // with a budget that fits is the corrupt stream
    byte[] cut = Lz4Frames.block(LZ4 | LEVEL, 1000, 65_536, 0, new byte[10]);
    byte[] stream = Hex.concat(Lz4Frames.rawBlock(ascii("minecraft"), LEVEL), cut);

    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 100));
    Failures.expect(
        OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 9 + 65_535));
    Failures.expect(CorruptStreamException.class, () -> Lz4BlockCodec.decode(stream, 9 + 65_536));
  }
}
