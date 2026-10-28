package dev.shoko.minecraft.world;

import java.util.List;

/**
 * The outcome of reading a chunk, or of reading one of its parts (AR-R26): exactly one of three
 * exclusive issues. An input or output failure of the file system is none of them: it is an {@link
 * java.io.IOException} thrown by the call (AR-R27).
 *
 * <p>Warnings never change the issue, which depends only on the input. Every list is unmodifiable,
 * never {@code null} and empty when there is nothing to report.
 *
 * @param <T> what a successful read yields: bytes, a chunk
 */
public sealed interface ReadResult<T>
    permits ReadResult.Present, ReadResult.Absent, ReadResult.Refused {

  /**
   * The data was decoded.
   *
   * @param <T> the type of the data
   * @param value the data
   * @param warnings the findings made while decoding, in the order they were made
   */
  record Present<T>(T value, List<ReadWarning> warnings) implements ReadResult<T> {}

  /**
   * No usable chunk at the position, and not an error of the world: the engine generates it.
   *
   * @param <T> the type a successful read would have yielded
   * @param cause why, a closed category
   * @param warnings the findings made while reading, empty for {@link AbsentCause#NO_REGION_FILE}
   *     and {@link AbsentCause#EMPTY_SLOT}, which are normal
   */
  record Absent<T>(AbsentCause cause, List<ReadWarning> warnings) implements ReadResult<T> {}

  /**
   * A record exists but is not interpreted: the chunk is neither generated nor loaded, and its
   * bytes are never touched.
   *
   * @param <T> the type a successful read would have yielded
   * @param cause why, a closed category
   * @param message for humans, in English: names the file, the index in the region, and the faulty
   *     field or section. Its wording is not part of the contract; the cause is
   */
  record Refused<T>(RefusedCause cause, String message) implements ReadResult<T> {}
}
