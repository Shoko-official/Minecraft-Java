package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;

/** The low-precision vector of R18: three 15-bit components, a 2-bit scale and a scale tail. */
final class LpVec3Codec {

  /** Largest quantized component (R18). */
  private static final int MAX_QUANTIZED = 32766;

  private static final double MIN_MAGNITUDE = 1.0 / MAX_QUANTIZED;
  private static final int COMPONENT_MASK = 0x7fff;
  private static final int X_SHIFT = 3;
  private static final int Y_SHIFT = 18;
  private static final int Z_SHIFT = 33;
  private static final long CONTINUATION_FLAG = 4;
  private static final long LOW_SCALE_MASK = 3;

  /** The scale tail is a VarInt of {@code scale >> 2}, so the scale fits 33 bits. */
  private static final long MAX_SCALE = (1L << 33) - 1;

  private LpVec3Codec() {}

  static Vec3 read(PacketReader in) throws ProtocolException {
    int first = in.readUnsignedByte();
    if (first == 0) {
      return new Vec3(0, 0, 0);
    }
    long second = in.readUnsignedByte();
    long high = Integer.toUnsignedLong(in.readInt());
    long word = first | (second << 8) | (high << 16);
    long scale = word & LOW_SCALE_MASK;
    if ((word & CONTINUATION_FLAG) != 0) {
      scale |= Integer.toUnsignedLong(in.readVarInt()) << 2;
    }
    return new Vec3(
        dequantize(word >> X_SHIFT, scale),
        dequantize(word >> Y_SHIFT, scale),
        dequantize(word >> Z_SHIFT, scale));
  }

  static void write(PacketWriter out, Vec3 vector) throws ProtocolException {
    double max = requireFinite(vector);
    if (max < MIN_MAGNITUDE) {
      out.writeByte(0);
      return;
    }
    double ceil = Math.ceil(max);
    if (ceil > MAX_SCALE) {
      throw new ProtocolException(Reason.VALUE_OUT_OF_RANGE, "vector component too large: " + max);
    }
    long scale = (long) ceil;
    boolean tail = scale > LOW_SCALE_MASK;
    long word =
        (scale & LOW_SCALE_MASK)
            | (tail ? CONTINUATION_FLAG : 0)
            | (quantize(vector.x(), scale) << X_SHIFT)
            | (quantize(vector.y(), scale) << Y_SHIFT)
            | (quantize(vector.z(), scale) << Z_SHIFT);
    out.writeByte((int) word);
    out.writeByte((int) (word >> 8));
    out.writeInt((int) (word >>> 16));
    if (tail) {
      out.writeVarInt((int) (scale >> 2));
    }
  }

  private static double requireFinite(Vec3 vector) throws ProtocolException {
    double max = 0;
    for (double component : new double[] {vector.x(), vector.y(), vector.z()}) {
      if (!Double.isFinite(component)) {
        throw new ProtocolException(Reason.VALUE_OUT_OF_RANGE, "non-finite vector component");
      }
      max = Math.max(max, Math.abs(component));
    }
    return max;
  }

  private static long quantize(double component, long scale) {
    return Math.round((component / scale * 0.5 + 0.5) * MAX_QUANTIZED);
  }

  private static double dequantize(long shifted, long scale) {
    long quantized = Math.min(shifted & COMPONENT_MASK, MAX_QUANTIZED);
    return (quantized * 2.0 / MAX_QUANTIZED - 1.0) * scale;
  }
}
