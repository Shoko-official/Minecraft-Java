package dev.shoko.minecraft.core.random;

import java.util.List;

/**
 * Vectors V-L1 to V-L3 of {@code docs/specs/core-random-vectors.md}, copied verbatim. Level D for
 * the public {@code java.util.Random} algorithm (R1 to R7), S for equality with the game (R8).
 * Floating-point values are the IEEE 754 bit patterns given between brackets in the spec.
 */
final class Lcg48Vectors {

  private Lcg48Vectors() {}

  /**
   * One V-L1 block; each list starts from a fresh generator built with {@code seed}.
   *
   * @param booleans the 16 first {@code nextBoolean()}, 1 meaning true
   */
  record BasicDraws(
      long seed,
      List<Integer> ints,
      List<Long> longs,
      String booleans,
      List<String> floats,
      List<String> doubles,
      List<String> gaussians) {}

  /** One V-L2 row: three {@code nextInt(bound)} then one {@code nextInt()} on a fresh generator. */
  record BoundedDraws(long seed, int bound, List<Integer> values, int nextIntAfter) {}

  /**
   * One V-L3 row: {@code nextInt(2^30 + 1)} on a fresh generator, its steps, then {@code
   * nextInt()}.
   */
  record Rejection(long seed, int value, int steps, int nextIntAfter) {}

  static final List<BasicDraws> V_L1 =
      List.of(
          new BasicDraws(
              0L,
              List.of(-1155484576, -723955400, 1033096058, -1690734402, -1557280266),
              List.of(-4962768465676381896L, 4437113781045784766L, -6688467811848818630L),
              "1101101011000111",
              List.of("0x3F3B20B4", "0x3F54D951", "0x3E764F2C"),
              List.of("0x3FE764168EA6CA89", "0x3FCEC9E5B3672E14", "0x3FE465B93A78EF81"),
              List.of(
                  "0x3FE9AE59D1D6F861",
                  "0xBFECD9772EB2E0C8",
                  "0x4000A5B9CCA3A4B8",
                  "0x3FE870CF65026A96")),
          new BasicDraws(
              1L,
              List.of(-1155869325, 431529176, 1761283695, 1749940626, 892128508),
              List.of(-4964420948893066024L, 7564655870752979346L, 3831662765844904176L),
              "1000000111001011",
              List.of("0x3F3B1AD5", "0x3DCDC4E0", "0x3ED1F61C"),
              List.of("0x3FE7635AA8CDC4E6", "0x3FDA3EC39684DF98", "0x3FCA96666128D71C"),
              List.of(
                  "0x3FF8FC3C669AA4C1",
                  "0xBFE3763B5EE2E541",
                  "0xBFF175AB5E5BB186",
                  "0xBFE3FC3B989CFC86")),
          new BasicDraws(
              42L,
              List.of(-1170105035, 234785527, -1360544799, 205897768, 1325939940),
              List.of(-5025562857975149833L, -5843495416241995736L, 5694868678511409995L),
              "1010010110100001",
              List.of("0x3F3A419D", "0x3D5FE8A0", "0x3F2EE7BB"),
              List.of("0x3FE74833A06FF457", "0x3FE5DCF778622E01", "0x3FD3C20F3F12BBB4"),
              List.of(
                  "0x3FF2453E82115D86",
                  "0x3FED6BCA38120847",
                  "0xBFEE654EB7A040C2",
                  "0xBFF1B63B72513280")),
          new BasicDraws(
              -1L,
              List.of(1155099827, 1887904451, 52699159, -1941176418, -1451336087),
              List.of(4961115982468162243L, 226341162490527646L, -6233441030884181172L),
              "0001110010100001",
              List.of("0x3E89B2D8", "0x3EE10E44", "0x3C490800"),
              List.of("0x3FD1365B2708722C", "0x3F8921011897FF00", "0x3FE52FCBCCCE6DDA"),
              List.of(
                  "0x3FFC90B7B3790A3A",
                  "0xBFED740E27D7F93C",
                  "0x3FDF2A0338E45A13",
                  "0x3FDD3DAA8A1780ED")),
          new BasicDraws(
              12345L,
              List.of(1553932502, -2090749135, -287790814, -355989640, -716867186),
              List.of(6674089274190705457L, -1236052134575208584L, -3078921119283744887L),
              "0111100000010110",
              List.of("0x3EB93E40", "0x3F0361B3", "0x3F6ED8A9"),
              List.of("0x3FD727C838361B32", "0x3FEDDB152756403B", "0x3FEAA8AF304D277C"),
              List.of(
                  "0xBFC80A1FFE3D6B0B",
                  "0x3FE2D4785FC80083",
                  "0x3FEE5C9BD7C8E902",
                  "0xBFDFA24B9A887588")),
          new BasicDraws(
              -123456789L,
              List.of(1442175866, -958985574, -148610113, -1285892480, 2058547138),
              List.of(6194098178591492762L, -638275576475756928L, 8841392636790150424L),
              "0111001100011011",
              List.of("0x3EABEBB6", "0x3F46D70A", "0x3F772463"),
              List.of("0x3FD57D76DC6D70A8", "0x3FEEE48C759AD6B4", "0x3FDEACBBF6BA1950"),
              List.of(
                  "0xBFB374A85180F6F2",
                  "0x3FCB91B3B43CCAFB",
                  "0xBFB14E8C833BA391",
                  "0x3FF2962DBA03333D")),
          new BasicDraws(
              Long.MIN_VALUE,
              List.of(-1155484576, -723955400, 1033096058, -1690734402, -1557280266),
              List.of(-4962768465676381896L, 4437113781045784766L, -6688467811848818630L),
              "1101101011000111",
              List.of("0x3F3B20B4", "0x3F54D951", "0x3E764F2C"),
              List.of("0x3FE764168EA6CA89", "0x3FCEC9E5B3672E14", "0x3FE465B93A78EF81"),
              List.of(
                  "0x3FE9AE59D1D6F861",
                  "0xBFECD9772EB2E0C8",
                  "0x4000A5B9CCA3A4B8",
                  "0x3FE870CF65026A96")),
          new BasicDraws(
              Long.MAX_VALUE,
              List.of(1155099827, 1887904451, 52699159, -1941176418, -1451336087),
              List.of(4961115982468162243L, 226341162490527646L, -6233441030884181172L),
              "0001110010100001",
              List.of("0x3E89B2D8", "0x3EE10E44", "0x3C490800"),
              List.of("0x3FD1365B2708722C", "0x3F8921011897FF00", "0x3FE52FCBCCCE6DDA"),
              List.of(
                  "0x3FFC90B7B3790A3A",
                  "0xBFED740E27D7F93C",
                  "0x3FDF2A0338E45A13",
                  "0x3FDD3DAA8A1780ED")),
          new BasicDraws(
              281474976710656L,
              List.of(-1155484576, -723955400, 1033096058, -1690734402, -1557280266),
              List.of(-4962768465676381896L, 4437113781045784766L, -6688467811848818630L),
              "1101101011000111",
              List.of("0x3F3B20B4", "0x3F54D951", "0x3E764F2C"),
              List.of("0x3FE764168EA6CA89", "0x3FCEC9E5B3672E14", "0x3FE465B93A78EF81"),
              List.of(
                  "0x3FE9AE59D1D6F861",
                  "0xBFECD9772EB2E0C8",
                  "0x4000A5B9CCA3A4B8",
                  "0x3FE870CF65026A96")),
          new BasicDraws(
              25214903917L,
              List.of(0, 4232237, 178803790, 758674372, 1565954732),
              List.of(4232237L, 767956431209526212L, 6725724361348706664L),
              "0000000010011110",
              List.of("0x00000000", "0x3A812800", "0x3D2A8540"),
              List.of("0x3DB0250800000000", "0x3FA550A8969C39E0", "0x3FD755A4A1761716"),
              List.of(
                  "0xBFCF6E880764BD18",
                  "0xBFE7A951AD374034",
                  "0x3FC4283B285F629A",
                  "0xBFF921DD26FD44E0")));

  static final List<BoundedDraws> V_L2 =
      List.of(
          new BoundedDraws(0L, 1, List.of(0, 0, 0), -1690734402),
          new BoundedDraws(0L, 2, List.of(1, 1, 0), -1690734402),
          new BoundedDraws(0L, 7, List.of(5, 2, 4), -1690734402),
          new BoundedDraws(0L, 10, List.of(0, 8, 9), -1690734402),
          new BoundedDraws(0L, 16, List.of(11, 13, 3), -1690734402),
          new BoundedDraws(0L, 26, List.of(18, 18, 23), -1690734402),
          new BoundedDraws(0L, 100, List.of(60, 48, 29), -1690734402),
          new BoundedDraws(0L, 1073741824, List.of(784870680, 892752974, 258274014), -1690734402),
          new BoundedDraws(0L, 1073741825, List.of(516548029, 663681053, 251269761), -1728529858),
          new BoundedDraws(0L, 2147483647, List.of(1569741360, 1785505948, 516548029), -1690734402),
          new BoundedDraws(42L, 1, List.of(0, 0, 0), 205897768),
          new BoundedDraws(42L, 2, List.of(1, 0, 1), 205897768),
          new BoundedDraws(42L, 7, List.of(1, 5, 6), 205897768),
          new BoundedDraws(42L, 10, List.of(0, 3, 8), 205897768),
          new BoundedDraws(42L, 16, List.of(11, 0, 10), 205897768),
          new BoundedDraws(42L, 26, List.of(0, 7, 22), 205897768),
          new BoundedDraws(42L, 100, List.of(30, 63, 48), 205897768),
          new BoundedDraws(42L, 1073741824, List.of(781215565, 58696381, 733605624), 205897768),
          new BoundedDraws(42L, 1073741825, List.of(117392763, 102948884, 662969970), -248792245),
          new BoundedDraws(42L, 2147483647, List.of(1562431130, 117392763, 1467211248), 205897768),
          new BoundedDraws(-1L, 1, List.of(0, 0, 0), -1941176418),
          new BoundedDraws(-1L, 2, List.of(0, 0, 0), -1941176418),
          new BoundedDraws(-1L, 7, List.of(3, 6, 4), -1941176418),
          new BoundedDraws(-1L, 10, List.of(3, 5, 9), -1941176418),
          new BoundedDraws(-1L, 16, List.of(4, 7, 0), -1941176418),
          new BoundedDraws(-1L, 26, List.of(5, 21, 9), -1941176418),
          new BoundedDraws(-1L, 100, List.of(13, 25, 79), -1941176418),
          new BoundedDraws(-1L, 1073741824, List.of(288774956, 471976112, 13174789), -1941176418),
          new BoundedDraws(-1L, 1073741825, List.of(577549913, 943952225, 26349579), -1941176418),
          new BoundedDraws(-1L, 2147483647, List.of(577549913, 943952225, 26349579), -1941176418),
          new BoundedDraws(Long.MIN_VALUE, 1, List.of(0, 0, 0), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 2, List.of(1, 1, 0), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 7, List.of(5, 2, 4), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 10, List.of(0, 8, 9), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 16, List.of(11, 13, 3), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 26, List.of(18, 18, 23), -1690734402),
          new BoundedDraws(Long.MIN_VALUE, 100, List.of(60, 48, 29), -1690734402),
          new BoundedDraws(
              Long.MIN_VALUE, 1073741824, List.of(784870680, 892752974, 258274014), -1690734402),
          new BoundedDraws(
              Long.MIN_VALUE, 1073741825, List.of(516548029, 663681053, 251269761), -1728529858),
          new BoundedDraws(
              Long.MIN_VALUE, 2147483647, List.of(1569741360, 1785505948, 516548029), -1690734402));

  static final List<Rejection> V_L3 =
      List.of(
          new Rejection(0L, 516548029, 3, -1690734402),
          new Rejection(1L, 215764588, 2, 1761283695),
          new Rejection(2L, 630021372, 2, -423279216),
          new Rejection(3L, 152454210, 3, -836442134),
          new Rejection(4L, 467700105, 6, -1376291473),
          new Rejection(5L, 379250092, 2, 379066948),
          new Rejection(6L, 793506876, 2, -1805495963),
          new Rejection(7L, 20678044, 4, 1495978761),
          new Rejection(8L, 32431021, 4, 858120744),
          new Rejection(9L, 542735596, 2, -1003149799),
          new Rejection(10L, 956992380, 2, 1107254586),
          new Rejection(11L, 917721111, 3, 919154311),
          new Rejection(12L, 128478812, 2, 1181413113),
          new Rejection(13L, 706221100, 2, 1909600750),
          new Rejection(14L, 730985498, 5, -1800026279),
          new Rejection(15L, 226612738, 3, 1796952534));
}
