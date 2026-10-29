/**
 * Encoders of the decoded data back to JSON, used by the round trip of plan L1 (decode, encode,
 * same tree). Each encoder mirrors a decoder; a field at its default value is not written.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (plan of tests L1).
 */
package dev.shoko.minecraft.loot.encode;
