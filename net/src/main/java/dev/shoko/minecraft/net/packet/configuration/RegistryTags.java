package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;

/**
 * The tags of one registry inside an update tags packet (R66).
 *
 * @param registry the registry identifier
 * @param tags the tags of this registry, prefixed array
 */
public record RegistryTags(Identifier registry, List<TagIds> tags) {}
