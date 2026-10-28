package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.Lz4Frames;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** AR-R15, AR-R16, AR-R29 and vector LZ1: decoding the LZ4Block frame. */
class Lz4BlockCodecTest {

  private static final long BIG = 1L << 30;
  private static final byte[] MINECRAFT = "minecraft".repeat(8).getBytes(StandardCharsets.US_ASCII);
  private static final String LZ1_BLOCK_BODY =
      "9f 6d 69 6e 65 63 72 61 66 74 09 00 27 50 63 72 61 66 74";
  private static final String LZ1_STREAM =
      "4c 5a 34 42 6c 6f 63 6b 26 13 00 00 00 48 00 00 00 ad 03 61 0f "
          + LZ1_BLOCK_BODY
          + " 4c 5a 34 42 6c 6f 63 6b 16 00 00 00 00 00 00 00 00 00 00 00 00";

  private static byte[] lz1(String body) {
    byte[] bytes = Hex.bytes(body);
    return Hex.concat(
        Lz4Frames.block(0x26, bytes.length, 72, 0x0f6103ad, bytes), Lz4Frames.endMark(6));
  }

  private static void assertCorrupt(byte[] stream) {
    Failures.expect(CorruptStreamException.class, () -> Lz4BlockCodec.decode(stream, BIG));
  }

  @Test
  void blockSizeOfTheWriterIs65536() {
    // spec: AR-R15 (default block size 65 536, level 6)
    assertThat(Lz4BlockCodec.BLOCK_SIZE).isEqualTo(65_536);
  }

  @Test
  void vectorLz1CompressedBlockDecodes() throws Exception {
    // spec: LZ1 (nine literals, a copy of 58 bytes at distance 9, five literals)
    assertThat(Lz4BlockCodec.decode(Hex.bytes(LZ1_STREAM), BIG)).isEqualTo(MINECRAFT);
  }

  @Test
  void vectorLz1RawBlockDecodes() throws Exception {
    // spec: LZ1 (the same data as a raw block: token 0x16, header 4c 5a ... ad 03 61 0f)
    byte[] stream =
        Hex.concat(
            Hex.bytes("4c 5a 34 42 6c 6f 63 6b 16 48 00 00 00 48 00 00 00 ad 03 61 0f"),
            MINECRAFT,
            Lz4Frames.endMark(6));

    assertThat(Lz4BlockCodec.decode(stream, BIG)).isEqualTo(MINECRAFT);
  }

  @Test
  void endMarkAloneIsAnEmptyStream() throws Exception {
    // spec: AR-R15 (end of stream: a raw header whose three ints are 0)
    assertThat(Lz4BlockCodec.decode(Lz4Frames.endMark(6), BIG)).isEmpty();
    assertThat(Lz4BlockCodec.decode(Lz4Frames.endMark(6), 0)).isEmpty();
  }

  @Test
  void severalBlocksAreConcatenated() throws Exception {
    // spec: AR-R15 (a stream is a series of blocks)
    byte[] one = new byte[65_536];
    byte[] two = "second block".getBytes(StandardCharsets.US_ASCII);
    new SplittableRandom(1L).nextBytes(one);
    byte[] stream =
        Hex.concat(
            Lz4Frames.rawBlock(one, 6),
            Lz4Frames.literalBlock(two, 6),
            Lz4Frames.rawBlock(MINECRAFT, 6),
            Lz4Frames.endMark(6));

    assertThat(Lz4BlockCodec.decode(stream, BIG)).isEqualTo(Hex.concat(one, two, MINECRAFT));
  }

  @Test
  void literalOnlyBlocksOfAnyRunLengthDecode() throws Exception {
    // spec: LZ4 block format (literal length extension bytes), AR-R15 method 0x20
    for (int length : new int[] {1, 14, 15, 16, 269, 270, 271, 525, 5000}) {
      byte[] plain = new byte[length];
      new SplittableRandom(length).nextBytes(plain);
      byte[] stream = Hex.concat(Lz4Frames.literalBlock(plain, 6), Lz4Frames.endMark(6));

      assertThat(Lz4BlockCodec.decode(stream, BIG)).as("length " + length).isEqualTo(plain);
    }
  }

  @Test
  void longMatchesAtDistanceOneDecode() throws Exception {
    // spec: LZ4 block format (match length extension bytes)
    byte[] stream = Hex.concat(Lz4Frames.runBlock(65_536, 6), Lz4Frames.endMark(6));
    byte[] expected = new byte[65_536];
    Arrays.fill(expected, (byte) 'a');

    assertThat(Lz4BlockCodec.decode(stream, BIG)).isEqualTo(expected);
  }

  @Test
  void blockSizeOfTheTokenBoundsTheBlock() throws Exception {
    // spec: AR-R15, AR-R16 (any block size the token allows: 1 << (level + 10))
    byte[] fourK = new byte[4096];
    fourK[100] = 7;
    byte[] stream =
        Hex.concat(
            Lz4Frames.rawBlock(fourK, 2), Lz4Frames.rawBlock(fourK, 2), Lz4Frames.endMark(2));

    assertThat(Lz4BlockCodec.decode(stream, BIG)).isEqualTo(Hex.concat(fourK, fourK));
    byte[] tooBig = new byte[4097];
    assertCorrupt(Hex.concat(Lz4Frames.rawBlock(tooBig, 2), Lz4Frames.endMark(2)));
    assertCorrupt(Hex.concat(Lz4Frames.rawBlock(new byte[65_537], 6), Lz4Frames.endMark(6)));
  }

  @Test
  void limitEqualToTheOutputPassesAndOneLessIsExceeded() throws Exception {
    // spec: AR-R29 (exactly the limit is accepted, one byte more is not), LIM1 (71 and 72)
    byte[] stream = Hex.bytes(LZ1_STREAM);

    assertThat(Lz4BlockCodec.decode(stream, 72)).isEqualTo(MINECRAFT);
    OutputLimitExceededException failure =
        Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 71));
    assertThat(failure.limit()).isEqualTo(71);
    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 0));
  }

  @Test
  void limitCountsAllBlocksTogether() throws Exception {
    // spec: AR-R29 (all blocks together)
    byte[] stream =
        Hex.concat(
            Lz4Frames.rawBlock(MINECRAFT, 6),
            Lz4Frames.rawBlock(MINECRAFT, 6),
            Lz4Frames.endMark(6));

    assertThat(Lz4BlockCodec.decode(stream, 144)).hasSize(144);
    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 143));
    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 72));
  }

  @Test
  void announcedLengthIsComparedWithTheBudgetBeforeAnyDecoding() {
    // spec: AR-R29, LIM1 (a header announcing more than the budget fails there, with the body
    // unread)
    byte[] garbageBody = new byte[] {1, 2, 3};
    byte[] stream = Lz4Frames.block(0x26, garbageBody.length, 65_536, 0, garbageBody);

    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 1000));
  }

  @Test
  void firstEventInStreamOrderDecidesBetweenLimitAndCorruption() throws Exception {
    // spec: AR-R29 (a corruption before the limit is corruption, the limit reached first is the
    // limit)
    byte[] wrongChecksum = Lz4Frames.block(0x16, 72, 72, 1, MINECRAFT);
    byte[] stream =
        Hex.concat(Lz4Frames.rawBlock(MINECRAFT, 6), wrongChecksum, Lz4Frames.endMark(6));

    Failures.expect(OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 72));
    Failures.expect(CorruptStreamException.class, () -> Lz4BlockCodec.decode(stream, 144));
  }

  @Test
  void streamOfLargeAnnouncedBlocksStopsAtTheLimit() {
    // spec: AR-R29 (bomb: 20 small blocks of 65 536 bytes each, limit 1 MiB)
    byte[] block = Lz4Frames.runBlock(65_536, 6);
    byte[][] parts = new byte[21][];
    Arrays.fill(parts, block);
    parts[20] = Lz4Frames.endMark(6);
    byte[] stream = Hex.concat(parts);

    assertThat(stream.length).isLessThan(20 * 300);
    Failures.expect(
        OutputLimitExceededException.class, () -> Lz4BlockCodec.decode(stream, 1 << 20));
  }

  @Test
  void copyAtDistanceZeroIsCorrupt() {
    // spec: LZ1 negative tests (distance 0)
    assertCorrupt(lz1(LZ1_BLOCK_BODY.replace("74 09 00 27", "74 00 00 27")));
  }

  @Test
  void copyBeyondTheOutputSoFarIsCorrupt() {
    // spec: LZ1 negative tests (distance beyond the output: only 9 bytes exist)
    assertCorrupt(lz1(LZ1_BLOCK_BODY.replace("74 09 00 27", "74 64 00 27")));
  }

  @Test
  void blockThatDecodesToAnotherLengthThanAnnouncedIsCorrupt() {
    // spec: AR-R15 (decompressed length of the header), here 73 and 71 bytes instead of 72
    assertCorrupt(lz1(LZ1_BLOCK_BODY.replace("00 27", "00 28")));
    assertCorrupt(lz1(LZ1_BLOCK_BODY.replace("00 27", "00 26")));
  }

  @Test
  void literalsBeyondTheEndOfTheBlockAreCorrupt() {
    // spec: LZ4 block format (a literal run cannot exceed the block)
    assertCorrupt(lz1("ff 6d 69 6e 65"));
    assertCorrupt(lz1("f0 ff ff"));
  }

  @Test
  void streamWithoutEndMarkIsCorrupt() {
    // spec: LZ1 negative tests (no end mark), AR-R15
    byte[] block = Arrays.copyOf(Hex.bytes(LZ1_STREAM), 40);

    assertCorrupt(block);
    assertCorrupt(Lz4Frames.rawBlock(MINECRAFT, 6));
    assertCorrupt(new byte[0]);
  }

  @Test
  void endMarkWithANonZeroChecksumIsCorrupt() {
    // spec: LZ1 negative tests (end mark with a non-zero checksum)
    assertCorrupt(
        Hex.concat(Lz4Frames.rawBlock(MINECRAFT, 6), Lz4Frames.block(0x16, 0, 0, 1, new byte[0])));
  }

  @Test
  void bytesAfterTheEndMarkAreCorrupt() {
    // spec: Lz4BlockCodec.decode contract (bytes after it)
    assertCorrupt(Hex.concat(Hex.bytes(LZ1_STREAM), new byte[] {0}));
    assertCorrupt(Hex.concat(Hex.bytes(LZ1_STREAM), Lz4Frames.endMark(6)));
  }

  @Test
  void wrongChecksumIsCorrupt() {
    // spec: LZ1 negative tests (wrong checksum), AR-R15 (masked XXH32, seed 0x9747b28c)
    byte[] stream = Hex.bytes(LZ1_STREAM);
    stream[17] = 0;
    stream[18] = 0;
    stream[19] = 0;
    stream[20] = 0;

    assertCorrupt(stream);
    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(0x16, 72, 72, Lz4Frames.checksum(MINECRAFT) + 1, MINECRAFT),
            Lz4Frames.endMark(6)));
  }

  @Test
  void invalidHeadersAreCorrupt() {
    // spec: AR-R15 and LZ1 (magic, method 0x10 or 0x20, positive lengths, equal lengths when raw)
    byte[] badMagic = Hex.bytes(LZ1_STREAM);
    badMagic[0] = 'X';
    assertCorrupt(badMagic);
    for (int token : new int[] {0x06, 0x30 | 6, 0x40 | 6, 0xF0 | 6}) {
      assertCorrupt(
          Hex.concat(
              Lz4Frames.block(token, 72, 72, Lz4Frames.checksum(MINECRAFT), MINECRAFT),
              Lz4Frames.endMark(6)));
    }
    assertCorrupt(
        Hex.concat(
            Lz4Frames.block(0x16, 72, 71, Lz4Frames.checksum(MINECRAFT), MINECRAFT),
            Lz4Frames.endMark(6)));
    assertCorrupt(Hex.concat(Lz4Frames.block(0x16, -72, 72, 0, new byte[0]), Lz4Frames.endMark(6)));
    assertCorrupt(Hex.concat(Lz4Frames.block(0x26, 5, -1, 0, new byte[5]), Lz4Frames.endMark(6)));
    assertCorrupt(Hex.concat(Lz4Frames.block(0x26, 0, 72, 0, new byte[0]), Lz4Frames.endMark(6)));
  }

  @Test
  void truncatedStreamsAreCorrupt() {
    // spec: AR-R15 (a header or a body cut short)
    byte[] stream = Hex.bytes(LZ1_STREAM);

    for (int length : new int[] {1, 8, 20, 21, 30, 39, 40, 60}) {
      assertCorrupt(Arrays.copyOf(stream, length));
    }
  }

  @Test
  void encodedEmptyInputIsTheEndMarkOnly() {
    // spec: AR-R15 (end of stream), Lz4BlockCodec.encode contract (possibly empty)
    assertThat(Hex.format(Lz4BlockCodec.encode(new byte[0])))
        .isEqualTo("4c 5a 34 42 6c 6f 63 6b 16 00 00 00 00 00 00 00 00 00 00 00 00");
  }

  @Test
  void incompressibleBlocksAreWrittenRawWithTheirChecksum() {
    // spec: AR-R15 (raw when compression does not shrink the block; token 0x16; masked XXH32)
    byte[] data = new byte[1000];
    new SplittableRandom(5L).nextBytes(data);

    byte[] stream = Lz4BlockCodec.encode(data);

    assertThat(Lz4Frames.parse(stream)).hasSize(2);
    assertThat(Hex.format(Arrays.copyOf(stream, 9))).isEqualTo("4c 5a 34 42 6c 6f 63 6b 16");
    assertThat(Arrays.copyOfRange(stream, 21, 1021)).isEqualTo(data);
    assertThat(Lz4Frames.parse(stream).getFirst().checksum()).isEqualTo(Lz4Frames.checksum(data));
    assertThat(Lz4Frames.parse(stream).getFirst().compressedLength()).isEqualTo(1000);
  }

  @Test
  void blocksAreAtMost65536BytesAndTokensAreTheDefaultOnes() {
    // spec: AR-R15, AR-R16 (blocks of 65 536 bytes, level 6, tokens 0x16 and 0x26)
    byte[] data = new byte[200_000];
    new SplittableRandom(6L).nextBytes(data);
    Arrays.fill(data, 70_000, 140_000, (byte) 3);

    var blocks = Lz4Frames.parse(Lz4BlockCodec.encode(data));

    assertThat(blocks)
        .extracting(Lz4Frames.Block::length)
        .containsExactly(65_536, 65_536, 65_536, 3_392, 0);
    assertThat(blocks).extracting(Lz4Frames.Block::token).isSubsetOf(0x16, 0x26);
    assertThat(blocks.getLast().token()).isEqualTo(0x16);
  }

  @Test
  void decodeOfEncodeGivesTheInputForManySizesAndContents() throws Exception {
    // spec: Lz4BlockCodec.encode contract (decode of the result gives the input)
    SplittableRandom random = new SplittableRandom(77L);
    int[] sizes = {
      0, 1, 4, 11, 12, 13, 15, 16, 17, 255, 256, 4096, 65_535, 65_536, 65_537, 200_001
    };
    for (int size : sizes) {
      byte[] random1 = new byte[size];
      random.nextBytes(random1);
      byte[] zeros = new byte[size];
      byte[] text = new byte[size];
      for (int k = 0; k < size; k++) {
        text[k] = (byte) "minecraft world".charAt(k % 15);
      }
      for (byte[] data : new byte[][] {random1, zeros, text}) {
        byte[] stream = Lz4BlockCodec.encode(data);
        assertThat(Lz4BlockCodec.decode(stream, size)).as("size " + size).isEqualTo(data);
      }
    }
  }

  @Test
  void encodingIsDeterministic() {
    // spec: determinism (same input, same output)
    byte[] data = new byte[100_000];
    new SplittableRandom(8L).nextBytes(data);
    Arrays.fill(data, 10, 50_000, (byte) 1);

    assertThat(Lz4BlockCodec.encode(data)).isEqualTo(Lz4BlockCodec.encode(data.clone()));
  }
}
