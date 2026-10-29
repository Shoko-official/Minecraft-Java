package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;

/**
 * One entry of a synchronized registry: an identifier and, optionally, its data as network NBT. An
 * entry without data is taken from a pack the client already knows (R63); the server of this
 * project always sends data.
 */
public sealed interface RegistryEntry {

  /**
   * Returns the entry identifier.
   *
   * @return the identifier
   */
  Identifier id();

  /**
   * Returns the entry data.
   *
   * @return the data, or empty for an entry the client takes from a known pack
   */
  Optional<NbtTag> optionalData();

  /**
   * An entry with its data, written with a true presence Boolean.
   *
   * @param id the identifier
   * @param data the data
   */
  record WithData(Identifier id, NbtTag data) implements RegistryEntry {

    @Override
    public Optional<NbtTag> optionalData() {
      return Optional.of(data);
    }
  }

  /**
   * An entry without data, written with a false presence Boolean.
   *
   * @param id the identifier
   */
  record WithoutData(Identifier id) implements RegistryEntry {

    @Override
    public Optional<NbtTag> optionalData() {
      return Optional.empty();
    }
  }
}
