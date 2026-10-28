package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * One entry of a tag file (R32): an element identifier, or a reference to another tag.
 *
 * @param id the identifier of the element, or of the tag when {@code tag} is true (written {@code
 *     #id}, read as an identifier by R22)
 * @param tag whether the entry references a tag (R23)
 * @param required whether a missing element or tag makes the whole tag fail (R36); {@code true} for
 *     a plain string entry
 */
public record TagEntry(Identifier id, boolean tag, boolean required) {}
