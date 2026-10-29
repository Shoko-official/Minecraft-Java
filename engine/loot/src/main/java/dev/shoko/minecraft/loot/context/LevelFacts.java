package dev.shoko.minecraft.loot.context;

/**
 * What the conditions read of the level besides blocks (LT-R11, LT-R36): the weather and the time.
 * Plain data given by the caller; the module has no reader of the level (open point, see the report
 * of the skeleton).
 *
 * @param raining whether it is raining (a thunderstorm counts as rain)
 * @param thundering whether there is a thunderstorm
 * @param gameTime the game time in ticks
 * @param dayTime the time of day in ticks
 */
public record LevelFacts(boolean raining, boolean thundering, long gameTime, long dayTime) {}
