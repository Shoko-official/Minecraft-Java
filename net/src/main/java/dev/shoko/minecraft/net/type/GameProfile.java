package dev.shoko.minecraft.net.type;

import java.util.List;
import java.util.UUID;

/**
 * A game profile as it crosses the wire (R20): a UUID, a name String(16) and at most 16 properties.
 * In offline mode the properties are empty (R57).
 *
 * @param id the profile UUID
 * @param name the player name, at most 16 UTF-16 code units
 * @param properties the properties in wire order, at most 16
 */
public record GameProfile(UUID id, String name, List<ProfileProperty> properties) {}
