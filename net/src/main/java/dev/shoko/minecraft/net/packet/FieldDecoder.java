package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;

/** Reads the fields of one packet type from its payload. */
@FunctionalInterface
interface FieldDecoder {

  Packet decode(PacketReader in) throws ProtocolException;
}
