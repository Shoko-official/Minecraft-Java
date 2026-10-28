package dev.shoko.minecraft.data.registry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.FakeAccess;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading K1, R35, R36, R64 (HolderSet: a #tag or an inline list)
class HolderSetTest {

  private static RegistryKey<String> key() {
    return new RegistryKey<>(Identifier.minecraft("block"));
  }

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static List<Identifier> ids(HolderSet<String> set) {
    return set.holders().stream().map(h -> h.id().orElseThrow()).toList();
  }

  private static Holder<String> ref(FakeAccess access, String name) {
    return Holder.reference(key(), id(name), access);
  }

  @Test
  void anInlineListKeepsItsMembersInOrder() {
    FakeAccess access = FakeAccess.create();
    HolderSet<String> set = HolderSet.list(List.of(ref(access, "b"), ref(access, "a")));
    assertThat(ids(set)).containsExactly(id("b"), id("a"));
    assertThat(set.tagKey()).isEmpty();
  }

  @Test
  void provisionalAnInlineListMayBeEmpty() {
    // spec: R64 c (an empty array is a valid empty set)
    HolderSet<String> set = HolderSet.list(List.of());
    assertThat(set.holders()).isEmpty();
    assertThat(set.tagKey()).isEmpty();
  }

  @Test
  void laterChangesToTheGivenListDoNotAffectTheSet() {
    FakeAccess access = FakeAccess.create();
    List<Holder<String>> list = new ArrayList<>(List.of(ref(access, "a")));
    HolderSet<String> set = HolderSet.list(list);
    list.add(ref(access, "b"));
    assertThat(set.holders()).hasSize(1);
  }

  @Test
  void aTagSetNamesItsTag() {
    TagKey<String> tag = new TagKey<>(key(), id("t"));
    HolderSet<String> set = HolderSet.tag(tag, FakeAccess.create());
    assertThat(set.tagKey()).hasValue(tag);
  }

  @Test
  void aTagSetNothingIsLookedUpUntilTheMembersAreRequested() {
    // spec: HolderSet.tag Javadoc
    FakeAccess access = FakeAccess.create().tag(key(), "t", "a", "b");
    HolderSet<String> set = HolderSet.tag(new TagKey<>(key(), id("t")), access);
    assertThat(access.lookups()).isZero();
    assertThat(set.tagKey()).isPresent();
    assertThat(access.lookups()).isZero();
    assertThat(ids(set)).containsExactly(id("a"), id("b"));
    assertThat(access.lookups()).isPositive();
  }

  @Test
  void provisionalATagSetMembersFollowTheResolvedOrder() {
    // spec: R35
    FakeAccess access = FakeAccess.create().tag(key(), "t", "c", "a", "b");
    HolderSet<String> set = HolderSet.tag(new TagKey<>(key(), id("t")), access);
    assertThat(ids(set)).containsExactly(id("c"), id("a"), id("b"));
  }

  @Test
  void provisionalAnEmptyTagIsAnEmptySetNotAnError() {
    // spec: R38 (a tag with no value exists)
    FakeAccess access = FakeAccess.create().tag(key(), "t");
    HolderSet<String> set = HolderSet.tag(new TagKey<>(key(), id("t")), access);
    assertThat(set.holders()).isEmpty();
  }

  @Test
  void provisionalAnAbsentOrFailedTagFailsWhenTheMembersAreRequested() {
    // spec: HolderSet.holders Javadoc, R36 (the access reports no members for a failed tag)
    HolderSet<String> set = HolderSet.tag(new TagKey<>(key(), id("gone")), FakeAccess.create());
    assertThat(set.tagKey()).isPresent();
    Expect.illegalState(set::holders);
  }

  @Test
  void provisionalATagMemberIsAReferenceToTheEntryOfTheSameRegistry() {
    // spec: R33 (a tag refers to entries of its own registry)
    FakeAccess access = FakeAccess.create().tag(key(), "t", "a");
    HolderSet<String> set = HolderSet.tag(new TagKey<>(key(), id("t")), access);
    assertThat(set.holders().get(0)).isEqualTo(Holder.reference(key(), id("a"), access));
  }
}
