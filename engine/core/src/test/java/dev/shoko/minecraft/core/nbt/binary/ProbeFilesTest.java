package dev.shoko.minecraft.core.nbt.binary;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.Hex;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Checks the test fixtures themselves against the sizes and SHA-256 digests printed in {@code
 * core-nbt-vectors.md}, so that a wrong fixture cannot make a reader test pass or fail.
 */
class ProbeFilesTest {

  @Test
  void f1MatchesSpecDigest() {
    // spec: core-nbt-vectors F1 (148 bytes)
    byte[] f1 = ProbeFiles.f1();
    assertThat(f1).hasSize(148);
    assertThat(Hex.sha256(f1))
        .isEqualTo("ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f");
    assertThat(f1[ProbeFiles.ENTITIES_ELEMENT_TYPE - 1]).isEqualTo((byte) 's');
    assertThat(f1[ProbeFiles.ROOT_END]).isZero();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "p00_base, 148, ea73f15eba7e75a69fc179de24b07781b1db2a5637af0cc9b50bbf029a24ea3f",
    "p01_root_name_x, 149, 646f136e7e92fbe2182c4d09387b104fe9e8685f70c0a129a30aaf77501c62ec",
    "p02_entities_end_len_minus1, 148,"
        + " f6442a15cc52fb643ae565398436d5596261e7018fa91d1f336aee435bee8642",
    "p03_entities_byte_len_minus1, 148,"
        + " d8e8cf2856b6d735891a4a9f700c3b6dda58fff9d0d5cb983de8faaac977b818",
    "p04_entities_end_len_1, 148, 700d0952ef84b800980ef05a95049aa40977f08bc61a23aba51eb3c0d32f599e",
    "p05_entities_byte_len_0, 148, 60663135c381b4e48a734b7210362d1158d03747ff3eb6198f020e469c828db0",
    "p06_trailing_byte, 149, ce63e07bbc061f31939ce2ea19478983bdb2467aaa68a1604a71d0c421e19337",
    "p07_truncated, 147, ea0714ca494b98feeb75f14d79db19b54a5540f9a6606b8855e31533c69e1411",
    "p08_extra_unknown_type_13, 152,"
        + " a6ced3e0eb7b8ec904c24dc07384515b78d4599ccfc7459727ec93ad0e607bd5",
    "p09_extra_string_raw_nul, 155,"
        + " 9f178baba0ee75426b3389b22d4a1b2e80afa6de9892c1ac513dcc0fabc301f2",
    "p10_extra_string_utf8_4byte, 158,"
        + " b4a2466ac809b4e05bb63b99e672f2381611dc7cab536e9576811e1ba8faf23d",
    "p11_extra_string_mutf8_pair, 160,"
        + " 606a0c580a90eb38a8709b2f0cf24c02b36a05dd35354b107fe79303ea9a82c7",
    "p12_extra_string_lone_surrogate, 157,"
        + " ebd6350053c0a537daaf85be2089cfbe9a486f61dc6d0ac6d630639db45d15a4",
    "p13_extra_bytearray_len_minus1, 156,"
        + " bb6d0f89375c432a61638c71a868df2a0cfe23ae1e8cf8371b33c96f25bb1d6a",
    "p14_duplicate_palette_id_stone, 170,"
        + " e7c1f288d548c97e0e8c289f3830e0f7cb85dafd0db9df7d36aaea26d480ca21",
    "p15_extra_deep_510, 2702, 0b577bef3e5bc8ea3d9fe756bbdad0c2030f606c0bbd850aac54123a89d40513",
    "p16_extra_deep_511, 2707, 4cb1e374ed001156f650c0e52916448fa368276b29c4b17071d77496a5e94999",
    "p17_extra_deep_512, 2712, 794902ec24a490f59d86f3294f17ba31f65fb02e0c7e7f5727cf83ab304d7939",
    "p18_extra_deep_513, 2717, 20893c718b06789bb103ec041fa13125ad9e529bd31bb9e1d0844e8cac4e03e8"
  })
  void probeMatchesSpecDigest(String id, int size, String sha256) {
    // spec: core-nbt-vectors PB1 table
    byte[] probe = ProbeFiles.probe(id);
    assertThat(probe).hasSize(size);
    assertThat(Hex.sha256(probe)).isEqualTo(sha256);
  }

  @Test
  void p19MatchesSpecDigest() {
    // spec: core-nbt-vectors PB1 table, p19 (67 109 020 bytes)
    assertThat(Hex.sha256(ProbeFiles.p19Stream()))
        .isEqualTo("567fb12d47b6e603f4d13f89583d9b69fb641c6c0f3454acba2a43ab3580a2aa");
  }

  @Test
  void p20MatchesSpecDigest() {
    // spec: core-nbt-vectors PB1 table, p20 (20 000 157 bytes)
    assertThat(Hex.sha256(ProbeFiles.p20Stream()))
        .isEqualTo("13d8131eaf3eaba05e3cd8f2441b1d6b17504079f8eb58363e029936e73f4b53");
  }

  @Test
  void storedContainersMatchSpecDigests() {
    // spec: core-nbt-vectors table C (C08 to C12), built from F1 with stored DEFLATE blocks
    byte[] gzip = Containers.gzipStored(ProbeFiles.f1());
    byte[] zlib = Containers.zlibStored(ProbeFiles.f1());
    assertThat(gzip).hasSize(171);
    assertThat(Hex.sha256(gzip))
        .isEqualTo("df59c0fb96f2764d7e1dbda1d623a84575778d2c9ef0d740336f78054fdbf0b5");
    assertThat(Hex.sha256(ProbeFiles.c09()))
        .isEqualTo("85194b67e4cc2736f9ccad9766c44c43c58a5723c3304a0160d3d9fd2be87487");
    assertThat(zlib).hasSize(159);
    assertThat(Hex.sha256(zlib))
        .isEqualTo("4373a2cd155f013bab1ad836df8050b5e79847e1793b64e071679c95908e5f37");
    assertThat(Hex.sha256(Containers.invert(zlib, 155, 159)))
        .isEqualTo("caceeba85434e249d316e0ab5b4eb9e0b8b8b71aad38c95f8b299919e0b0b8d3");
    assertThat(Hex.sha256(Arrays.copyOf(zlib, 155)))
        .isEqualTo("80399581468da3d89fedc7d09ee717c95c0989c2df5a312d706bf03b73ea009e");
  }

  @Test
  void writerDepthVectorW4MatchesSpecDigest() {
    // spec: core-nbt-vectors W4 (2571 bytes): expected output of the writer, built by hand
    byte[] w4 = BinaryVectors.w4Bytes();
    assertThat(w4).hasSize(2571);
    assertThat(Hex.sha256(w4))
        .isEqualTo("a3a60b2570cb6abc9e115e64bc8579ee4f92ff42b6233f07b84edfe5425e6169");
  }

  @Test
  void nestedListVectorsMatchSpecDigests() {
    // spec: core-nbt-vectors E09a (2563 bytes) and E09b (2568 bytes)
    assertThat(BinaryVectors.nestedListsRoot(512)).hasSize(2563);
    assertThat(Hex.sha256(BinaryVectors.nestedListsRoot(512)))
        .isEqualTo("c577920edeaead13ad505ca09a1859049301d88e58ecb8a53c7e10cae672d02e");
    assertThat(BinaryVectors.nestedListsRoot(513)).hasSize(2568);
    assertThat(Hex.sha256(BinaryVectors.nestedListsRoot(513)))
        .isEqualTo("c6ca4e050c4cdab8eb7f9cd9924855aba1f9de7fd2539de59a8e34b3b45c72cd");
  }
}
