package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R3, R5, R66 b (PackMeta of a pack without pack.mcmeta)
class PackMetaTest {

  @Test
  void provisionalTheImplicitDataPackTargetsTheExactDataFormat() {
    // spec: R3 (pack without pack.mcmeta), PackMeta.implicit Javadoc ("the exact target format")
    PackMeta meta = PackMeta.implicit("vanilla", PackType.DATA);
    PackFormatRange range = meta.formats().orElseThrow();
    assertThat(range.min()).isEqualTo(PackType.DATA.targetFormat());
    assertThat(range.max()).isEqualTo(PackType.DATA.targetFormat());
    assertThat(meta.compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalTheImplicitResourcePackTargetsTheExactResourceFormat() {
    // spec: R1, R3
    PackMeta meta = PackMeta.implicit("resources", PackType.RESOURCE);
    PackFormatRange range = meta.formats().orElseThrow();
    assertThat(range.min()).isEqualTo(PackType.RESOURCE.targetFormat());
    assertThat(range.max()).isEqualTo(PackType.RESOURCE.targetFormat());
    assertThat(meta.compatibility(PackType.RESOURCE)).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalTheImplicitPackRequiresOnlyTheVanillaFlag() {
    // spec: R66 b ("le pack vanilla implicite requiert minecraft:vanilla")
    PackMeta meta = PackMeta.implicit("vanilla", PackType.DATA);
    assertThat(meta.features()).isEqualTo(FeatureFlags.vanilla());
  }

  @Test
  void provisionalTheImplicitPackHasNoFilterAndNoOverlay() {
    // spec: PackMeta.implicit Javadoc
    PackMeta meta = PackMeta.implicit("vanilla", PackType.DATA);
    assertThat(meta.filters()).isEmpty();
    assertThat(meta.overlays()).isEmpty();
  }
}
