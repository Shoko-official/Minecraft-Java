/**
 * Packs and their stack: file access, discovery, priority, replacement, block filters and overlays
 * (R3, R7, R8, R14, R19, R40, R41, R47, R48).
 *
 * <p>Spec: {@code docs/specs/data-datapack-loading.md}. A stack lists packs from the lowest to the
 * highest priority and answers, for every path, which pack files exist and which one wins. File
 * lists are sorted by code point and use {@code /} on every system (determinism). No method accepts
 * or returns {@code null}.
 */
package dev.shoko.minecraft.data.pack;
