package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.container.NestedContents;
import dev.shoko.minecraft.item.container.SlottedStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import dev.shoko.minecraft.item.testing.Findings;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R23 (probe PI4): container, bundle_contents and
// charged_projectiles carry nested stacks, and the depth limit of the caller applies to all three
// (finding 15), and a slot of the container component is a valid index (defect D2).
class NestingBoundsHuntTest {

  private static NestedContents view(int maxDepth) {
    return NestedContents.of(StackCodec.of(HuntFixtures.catalog()), maxDepth);
  }

  private static CompoundTag stack(String item, String component, NbtTag contents) {
    LinkedHashMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("id", new StringTag("minecraft:" + item));
    entries.put("count", new IntTag(1));
    if (contents != null) {
      LinkedHashMap<String, NbtTag> components = new LinkedHashMap<>();
      components.put("minecraft:" + component, contents);
      entries.put("components", CompoundTag.of(components));
    }
    return CompoundTag.of(entries);
  }

  private static CompoundTag stoneWithoutComponents() {
    LinkedHashMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("id", new StringTag("minecraft:stone"));
    entries.put("count", new IntTag(1));
    return CompoundTag.of(entries);
  }

  /**
   * A container value whose single stack is the outermost of a chain: {@code levels} stacks in
   * total, the innermost one being a stone; every holder in between carries the next stack in
   * {@code component} as a list of plain stacks.
   */
  private static ComponentValue plainChain(String holder, String component, int levels) {
    CompoundTag inner = stoneWithoutComponents();
    for (int level = 1; level < levels; level++) {
      inner = stack(holder, component, ListTag.of(List.of(inner)));
    }
    LinkedHashMap<String, NbtTag> top = new LinkedHashMap<>();
    top.put("slot", new IntTag(0));
    top.put("item", inner);
    return new ComponentValue(ListTag.of(List.of(CompoundTag.of(top))));
  }

  @Test
  void provisionalABundleChainWithinTheLimitIsRead() {
    // spec: IC-R23 (the fixture itself is valid: three levels under a limit of three)
    Findings.value(view(3).read(plainChain("bundle", "bundle_contents", 3)));
    Findings.value(view(3).read(plainChain("crossbow", "charged_projectiles", 3)));
  }

  @Test
  void provisionalABundleChainOverTheLimitIsRefused() {
    // spec: IC-R23 (finding 15)
    Decoding<List<SlottedStack>> tooDeep = view(3).read(plainChain("bundle", "bundle_contents", 4));
    Findings.assertFailure(tooDeep, FindingCode.NESTING_TOO_DEEP);
  }

  @Test
  void provisionalACrossbowChainOverTheLimitIsRefused() {
    // spec: IC-R23 (finding 15)
    Decoding<List<SlottedStack>> tooDeep =
        view(3).read(plainChain("crossbow", "charged_projectiles", 4));
    Findings.assertFailure(tooDeep, FindingCode.NESTING_TOO_DEEP);
  }

  @Test
  void provisionalAVeryDeepBundleChainIsAFindingNotAStackOverflow() {
    // spec: IC-R23 (finding 15 and defect D3)
    Decoding<List<SlottedStack>> deep =
        view(100).read(plainChain("bundle", "bundle_contents", 5000));
    Findings.assertFailure(deep, FindingCode.NESTING_TOO_DEEP);
  }

  @Test
  void provisionalAStackWithNoNestedContentIsDepthOne() {
    // spec: IC-R23
    ComponentValue single = plainChain("bundle", "bundle_contents", 1);
    assertThat(Findings.value(view(1).read(single))).hasSize(1);
  }
}
