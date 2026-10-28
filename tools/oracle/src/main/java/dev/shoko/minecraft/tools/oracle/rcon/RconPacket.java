package dev.shoko.minecraft.tools.oracle.rcon;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * One packet of the public RCON protocol: a little-endian int32 length (counting what follows), a
 * little-endian request id, a little-endian type, the payload and two null bytes.
 *
 * @param type {@link #TYPE_LOGIN}, {@link #TYPE_COMMAND} or {@link #TYPE_RESPONSE}
 */
public record RconPacket(int requestId, int type, String payload) {

  public static final int TYPE_RESPONSE = 0;
  public static final int TYPE_COMMAND = 2;
  public static final int TYPE_LOGIN = 3;

  /** Request id that the server answers with when authentication is refused. */
  public static final int AUTH_FAILED_ID = -1;

  /** Id, type and the two terminating null bytes. */
  private static final int OVERHEAD = 10;

  /** Generous bound on one packet; the server splits long responses in smaller fragments. */
  static final int MAX_LENGTH = 1 << 20;

  public byte[] encode() {
    return encode(requestId, type, payload.getBytes(StandardCharsets.UTF_8));
  }

  /** Encodes raw payload bytes, which need not be a whole UTF-8 text on their own. */
  static byte[] encode(int requestId, int type, byte[] payload) {
    ByteBuffer buffer =
        ByteBuffer.allocate(Integer.BYTES + OVERHEAD + payload.length)
            .order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(OVERHEAD + payload.length).putInt(requestId).putInt(type).put(payload);
    buffer.put((byte) 0).put((byte) 0);
    return buffer.array();
  }

  /** Hides the payload of login packets, which is the password. */
  @Override
  public String toString() {
    String shown = type == TYPE_LOGIN ? "<hidden>" : payload;
    return "RconPacket[requestId=" + requestId + ", type=" + type + ", payload=" + shown + "]";
  }

  public void write(OutputStream out) throws IOException {
    out.write(encode());
    out.flush();
  }

  /** Reads one whole packet, waiting for every fragment of it to arrive. */
  public static RconPacket read(InputStream in) throws IOException {
    Frame frame = readFrame(in);
    return new RconPacket(frame.requestId(), frame.type(), decodeStrict(frame.payload()));
  }

  /** Decodes UTF-8 and fails on the first invalid byte instead of replacing it. */
  static String decodeStrict(byte[] bytes) throws IOException {
    CharsetDecoder decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    ByteBuffer input = ByteBuffer.wrap(bytes);
    CharBuffer output = CharBuffer.allocate(bytes.length);
    CoderResult result = decoder.decode(input, output, true);
    if (!result.isError()) {
      result = decoder.flush(output);
    }
    if (result.isError()) {
      int offset = input.position();
      throw new IOException(
          String.format(
              "RCON payload is not valid UTF-8: byte 0x%02x at offset %d",
              bytes[offset] & 0xff, offset));
    }
    return output.flip().toString();
  }

  /**
   * Reads one packet without decoding its payload: a response split over several packets may cut a
   * multi-byte character, so fragments are joined as bytes and decoded once.
   */
  static Frame readFrame(InputStream in) throws IOException {
    int length =
        ByteBuffer.wrap(readExactly(in, Integer.BYTES)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    if (length < OVERHEAD || length > MAX_LENGTH) {
      throw new IOException("RCON packet with invalid length " + length);
    }
    ByteBuffer body = ByteBuffer.wrap(readExactly(in, length)).order(ByteOrder.LITTLE_ENDIAN);
    int requestId = body.getInt();
    int type = body.getInt();
    if (body.get(length - 2) != 0 || body.get(length - 1) != 0) {
      throw new IOException("RCON packet " + requestId + " lacks its two null terminators");
    }
    byte[] payload = new byte[length - OVERHEAD];
    body.get(payload);
    return new Frame(requestId, type, payload);
  }

  /** A received packet with its payload still in bytes. */
  static final class Frame {
    private final int requestId;
    private final int type;
    private final byte[] payload;

    Frame(int requestId, int type, byte[] payload) {
      this.requestId = requestId;
      this.type = type;
      this.payload = payload;
    }

    int requestId() {
      return requestId;
    }

    int type() {
      return type;
    }

    byte[] payload() {
      return payload;
    }
  }

  private static byte[] readExactly(InputStream in, int count) throws IOException {
    byte[] bytes = in.readNBytes(count);
    if (bytes.length != count) {
      throw new EOFException("RCON connection closed in the middle of a packet");
    }
    return bytes;
  }
}
