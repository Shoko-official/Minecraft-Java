package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R29, probes PI2 and PI11 (text form of the item_stack
// argument of commands). Level S: every test is provisional.
class StackCodecTextTest {

  private static StackCodec codec() {
    return TestItems.stackCodec();
  }

  private static Decoding<ItemStack> read(String text) {
    return codec().fromCommandText(text);
  }

  @Test
  void provisionalABareIdentifierIsAOneUnitStack() {
    // spec: IC-R29
    ItemStack stack = Findings.value(read("minecraft:stone"));
    assertThat(stack).isEqualTo(TestItems.stack("stone", 1));
    assertThat(stack.count()).isEqualTo(1);
  }

  @Test
  void provisionalAComponentIsSetWithASnbtValue() {
    // spec: IC-R29
    ItemStack stack = Findings.value(read("minecraft:stone[minecraft:rarity=\"epic\"]"));
    assertThat(stack).isEqualTo(StackSupport.withRarity("stone", 1, "epic"));
  }

  @Test
  void provisionalABangRemovesAComponentOfThePrototype() {
    // spec: IC-R29, IC-R27
    ItemStack stack = Findings.value(read("minecraft:stone[!minecraft:rarity]"));
    assertThat(stack.component(StackSupport.rarityId())).isEmpty();
    assertThat(stack)
        .isEqualTo(TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()));
  }

  @Test
  void provisionalSeveralEntriesAreSeparatedByCommas() {
    // spec: IC-R29
    ItemStack stack =
        Findings.value(read("minecraft:stone[minecraft:rarity=\"epic\",!minecraft:lore]"));
    assertThat(stack.component(StackSupport.rarityId())).contains(StackSupport.text("epic"));
    assertThat(stack.component(TestItems.id("lore"))).isEmpty();
  }

  @Test
  void provisionalSpacesAfterCommasAreAccepted() {
    // spec: IC-R29
    ItemStack tight =
        Findings.value(read("minecraft:stone[minecraft:rarity=\"epic\",!minecraft:lore]"));
    ItemStack spaced =
        Findings.value(read("minecraft:stone[minecraft:rarity=\"epic\", !minecraft:lore]"));
    assertThat(spaced).isEqualTo(tight);
  }

  @Test
  void provisionalAnObjectValueIsReadAsSnbt() {
    // spec: IC-R29, probe PI11 (custom_data={a:1b}, S shape of the component)
    ItemStack stack = Findings.value(read("minecraft:stone[minecraft:custom_data={a:1b}]"));
    assertThat(stack.component(TestItems.id("custom_data"))).isPresent();
  }

  @Test
  void provisionalAnUnclosedBracketIsASyntaxError() {
    // spec: IC-R29 (Javadoc of fromCommandText: TEXT_SYNTAX)
    Findings.assertFailure(
        read("minecraft:stone[minecraft:rarity=\"epic\""), FindingCode.TEXT_SYNTAX);
    Findings.assertFailure(read("minecraft:stone["), FindingCode.TEXT_SYNTAX);
  }

  @Test
  void provisionalAComponentWithoutValueIsASyntaxError() {
    // spec: IC-R29
    Findings.assertFailure(read("minecraft:stone[minecraft:rarity]"), FindingCode.TEXT_SYNTAX);
  }

  @Test
  void provisionalTrailingTextIsASyntaxError() {
    // spec: IC-R29
    Findings.assertFailure(read("minecraft:stone xyz"), FindingCode.TEXT_SYNTAX);
    Findings.assertFailure(read("minecraft:stone[]]"), FindingCode.TEXT_SYNTAX);
    Findings.assertFailure(
        read("minecraft:stone[minecraft:rarity=\"epic\"]x"), FindingCode.TEXT_SYNTAX);
  }

  @Test
  void provisionalAnEmptyTextIsAFailure() {
    // spec: IC-R29 (the code is not specified: empty identifier or syntax)
    Findings.assertAnyFailure(read(""));
    Findings.assertAnyFailure(read("[minecraft:rarity=\"epic\"]"));
  }

  @Test
  void provisionalAnUnknownItemIsAFailure() {
    // spec: IC-R29
    Findings.assertAnyFailure(read("minecraft:inconnu"));
  }

  @Test
  void provisionalAnUnknownComponentIsAFailure() {
    // spec: IC-R29, IC-R10 (the code is the one of the component or a syntax error)
    Decoding<ItemStack> decoding = read("minecraft:stone[minecraft:inconnu=1]");
    Findings.assertAnyFailure(decoding);
    assertThat(Findings.codesOf(decoding))
        .containsAnyOf(FindingCode.UNKNOWN_COMPONENT, FindingCode.TEXT_SYNTAX);
  }

  @Test
  void provisionalAnUnterminatedStringIsAFailure() {
    // spec: IC-R29
    Findings.assertAnyFailure(read("minecraft:stone[minecraft:rarity=\"epic]"));
  }

  @Test
  void provisionalTheCountIsNotPartOfTheTextOfAStack() {
    // spec: IC-R29 (Javadoc of toCommandText: the identifier, no count)
    assertThat(codec().toCommandText(TestItems.stack("stone", 5))).isEqualTo("minecraft:stone");
    assertThat(codec().toCommandText(TestItems.stack("stone", 1))).isEqualTo("minecraft:stone");
  }

  @Test
  void provisionalModificationsAreWrittenInBrackets() {
    // spec: IC-R29
    String set = codec().toCommandText(StackSupport.withRarity("stone", 2, "epic"));
    assertThat(set).startsWith("minecraft:stone[").endsWith("]").contains("minecraft:rarity");
    String removed =
        codec()
            .toCommandText(TestItems.stack("stone", 2).withoutComponent(StackSupport.rarityId()));
    assertThat(removed).startsWith("minecraft:stone[").endsWith("]").contains("!minecraft:rarity");
  }

  @Test
  void provisionalWritingTheEmptyStackIsRefused() {
    // spec: IC-R15 (Javadoc of toCommandText)
    assertThatIllegalArgumentException().isThrownBy(() -> codec().toCommandText(ItemStack.empty()));
  }

  @Test
  void provisionalTheTextOfAStackReadsBackToTheSameContent() {
    // spec: IC-R29 (the count is dropped, the content is kept)
    List<ItemStack> stacks =
        List.of(
            TestItems.stack("stone", 3),
            StackSupport.withRarity("stone", 64, "epic"),
            TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()),
            TestItems.stack("stone_pickaxe", 1, "damage", "17"),
            TestItems.stack(
                "potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"),
            TestItems.stack("bread", 2, "food", "{'nutrition':1,'saturation':-0.0}"),
            TestItems.stack("stone", 5, "enchantments", "{'minecraft:sharpness':3}"));
    for (ItemStack stack : stacks) {
      String text = codec().toCommandText(stack);
      ItemStack back = Findings.value(read(text));
      assertThat(back.sameContent(stack)).as(text).isTrue();
      assertThat(back.count()).isEqualTo(1);
    }
  }
}
