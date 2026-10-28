package dev.shoko.minecraft.world.block;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Converts a block state to and from an entry of a {@code block_states} palette (R16 to R22).
 *
 * <p>Reading (provisional, R19 to R22) never fails and never refuses a chunk: no invalid entry does
 * (R22 b). Each case other than a valid entry sends one warning, which names the block and the
 * property concerned (R22 c):
 *
 * <ul>
 *   <li>a String entry is the default state of that block;
 *   <li>a Compound {@code {id, properties?}}: each missing property takes the value of the default
 *       state, {@code DEFAULTED_PROPERTY} (R20); the order of keys is irrelevant (R18);
 *   <li>unknown block identifier gives {@code air}, {@code UNKNOWN_BLOCK}; unknown property is
 *       ignored, {@code UNKNOWN_PROPERTY}; a value outside the list takes the default state's value
 *       for that property, {@code INVALID_PROPERTY_VALUE} (R21);
 *   <li>any other tag type, or a Compound without a String {@code id}, is treated like an unknown
 *       block: {@code air} and {@code MALFORMED_PALETTE_ENTRY} (R22 a).
 * </ul>
 *
 * <p>Two entries corrected to the same state stay two entries of the palette, so indices stay valid
 * (R22 d).
 *
 * <p>Writing (R16): a Compound with String {@code id}, and a Compound {@code properties} of String
 * values holding every property of the block, present only if the block has properties; the default
 * state is not elided (R17).
 */
public final class BlockStateEntryCodec implements PaletteEntryCodec<BlockState> {

  private final BlockStateRegistry registry;
  private final WarningSink warnings;

  private BlockStateEntryCodec(BlockStateRegistry registry, WarningSink warnings) {
    this.registry = registry;
    this.warnings = warnings;
  }

  /**
   * Creates a codec over a registry.
   *
   * @param registry the blocks and states to resolve against
   * @param warnings where the findings of the lenient read go, with the codes above
   * @return the codec
   */
  public static BlockStateEntryCodec of(BlockStateRegistry registry, WarningSink warnings) {
    return new BlockStateEntryCodec(registry, warnings);
  }

  @Override
  public BlockState decode(NbtTag entry) {
    return switch (entry) {
      case StringTag name ->
          byName(name.value()).map(registry::defaultState).orElseGet(this::unknown);
      case CompoundTag compound when compound.get("id").orElse(null) instanceof StringTag name ->
          decodeCompound(name.value(), compound.get("properties").orElse(null));
      default -> malformed(entry);
    };
  }

  private Optional<BlockType> byName(String id) {
    Optional<BlockType> block = registry.block(id);
    if (block.isEmpty()) {
      warnings.warn(WarningCode.UNKNOWN_BLOCK, "unknown block " + id + ", replaced by air");
    }
    return block;
  }

  private BlockState unknown() {
    return registry.air();
  }

  private BlockState malformed(NbtTag entry) {
    warnings.warn(
        WarningCode.MALFORMED_PALETTE_ENTRY,
        "palette entry is neither a String nor a Compound with a String id ("
            + entry.type()
            + "), replaced by air");
    return registry.air();
  }

  private BlockState decodeCompound(String id, NbtTag properties) {
    Optional<BlockType> found = byName(id);
    if (found.isEmpty()) {
      return registry.air();
    }
    BlockType block = found.get();
    CompoundTag given = properties instanceof CompoundTag compound ? compound : CompoundTag.empty();
    Map<String, String> values = new LinkedHashMap<>();
    for (BlockProperty property : block.properties()) {
      values.put(
          property.name(), valueOf(block, property, given.get(property.name()).orElse(null)));
    }
    for (String key : given.entries().keySet()) {
      if (block.property(key).isEmpty()) {
        warnings.warn(
            WarningCode.UNKNOWN_PROPERTY, "block " + id + " has no property " + key + ", ignored");
      }
    }
    return registry.state(block, values);
  }

  private String valueOf(BlockType block, BlockProperty property, NbtTag given) {
    String fallback = block.valueOf(block.defaultStateId(), property.name());
    if (given == null) {
      warnings.warn(
          WarningCode.DEFAULTED_PROPERTY,
          "block " + block.id() + " property " + property.name() + " is missing, took " + fallback);
      return fallback;
    }
    if (given instanceof StringTag text && property.values().contains(text.value())) {
      return text.value();
    }
    warnings.warn(
        WarningCode.INVALID_PROPERTY_VALUE,
        "block "
            + block.id()
            + " property "
            + property.name()
            + " has an invalid value, took "
            + fallback);
    return fallback;
  }

  @Override
  public NbtTag encode(BlockState value) {
    BlockType block = value.block();
    SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
    entry.put("id", new StringTag(block.id()));
    if (!block.properties().isEmpty()) {
      SequencedMap<String, NbtTag> properties = new LinkedHashMap<>();
      for (BlockProperty property : block.properties()) {
        properties.put(property.name(), new StringTag(value.value(property.name())));
      }
      entry.put("properties", CompoundTag.of(properties));
    }
    return CompoundTag.of(entry);
  }
}
