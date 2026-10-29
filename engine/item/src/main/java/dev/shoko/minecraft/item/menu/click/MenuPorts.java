package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.MachineRules;

/**
 * The two ports a menu needs from {@code game}.
 *
 * @param machine what the machine input slots accept
 * @param onResultTaken what happens when a result is taken
 */
public record MenuPorts(MachineRules machine, ResultTakeHook onResultTaken) {}
