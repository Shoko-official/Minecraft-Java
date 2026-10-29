package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.CodecFixture.Sample;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Nbt;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R24, IC-R27, IC-R10, IC-R14: component values and the
// components compound of a stack in NBT. No test pins the NBT type of a decoded field: values are
// compared by round trips and, for string components, by their text.
class ComponentCodecNbtTest {

  private static final Identifier RARITY = Identifier.minecraft("rarity");
  private static final Identifier LORE = Identifier.minecraft("lore");
  private static final Identifier ITEM_MODEL = Identifier.minecraft("item_model");

  private static ComponentValue text(String value) {
    return new ComponentValue(new StringTag(value));
  }

  @Test
  void aStringComponentIsReadFromAStringTag() {
    // spec: IC-R4 (rarity is a string)
    ComponentCodec codec = CodecFixture.codec();
    ComponentValue value = Findings.value(codec.valueFromNbt(RARITY, new StringTag("epic")));
    assertThat(value).isEqualTo(text("epic"));
    assertThat(value).isEqualTo(CodecFixture.decode(codec, "rarity", "'epic'"));
  }

  @Test
  void anEmptyCompoundIsAValidEnchantmentsValue() {
    // spec: IC-R4 (enchantments {})
    ComponentCodec codec = CodecFixture.codec();
    ComponentValue value =
        Findings.value(
            codec.valueFromNbt(Identifier.minecraft("enchantments"), CompoundTag.empty()));
    assertThat(value.tag()).isEqualTo(CompoundTag.empty());
  }

  @Test
  void theCanonicalTagOfADecodedValueDecodesToTheSameValue() {
    // spec: ComponentValue (canonical NBT form), IC-R18
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      ComponentValue value = CodecFixture.decode(codec, sample.component(), sample.json());
      var again = codec.valueFromNbt(CodecFixture.id(sample.component()), value.tag());
      assertThat(Findings.value(again)).as("%s", sample).isEqualTo(value);
    }
  }

  @Test
  void aValueReadFromNbtWritesBackToJsonAndReadsBackToTheSameValue() {
    // spec: ComponentCodec.valueToJson, IC-R18
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      Identifier id = CodecFixture.id(sample.component());
      ComponentValue value = CodecFixture.decode(codec, sample.component(), sample.json());
      ComponentValue fromNbt = Findings.value(codec.valueFromNbt(id, value.tag()));
      var json = Json.parseStrict(codec.valueToJson(id, fromNbt));
      assertThat(Findings.value(codec.valueFromJson(id, json))).as("%s", sample).isEqualTo(value);
    }
  }

  @Test
  void provisionalAnUnregisteredComponentIsRefused() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.valueFromNbt(Identifier.minecraft("not_a_component"), CompoundTag.empty()),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalAWrongKindOfTagIsATypeMismatch() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.valueFromNbt(Identifier.minecraft("food"), new StringTag("bread")),
        FindingCode.TYPE_MISMATCH);
    Findings.assertFailure(codec.valueFromNbt(RARITY, ListTag.empty()), FindingCode.TYPE_MISMATCH);
    Findings.assertFailure(
        codec.valueFromNbt(RARITY, CompoundTag.empty()), FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAFieldOutsideTheShapeIsRefusedInNbtToo() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.valueFromNbt(
            Identifier.minecraft("villager_food"), Nbt.compound("{nutrition:4,invented:1b}")),
        FindingCode.UNKNOWN_FIELD);
  }

  @Test
  void provisionalAMissingRequiredFieldIsRefusedInNbtToo() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.valueFromNbt(Identifier.minecraft("villager_food"), CompoundTag.empty()),
        FindingCode.MISSING_FIELD);
  }

  @Test
  void aComponentsCompoundGivesSetsAndRemovalsInKeyOrder() {
    // spec: IC-R24, IC-R27: "!id" removes a component of the prototype
    ComponentCodec codec = CodecFixture.codec();
    CompoundTag compound =
        Nbt.compound(
            "{\"minecraft:rarity\":\"epic\",\"!minecraft:lore\":{},\"minecraft:item_model\":\"minecraft:dirt\"}");
    ComponentPatch patch = Findings.value(codec.patchFromNbt(compound));
    assertThat(patch.changes().keySet()).containsExactly(RARITY, LORE, ITEM_MODEL);
    assertThat(patch.changes().get(RARITY)).isEqualTo(new ComponentChange.Set(text("epic")));
    assertThat(patch.changes().get(LORE)).isEqualTo(new ComponentChange.Removed());
    assertThat(patch.changes().get(ITEM_MODEL))
        .isEqualTo(new ComponentChange.Set(text("minecraft:dirt")));
  }

  @Test
  void anEmptyComponentsCompoundIsTheEmptyPatch() {
    // spec: IC-R26 ("components" absent or empty means no modification)
    ComponentCodec codec = CodecFixture.codec();
    assertThat(Findings.value(codec.patchFromNbt(CompoundTag.empty())).isEmpty()).isTrue();
  }

  @Test
  void provisionalAnUnregisteredComponentFailsTheWholeCompound() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.patchFromNbt(
            Nbt.compound("{\"minecraft:rarity\":\"epic\",\"minecraft:not_a_component\":{}}")),
        FindingCode.UNKNOWN_COMPONENT);
    Findings.assertFailure(
        codec.patchFromNbt(Nbt.compound("{\"!minecraft:not_a_component\":{}}")),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalABadValueFailsTheWholeCompound() {
    // spec: IC-R12 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.patchFromNbt(Nbt.compound("{\"minecraft:rarity\":[1]}")), FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalThePatchIsWrittenWithSetsUnderTheIdAndRemovalsUnderABang() {
    // spec: IC-R27 (provisional): "!id" with {} for a removal, in the order of the patch
    ComponentCodec codec = CodecFixture.codec();
    SequencedMap<Identifier, ComponentChange> changes = new LinkedHashMap<>();
    changes.put(RARITY, new ComponentChange.Set(text("epic")));
    changes.put(LORE, new ComponentChange.Removed());
    changes.put(ITEM_MODEL, new ComponentChange.Set(text("minecraft:dirt")));
    CompoundTag written = codec.patchToNbt(new ComponentPatch(changes));
    assertThat(written.entries().keySet())
        .containsExactly("minecraft:rarity", "!minecraft:lore", "minecraft:item_model");
    assertThat(written.get("minecraft:rarity")).contains(new StringTag("epic"));
    assertThat(written.get("!minecraft:lore")).contains(CompoundTag.empty());
    assertThat(written.get("minecraft:item_model")).contains(new StringTag("minecraft:dirt"));
  }

  @Test
  void provisionalAnEmptyPatchIsAnEmptyCompound() {
    // spec: IC-R27 (provisional): components written only with at least one modification
    ComponentCodec codec = CodecFixture.codec();
    assertThat(codec.patchToNbt(ComponentPatch.empty())).isEqualTo(CompoundTag.empty());
  }

  @Test
  void provisionalAWrittenPatchReadsBackToTheSamePatch() {
    // spec: IC-R27 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    ComponentPatch patch =
        ComponentPatch.empty()
            .withSet(RARITY, text("rare"))
            .withRemoved(LORE)
            .withSet(
                Identifier.minecraft("food"),
                CodecFixture.decode(codec, "food", "{'nutrition':5,'saturation':6.0}"))
            .withRemoved(Identifier.minecraft("enchantments"));
    ComponentPatch again = Findings.value(codec.patchFromNbt(codec.patchToNbt(patch)));
    assertThat(again).isEqualTo(patch);
    assertThat(again.changes().keySet()).containsExactlyElementsOf(patch.changes().keySet());
  }

  @Test
  void provisionalEverySampleSurvivesAPatchRoundTrip() {
    // spec: IC-R27 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      Identifier id = CodecFixture.id(sample.component());
      ComponentPatch patch =
          ComponentPatch.empty()
              .withSet(id, CodecFixture.decode(codec, sample.component(), sample.json()));
      assertThat(Findings.value(codec.patchFromNbt(codec.patchToNbt(patch))))
          .as("%s", sample)
          .isEqualTo(patch);
    }
  }

  @Test
  void provisionalAValueOfTheWrongKindUnderAnIdentifierIsNotASet() {
    // spec: IC-R12 (provisional): an Int where rarity wants a string
    ComponentCodec codec = CodecFixture.codec();
    CompoundTag compound = Nbt.compound("{\"minecraft:rarity\":3}");
    Findings.assertFailure(codec.patchFromNbt(compound), FindingCode.TYPE_MISMATCH);
  }
}
