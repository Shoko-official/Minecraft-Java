package dev.shoko.minecraft.tools.oracle.nbt;

/** Tag ids of the public NBT format. */
final class NbtTag {

  static final int END = 0;
  static final int BYTE = 1;
  static final int SHORT = 2;
  static final int INT = 3;
  static final int LONG = 4;
  static final int FLOAT = 5;
  static final int DOUBLE = 6;
  static final int BYTE_ARRAY = 7;
  static final int STRING = 8;
  static final int LIST = 9;
  static final int COMPOUND = 10;
  static final int INT_ARRAY = 11;
  static final int LONG_ARRAY = 12;

  private NbtTag() {}

  /**
   * Smallest encoded size of one payload of this type, used to refuse absurd list lengths before
   * allocating. Unknown ids return 1 and are rejected when the first element is read.
   */
  static int minimumPayloadSize(int type) {
    return switch (type) {
      case SHORT, STRING -> 2;
      case INT, FLOAT, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY -> 4;
      case LIST -> 5;
      case LONG, DOUBLE -> 8;
      default -> 1;
    };
  }
}
