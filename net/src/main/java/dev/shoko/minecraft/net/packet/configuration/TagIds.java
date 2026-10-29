package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;

/**
 * One tag with the numeric ids of its members (R66). The ids are the protocol ids of a static
 * registry (R61) or the positions of a synchronized one (R63).
 *
 * @param name the tag identifier, without a leading {@code #}
 * @param entries the member ids, prefixed array of VarInt
 */
public record TagIds(Identifier name, List<Integer> entries) {}
