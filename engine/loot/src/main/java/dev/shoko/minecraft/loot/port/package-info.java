/**
 * The ports of the module (ADR 0004, amendment 1, decision 4): {@link
 * dev.shoko.minecraft.loot.port.EntityView} reads an entity, {@link
 * dev.shoko.minecraft.loot.port.ActionSink} receives the intents of effects, {@link
 * dev.shoko.minecraft.loot.port.RandomSequenceStore} gives the persistent named random sequences.
 * {@code entity} and {@code game} implement them; this module depends on none of them.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R16, LT-R17, LT-R65). Blocks, items and biomes
 * have no port: they are read through {@code world} and {@code item}.
 */
package dev.shoko.minecraft.loot.port;
