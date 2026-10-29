package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.world.block.BlockState;
import java.util.Map;
import java.util.SequencedMap;

/**
 * The block predicate of {@code match_block} and of the block key of a location predicate (LT-R34,
 * LT-R59): {@code {"blocks": ..., "state": {"age": "7"}}}. The optional {@code nbt}, {@code
 * components} and {@code predicates} keys of the documentation, which need a block entity, are not
 * typed (open point).
 *
 * @param blocks the accepted blocks, unrestricted when the key is absent
 * @param state the properties that must have exactly this value, in file order, unmodifiable
 */
public record BlockMatch(IdOrTagSet blocks, SequencedMap<String, String> state) {

  /**
   * Tells whether a state matches (LT-R34): its block is accepted and each listed property has the
   * listed value. A listed property the block does not have does not match.
   *
   * @param candidate a block state
   * @param access the loaded registries and tags
   * @return {@code true} when it matches
   */
  public boolean matches(BlockState candidate, RegistryAccess access) {
    Identifier id = Identifier.tryParse(candidate.block().id()).orElse(null);
    if (id == null || !blocks.contains(id, access)) {
      return false;
    }
    for (Map.Entry<String, String> expected : state.entrySet()) {
      if (candidate.block().property(expected.getKey()).isEmpty()
          || !candidate.value(expected.getKey()).equals(expected.getValue())) {
        return false;
      }
    }
    return true;
  }
}
