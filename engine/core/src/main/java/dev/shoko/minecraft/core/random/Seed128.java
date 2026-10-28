package dev.shoko.minecraft.core.random;

/**
 * A 128-bit value as two 64-bit words, low word first. It is the state {@code (s0, s1)} of {@link
 * XoroshiroRandom} (R13), the unmixed expansion of a 64-bit seed (R22), the fingerprint of an
 * identifier (R26) and the {@code source} long array {@code [L; low, high]} of a persisted named
 * sequence (R32). Any pair of longs is a valid value, the all-zero pair included.
 *
 * @param lo the low word ({@code s0} for a generator state)
 * @param hi the high word ({@code s1} for a generator state)
 */
public record Seed128(long lo, long hi) {}
