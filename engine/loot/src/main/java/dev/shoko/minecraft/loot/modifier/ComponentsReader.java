package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootFindingCode;

/** Reads the {@code components} of {@code set_components} with the codec of {@code item}. */
final class ComponentsReader {

  private final ComponentCodec codec;
  private final Findings findings;

  ComponentsReader(ItemCatalog items, Findings findings) {
    this.codec = ComponentCodec.of(items.components());
    this.findings = findings;
  }

  ModifierAction setComponents(Fields fields) {
    JsonNode node = fields.required("components");
    if (node == null) {
      return new ModifierAction.SetComponents(ComponentPatch.empty());
    }
    Decoding<ComponentPatch> decoded = codec.patchFromJson(node);
    for (Finding finding : decoded.findings()) {
      if (finding.code().severity() == Severity.ERROR) {
        findings.add(map(finding), node, finding.path() + ": " + finding.message());
      }
    }
    if (decoded instanceof Decoding.Success<ComponentPatch> success) {
      return new ModifierAction.SetComponents(success.value());
    }
    return new ModifierAction.SetComponents(ComponentPatch.empty());
  }

  private static LootFindingCode map(Finding finding) {
    return switch (finding.code()) {
      case MISSING_FIELD -> LootFindingCode.MISSING_FIELD;
      case UNKNOWN_FIELD -> LootFindingCode.UNKNOWN_FIELD;
      case VALUE_RANGE -> LootFindingCode.VALUE_RANGE;
      case INVALID_IDENTIFIER -> LootFindingCode.INVALID_IDENTIFIER;
      case UNKNOWN_COMPONENT -> LootFindingCode.UNKNOWN_TYPE;
      default -> LootFindingCode.TYPE_MISMATCH;
    };
  }
}
