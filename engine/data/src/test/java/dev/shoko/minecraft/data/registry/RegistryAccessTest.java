package dev.shoko.minecraft.data.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import org.junit.jupiter.api.Test;

// spec: RegistryAccess.empty Javadoc (an access that knows nothing: no registry, no tag)
class RegistryAccessTest {

  @Test
  void theEmptyAccessKnowsNoEntryNoRegistryAndNoTag() {
    RegistryKey<String> blocks = new RegistryKey<>(Identifier.minecraft("block"));
    RegistryAccess access = RegistryAccess.empty();
    assertThat(access.contains(blocks, Identifier.minecraft("stone"))).isFalse();
    assertThat(access.registry(blocks)).isEmpty();
    assertThat(access.tagMembers(new TagKey<>(blocks, Identifier.minecraft("logs")))).isEmpty();
  }

  @Test
  void aTagKeyAndARegistryKeyAreValuesCompared() {
    RegistryKey<String> one = new RegistryKey<>(Identifier.minecraft("block"));
    RegistryKey<String> two = new RegistryKey<>(Identifier.parse("block"));
    assertThat(one).isEqualTo(two);
    assertThat(new TagKey<>(one, Identifier.parse("t")))
        .isEqualTo(new TagKey<>(two, Identifier.parse("minecraft:t")));
    assertThat(new TagKey<>(one, Identifier.parse("t")))
        .isNotEqualTo(new TagKey<>(one, Identifier.parse("u")));
    assertThat(new TagKey<>(one, Identifier.parse("t")))
        .isNotEqualTo(
            new TagKey<>(new RegistryKey<String>(Identifier.parse("item")), Identifier.parse("t")));
  }
}
