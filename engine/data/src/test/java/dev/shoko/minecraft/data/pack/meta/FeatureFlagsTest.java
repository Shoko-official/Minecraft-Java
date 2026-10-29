package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Expect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R44, R45, R66 (FeatureFlags value type)
class FeatureFlagsTest {

  private static Identifier flag(String id) {
    return Identifier.parse(id);
  }

  private static FeatureFlags flags(String... ids) {
    List<Identifier> list = new ArrayList<>();
    for (String id : ids) {
      list.add(flag(id));
    }
    return FeatureFlags.of(list);
  }

  private static List<String> texts(Collection<Identifier> ids) {
    return ids.stream().map(Identifier::toString).toList();
  }

  @Test
  void provisionalVanillaHoldsOnlyTheVanillaFlag() {
    // spec: R45, R66 a
    FeatureFlags vanilla = FeatureFlags.vanilla();
    assertThat(texts(vanilla.flags())).containsExactly("minecraft:vanilla");
    assertThat(vanilla.contains(flag("minecraft:vanilla"))).isTrue();
    assertThat(vanilla.contains(flag("minecraft:trade_rebalance"))).isFalse();
  }

  @Test
  void provisionalFlagsIterateInCodePointOrderWhateverTheInputOrder() {
    // spec: VF5, R66 e
    FeatureFlags given = flags("minecraft:vanilla", "minecraft:trade_rebalance");
    assertThat(texts(given.flags()))
        .containsExactly("minecraft:trade_rebalance", "minecraft:vanilla");
    FeatureFlags reversed = flags("minecraft:trade_rebalance", "minecraft:vanilla");
    assertThat(texts(reversed.flags()))
        .containsExactly("minecraft:trade_rebalance", "minecraft:vanilla");
    assertThat(texts(flags("x:y", "a:b", "minecraft:z", "a:a").flags()))
        .containsExactly("a:a", "a:b", "minecraft:z", "x:y");
  }

  @Test
  void duplicatesAreMerged() {
    assertThat(flags("a:b", "a:b", "a:b").flags()).hasSize(1);
    assertThat(flags("a:b", "c:d", "a:b").flags()).hasSize(2);
  }

  @Test
  void anEmptySetIsAllowed() {
    FeatureFlags empty = FeatureFlags.of(List.of());
    assertThat(empty.flags()).isEmpty();
    assertThat(empty.contains(flag("minecraft:vanilla"))).isFalse();
  }

  @Test
  void provisionalContainsAllChecksEveryRequiredFlag() {
    // spec: R45, R66 c
    FeatureFlags active = flags("minecraft:vanilla", "minecraft:trade_rebalance");
    assertThat(active.containsAll(flags("minecraft:trade_rebalance"))).isTrue();
    assertThat(active.containsAll(flags("minecraft:vanilla", "minecraft:trade_rebalance")))
        .isTrue();
    assertThat(active.containsAll(flags("minecraft:trade_rebalance", "x:y"))).isFalse();
    assertThat(active.containsAll(flags("x:y"))).isFalse();
    assertThat(active.containsAll(FeatureFlags.of(List.of()))).isTrue();
    assertThat(FeatureFlags.vanilla().containsAll(active)).isFalse();
  }

  @Test
  void provisionalUnionHoldsTheFlagsOfBoth() {
    // spec: R45 (vanilla plus the union of the flags enabled by packs)
    FeatureFlags union = FeatureFlags.vanilla().union(flags("minecraft:trade_rebalance", "x:y"));
    assertThat(texts(union.flags()))
        .containsExactly("minecraft:trade_rebalance", "minecraft:vanilla", "x:y");
    assertThat(texts(FeatureFlags.vanilla().union(FeatureFlags.vanilla()).flags()))
        .containsExactly("minecraft:vanilla");
  }

  @Test
  void unionLeavesTheOperandsUnchanged() {
    FeatureFlags left = flags("a:a");
    FeatureFlags right = flags("b:b");
    FeatureFlags union = left.union(right);
    assertThat(texts(left.flags())).containsExactly("a:a");
    assertThat(texts(right.flags())).containsExactly("b:b");
    assertThat(union.flags()).hasSize(2);
  }

  @Test
  void equalityComparesTheSets() {
    FeatureFlags a = flags("a:a", "b:b");
    FeatureFlags b = flags("b:b", "a:a", "a:a");
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(flags("a:a"));
    assertThat(a).isNotEqualTo(flags("a:a", "b:c"));
    assertThat(FeatureFlags.vanilla()).isEqualTo(flags("minecraft:vanilla"));
    assertThat(a).isNotEqualTo("a").isNotEqualTo(null);
  }

  @Test
  void laterChangesToTheGivenCollectionDoNotAffectTheSet() {
    List<Identifier> source = new ArrayList<>(List.of(flag("a:a")));
    FeatureFlags set = FeatureFlags.of(source);
    source.add(flag("b:b"));
    assertThat(set.flags()).hasSize(1);
  }

  @Test
  void theReturnedCollectionIsUnmodifiable() {
    Collection<Identifier> exposed = flags("a:a").flags();
    Expect.unmodifiable(() -> exposed.add(flag("b:b")));
    Expect.unmodifiable(exposed::clear);
  }
}
