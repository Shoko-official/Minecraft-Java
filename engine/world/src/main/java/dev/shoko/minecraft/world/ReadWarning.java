package dev.shoko.minecraft.world;

/**
 * A non-fatal finding of a read: a pair of a code and a message (AR-R26). The code is the
 * observable contract; the message is for humans, in English, and names the faulty element.
 *
 * @param code the kind of finding
 * @param message a description for humans
 */
public record ReadWarning(WarningCode code, String message) {}
