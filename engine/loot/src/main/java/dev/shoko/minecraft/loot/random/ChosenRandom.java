package dev.shoko.minecraft.loot.random;

import dev.shoko.minecraft.core.random.RandomSource;

/**
 * The source selected for one draw (LT-R63).
 *
 * @param source the generator to consume; the named sequence itself for {@link
 *     RandomOrigin#NAMED_SEQUENCE}, so that its persistent state advances (LT-R65)
 * @param origin which rule of LT-R63 selected it
 */
public record ChosenRandom(RandomSource source, RandomOrigin origin) {}
