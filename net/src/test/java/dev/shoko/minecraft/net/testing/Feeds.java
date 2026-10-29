package dev.shoko.minecraft.net.testing;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.frame.FrameDecoder;
import java.util.ArrayList;
import java.util.List;

/** Ways to give bytes to a {@link FrameDecoder}. */
public final class Feeds {

  private Feeds() {}

  /** Feeds all the bytes in one call. */
  public static List<Frame> whole(FrameDecoder decoder, byte[] data) throws ProtocolException {
    return decoder.feed(data, 0, data.length);
  }

  /** Feeds the bytes in chunks of a given size and collects every frame in order. */
  public static List<Frame> chunked(FrameDecoder decoder, byte[] data, int chunkSize)
      throws ProtocolException {
    List<Frame> frames = new ArrayList<>();
    for (int position = 0; position < data.length; position += chunkSize) {
      frames.addAll(decoder.feed(data, position, Math.min(chunkSize, data.length - position)));
    }
    return frames;
  }

  /** Feeds the bytes one at a time. */
  public static List<Frame> byteByByte(FrameDecoder decoder, byte[] data) throws ProtocolException {
    return chunked(decoder, data, 1);
  }
}
