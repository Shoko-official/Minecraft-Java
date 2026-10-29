package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.assembly.References;
import dev.shoko.minecraft.loot.assembly.References.Kind;
import dev.shoko.minecraft.loot.assembly.References.Ref;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds the references of named providers that no loaded provider answers (LT-R50), whatever the
 * family of the provider and of the reference: {@code from_float}, {@code from_int} and the
 * probability of a {@code binomial} cross the two families.
 */
final class ProviderReferences {

  private ProviderReferences() {}

  static List<LootFinding> unresolved(NumberProviderSet set) {
    List<LootFinding> findings = new ArrayList<>();
    for (Map.Entry<Identifier, IntProvider> entry : set.ints().entrySet()) {
      check(entry.getKey(), References.of(entry.getValue()), set, findings);
    }
    for (Map.Entry<Identifier, FloatProvider> entry : set.floats().entrySet()) {
      check(entry.getKey(), References.of(entry.getValue()), set, findings);
    }
    return findings;
  }

  private static void check(
      Identifier owner, List<Ref> refs, NumberProviderSet set, List<LootFinding> findings) {
    for (Ref ref : refs) {
      boolean missing =
          (ref.kind() == Kind.INT_PROVIDER && set.intProvider(ref.id()).isEmpty())
              || (ref.kind() == Kind.FLOAT_PROVIDER && set.floatProvider(ref.id()).isEmpty());
      if (missing) {
        findings.add(NumberProviderCodec.unresolved(owner, ref.id()));
      }
    }
  }
}
