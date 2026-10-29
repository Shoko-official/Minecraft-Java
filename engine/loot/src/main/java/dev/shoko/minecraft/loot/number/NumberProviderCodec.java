package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Decodes number providers from their JSON form (LT-R50). A number is a constant, an object has a
 * {@code type} among the registered types, a string is a reference to a named provider. Findings
 * carry the JSON path.
 *
 * <p>Shapes not shown by the data (provisional): {@code add}, {@code mul}, {@code min}, {@code max}
 * and {@code avg} read an {@code inputs} list, {@code sub}, {@code div}, {@code mod}, {@code
 * floor_div} and {@code floor_mod} read {@code left} and {@code right}, {@code pow} reads {@code
 * base} and {@code exponent}, the unary operators and the conversions read {@code input} (LT-R79,
 * provisional).
 */
public final class NumberProviderCodec {

  private NumberProviderCodec() {}

  /**
   * Decodes one integer provider.
   *
   * @param node the JSON value
   * @param access the loaded registries and tags
   * @return the provider; {@code UNKNOWN_TYPE} for a type outside the 23, {@code TYPE_MISMATCH} or
   *     {@code MISSING_FIELD} for a wrong shape. A reference is not checked here: the loader checks
   *     it once every provider is read ({@code UNRESOLVED_REFERENCE})
   */
  public static LootDecoding<IntProvider> decodeInt(JsonNode node, RegistryAccess access) {
    return new Decoders(access).ints.decode(node);
  }

  /**
   * Decodes one float provider.
   *
   * @param node the JSON value
   * @param access the loaded registries and tags
   * @return the provider; failures as in {@link #decodeInt}, with the 28 float types
   */
  public static LootDecoding<FloatProvider> decodeFloat(JsonNode node, RegistryAccess access) {
    return new Decoders(access).floats.decode(node);
  }

  /**
   * Decodes the two registries of named providers.
   *
   * @param ints the {@code context_int_provider} elements
   * @param floats the {@code context_float_provider} elements
   * @param access the loaded registries and tags
   * @return the set; a provider that fails to decode is absent and a reference to it is {@code
   *     UNRESOLVED_REFERENCE}. Findings are listed per element, in identifier order
   */
  public static LootDecoding<NumberProviderSet> decodeSet(
      ElementRegistryData ints, ElementRegistryData floats, RegistryAccess access) {
    Findings findings = new Findings();
    SequencedMap<Identifier, IntProvider> intMap = new LinkedHashMap<>();
    for (Map.Entry<Identifier, JsonNode> element : ints.elements().entrySet()) {
      LootDecoding<IntProvider> decoded = decodeInt(element.getValue(), access);
      findings.addAll(prefixed(element.getKey(), decoded.findings()));
      if (decoded instanceof LootDecoding.Success<IntProvider> success) {
        intMap.put(element.getKey(), success.value());
      }
    }
    SequencedMap<Identifier, FloatProvider> floatMap = new LinkedHashMap<>();
    for (Map.Entry<Identifier, JsonNode> element : floats.elements().entrySet()) {
      LootDecoding<FloatProvider> decoded = decodeFloat(element.getValue(), access);
      findings.addAll(prefixed(element.getKey(), decoded.findings()));
      if (decoded instanceof LootDecoding.Success<FloatProvider> success) {
        floatMap.put(element.getKey(), success.value());
      }
    }
    NumberProviderSet set = new NumberProviderSet(intMap, floatMap);
    findings.addAll(ProviderReferences.unresolved(set));
    return findings.result(set);
  }

  /**
   * Lists the identifiers of the registered types of one family.
   *
   * @param floats {@code true} for the 28 float types, {@code false} for the 23 integer types
   * @return the type identifiers in registry order
   */
  public static List<Identifier> registeredTypes(boolean floats) {
    return (floats ? Decoders.FLOAT_TYPES : Decoders.INT_TYPES)
        .stream().map(Identifier::minecraft).toList();
  }

  /**
   * Lists the references of named providers that no loaded provider answers.
   *
   * @param set the named providers
   * @return one {@code UNRESOLVED_REFERENCE} finding per missing target
   */
  public static List<LootFinding> unresolvedReferences(NumberProviderSet set) {
    return ProviderReferences.unresolved(set);
  }

  private static List<LootFinding> prefixed(Identifier element, List<LootFinding> findings) {
    return findings.stream()
        .map(f -> new LootFinding(f.code(), f.path(), element + ": " + f.message()))
        .toList();
  }

  /** Marks that a reference problem belongs to a named element. */
  static LootFinding unresolved(Identifier owner, Identifier target) {
    return new LootFinding(
        LootFindingCode.UNRESOLVED_REFERENCE,
        "$",
        owner + " references missing provider " + target);
  }
}
