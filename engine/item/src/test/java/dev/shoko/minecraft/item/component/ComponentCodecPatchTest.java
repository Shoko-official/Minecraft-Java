package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R28, IC-R14, IC-R27: the "components" object of a stack in
// JSON, where a key prefixed by "!" removes a component of the prototype.
class ComponentCodecPatchTest {

  private static final Identifier RARITY = Identifier.minecraft("rarity");
  private static final Identifier LORE = Identifier.minecraft("lore");
  private static final Identifier ITEM_MODEL = Identifier.minecraft("item_model");

  private static ComponentPatch patch(String relaxedJson) {
    return Findings.value(CodecFixture.codec().patchFromJson(Json.parse(relaxedJson)));
  }

  @Test
  void anEmptyObjectIsTheEmptyPatch() {
    // spec: IC-R28
    assertThat(patch("{}").isEmpty()).isTrue();
  }

  @Test
  void aKeyGivesASetOfThatComponent() {
    // spec: IC-R28
    ComponentPatch read = patch("{'minecraft:rarity':'epic'}");
    assertThat(read.changes().keySet()).containsExactly(RARITY);
    assertThat(read.changes().get(RARITY))
        .isEqualTo(new ComponentChange.Set(new ComponentValue(new StringTag("epic"))));
  }

  @Test
  void aKeyPrefixedByABangGivesARemoval() {
    // spec: IC-R28 ("accepte la cle prefixee !")
    ComponentPatch read = patch("{'!minecraft:lore':{}}");
    assertThat(read.changes().keySet()).containsExactly(LORE);
    assertThat(read.changes().get(LORE)).isEqualTo(new ComponentChange.Removed());
  }

  @Test
  void setsAndRemovalsKeepTheOrderOfTheFile() {
    // spec: IC-R28
    ComponentPatch read =
        patch(
            "{'minecraft:item_model':'minecraft:dirt','!minecraft:lore':{},"
                + "'minecraft:rarity':'rare'}");
    assertThat(read.changes().keySet()).containsExactly(ITEM_MODEL, LORE, RARITY);
  }

  @Test
  void aValueIsDecodedLikeAValueAlone() {
    // spec: IC-R28, IC-R12
    ComponentCodec codec = CodecFixture.codec();
    ComponentPatch read =
        patch("{'minecraft:food':{'nutrition':5,'saturation':6.0},'minecraft:rarity':'epic'}");
    assertThat(read.changes().get(Identifier.minecraft("food")))
        .isEqualTo(
            new ComponentChange.Set(
                CodecFixture.decode(codec, "food", "{'nutrition':5,'saturation':6.0}")));
  }

  @Test
  void appliedToAPrototypeItReplacesAndRemoves() {
    // spec: IC-R14, IC-R28
    ComponentMap prototype = TestItems.type("stone").prototype();
    ComponentPatch read = patch("{'minecraft:rarity':'epic','!minecraft:lore':{}}");
    ComponentMap effective = prototype.apply(read);
    assertThat(effective.get(RARITY)).contains(new ComponentValue(new StringTag("epic")));
    assertThat(effective.contains(LORE)).isFalse();
    assertThat(effective.contains(ITEM_MODEL)).isTrue();
  }

  @Test
  void provisionalAnUnregisteredComponentIsRefused() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.patchFromJson(Json.parse("{'minecraft:not_a_component':{}}")),
        FindingCode.UNKNOWN_COMPONENT);
    Findings.assertFailure(
        codec.patchFromJson(Json.parse("{'!minecraft:not_a_component':{}}")),
        FindingCode.UNKNOWN_COMPONENT);
    Findings.assertFailure(
        codec.patchFromJson(Json.parse("{'minecraft:rarity':'epic','minecraft:nope':1}")),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalABadValueFailsThePatch() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.patchFromJson(Json.parse("{'minecraft:food':{'nutrition':5}}")),
        FindingCode.MISSING_FIELD);
    Findings.assertFailure(
        codec.patchFromJson(Json.parse("{'minecraft:rarity':5}")), FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAnythingButAnObjectIsNotAComponentsObject() {
    // spec: IC-R28 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    for (String text : new String[] {"[]", "'minecraft:rarity'", "3", "null"}) {
      Findings.assertAnyFailure(codec.patchFromJson(Json.parse(text)));
    }
  }

  @Test
  void provisionalAKeyThatIsNotAnIdentifierIsRefused() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertAnyFailure(codec.patchFromJson(Json.parse("{'Not An Id':1}")));
    Findings.assertAnyFailure(codec.patchFromJson(Json.parse("{'!':{}}")));
  }

  @Test
  void provisionalAPatchFromJsonWritesToNbtAndReadsBackEqual() {
    // spec: IC-R27, IC-R28 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    ComponentPatch read =
        patch(
            "{'minecraft:rarity':'epic','!minecraft:lore':{},"
                + "'minecraft:food':{'nutrition':5,'saturation':6.0}}");
    assertThat(Findings.value(codec.patchFromNbt(codec.patchToNbt(read)))).isEqualTo(read);
  }
}
