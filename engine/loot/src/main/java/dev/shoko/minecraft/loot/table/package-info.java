/**
 * The decoded form of loot tables: pools, entries of the 9 types, and the decoding of a table file.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R1 to LT-R10, LT-R23, LT-R61b, vectors V-L1 to
 * V-L4, V-L7, V-L8). The evaluation of a table is in {@code eval}. A table is immutable; the 1447
 * vanilla tables, 1589 pools and 2556 {@code item} entries are counted by tests that read the
 * extracted data at run time, nothing of the data is copied into the code.
 */
package dev.shoko.minecraft.loot.table;
