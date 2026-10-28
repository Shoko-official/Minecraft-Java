package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Attack tests for the seed functions (spec core-random R22 to R27, R29, R31). Expected values are
 * computed here with direct long arithmetic, the JDK's SplittableRandom for the mixer (R23
 * recoupement) and a hand-written big-endian read of the MD5 digest.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class SeedFunctionsAttackTest {

  private static final long K_SQRT2 = 0x6A09E667F3BCC909L;
  private static final long K_PHI = 0x9E3779B97F4A7C15L;

  private static long mix(long z) {
    return new SplittableRandom(z - K_PHI).nextLong();
  }

  /** Provisional normalization of R26: no colon, or an empty namespace, means minecraft. */
  private static String complete(String identifier) {
    if (!identifier.contains(":")) {
      return "minecraft:" + identifier;
    }
    return identifier.startsWith(":") ? "minecraft" + identifier : identifier;
  }

  private static long[] md5Words(String text) throws Exception {
    byte[] d = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
    long lo = 0;
    long hi = 0;
    for (int i = 0; i < 8; i++) {
      lo = (lo << 8) | (d[i] & 0xFF);
      hi = (hi << 8) | (d[i + 8] & 0xFF);
    }
    return new long[] {lo, hi};
  }

  // R23 against SplittableRandom on extreme and random words
  @Test
  void mixMatchesSplittableRandom() {
    long[] words = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, K_SQRT2, K_PHI, -K_PHI};
    for (long w : words) {
      assertThat(SeedExpansion.mix(w)).isEqualTo(mix(w));
    }
    SplittableRandom r = new SplittableRandom(1L);
    for (int i = 0; i < 100_000; i++) {
      long w = r.nextLong();
      assertThat(SeedExpansion.mix(w)).isEqualTo(mix(w));
    }
  }

  // R22, R24 on extreme seeds, including the ones whose unmixed lo or hi is zero
  @ParameterizedTest
  @ValueSource(
      longs = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, K_SQRT2, -K_PHI ^ K_SQRT2, 1L << 48})
  void expansionOnExtremeSeeds(long seed) {
    long lo = seed ^ K_SQRT2;
    long hi = lo + K_PHI;
    assertThat(SeedExpansion.expandUnmixed(seed)).isEqualTo(new Seed128(lo, hi));
    assertThat(SeedExpansion.init64(seed)).isEqualTo(new Seed128(mix(lo), mix(hi)));
    assertThat(lo | hi).isNotZero();
  }

  // Non-ASCII identifiers are built from code points: the sources stay ASCII.
  static Stream<String> unusualIdentifiers() {
    return Stream.of(
        "minecraft:test",
        "test",
        ":test",
        "",
        "minecraft:",
        "probe:" + Character.toString(0xE9),
        "probe:"
            + Character.toString(0x65E5)
            + Character.toString(0x672C)
            + Character.toString(0x8A9E),
        "probe:" + Character.toString(0x1F600),
        "Minecraft:Test",
        "a:b:c",
        "  spaced  ");
  }

  // R26 (provisional normalization) on non-ASCII, empty and unusual identifiers
  @ParameterizedTest
  @MethodSource("unusualIdentifiers")
  void fingerprintMatchesADirectMd5(String identifier) throws Exception {
    long[] words = md5Words(complete(identifier));
    assertThat(IdentifierFingerprint.of(identifier)).isEqualTo(new Seed128(words[0], words[1]));
  }

  @Test
  void fingerprintOfAVeryLongIdentifier() throws Exception {
    String id = "probe:" + "x/".repeat(500_000);
    long[] words = md5Words(id);
    assertThat(IdentifierFingerprint.of(id)).isEqualTo(new Seed128(words[0], words[1]));
  }

  // R26 (S, provisional normalization, vectors V-X4): an empty namespace, ":test", is hashed as
  // its complete form minecraft:test. Red on the first implementation, aligned with the updated
  // spec.
  @Test
  void provisionalIdentifierWithAnEmptyNamespaceIsHashedAsItsCompleteForm() {
    assertThat(IdentifierFingerprint.of(":test"))
        .isEqualTo(IdentifierFingerprint.of("minecraft:test"));
  }

  // R31: salt sign extension and flags, against direct computation, extreme salts and seeds
  @Test
  void initialStateMatchesDirectComputation() throws Exception {
    int[] salts = {0, 1, -1, 7, Integer.MIN_VALUE, Integer.MAX_VALUE};
    long[] seeds = {0L, 12345L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0xFFFFFFFF00000000L};
    String[] ids = {"probe:a", "minecraft:test", "test", ":test"};
    for (int salt : salts) {
      for (long ws : seeds) {
        for (String id : ids) {
          for (int flags = 0; flags < 4; flags++) {
            boolean w = (flags & 1) != 0;
            boolean inc = (flags & 2) != 0;
            long v = (w ? ws : 0L) ^ (long) salt;
            long lo = v ^ K_SQRT2;
            long hi = lo + K_PHI;
            if (inc) {
              long[] md5 = md5Words(complete(id));
              lo ^= md5[0];
              hi ^= md5[1];
            }
            Seed128 expected = new Seed128(mix(lo), mix(hi));
            assertThat(new RandomSequenceSettings(salt, w, inc).initialState(ws, id))
                .as("salt %d ws %d id %s W %b I %b", salt, ws, id, w, inc)
                .isEqualTo(expected);
          }
        }
      }
    }
    // Sign extension: salt -1 with world seed 0 gives v = 0xFFFFFFFFFFFFFFFF (case N3).
    assertThat(new RandomSequenceSettings(-1, false, false).initialState(0L, "probe:a"))
        .isEqualTo(new Seed128(-110783831392733308L, 2932223646667407290L));
    assertThat(new RandomSequenceSettings(Integer.MIN_VALUE, false, false).initialState(0L, "x"))
        .isEqualTo(SeedExpansion.init64(0xFFFFFFFF80000000L));
  }

  // R29 (provisional) extremes against direct computation
  @Test
  void positionHashOnExtremeCoordinates() {
    int[] coords = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, -64, 319, 30_000_000};
    for (int x : coords) {
      for (int y : coords) {
        for (int z : coords) {
          long expected = mix(mix(mix(x) ^ (long) y) ^ (long) z);
          assertThat(DerivedSources.positionHash(x, y, z)).isEqualTo(expected);
          assertThat(DerivedSources.atPosition(-9L, x, y, z).state())
              .isEqualTo(SeedExpansion.init64(-9L ^ expected));
        }
      }
    }
  }

  /**
   * R29 known limit of the provisional, not faithful (Q1) hash, vectors V-X6. Adapted from the
   * original attack, which asserted distinct streams: the updated spec documents the collision as
   * allowed by the contract (only neighbours must differ). Two x whose mix(x) agree on their 33
   * high bits differ by a D below 2^31, and (x, y, z) collides with (x', y XOR D, z). The search is
   * bounded; a birthday collision on 33 bits is expected after about 10^5 values.
   */
  @Test
  void provisionalNotFaithfulKnownLimitDistantPositionsCanShareTheirWholeStream() {
    Map<Long, Integer> seen = new HashMap<>();
    int x1 = -1;
    int x2 = -1;
    for (int x = 0; x < 1 << 22 && x2 < 0; x++) {
      Integer previous = seen.putIfAbsent(SeedExpansion.mix(x) >> 31, x);
      if (previous != null) {
        x1 = previous;
        x2 = x;
      }
    }
    assertThat(x2).as("a collision on the 33 high bits of mix").isNotNegative();
    int dy = (int) (SeedExpansion.mix(x1) ^ SeedExpansion.mix(x2));
    for (int z : new int[] {0, -7, Integer.MAX_VALUE}) {
      assertThat(DerivedSources.positionHash(x1, 0, z))
          .as("positions (%d, 0, %d) and (%d, %d, %d)", x1, z, x2, dy, z)
          .isEqualTo(DerivedSources.positionHash(x2, dy, z));
    }
  }

  // Concurrent fingerprints and initial states match the sequential ones
  @Test
  void concurrentSeedFunctionsShareNothing() throws Exception {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 2000; i++) {
      ids.add("probe:id_" + i);
    }
    List<Seed128> sequential = new ArrayList<>();
    for (String id : ids) {
      sequential.add(RandomSequenceSettings.DEFAULTS.initialState(12345L, id));
    }
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Future<List<Seed128>>> futures = new ArrayList<>();
      for (int t = 0; t < 8; t++) {
        futures.add(
            pool.submit(
                () -> {
                  List<Seed128> out = new ArrayList<>();
                  for (String id : ids) {
                    out.add(RandomSequenceSettings.DEFAULTS.initialState(12345L, id));
                  }
                  return out;
                }));
      }
      for (Future<List<Seed128>> f : futures) {
        assertThat(f.get(60, TimeUnit.SECONDS)).isEqualTo(sequential);
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
