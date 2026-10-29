package dev.shoko.minecraft.net.type;

import java.util.Optional;

/**
 * A property of a game profile, such as the skin textures: a name String(64), a value String(32767)
 * and an optional signature String(1024) (R20).
 */
public sealed interface ProfileProperty {

  /**
   * Returns the property name.
   *
   * @return the name
   */
  String name();

  /**
   * Returns the property value.
   *
   * @return the value
   */
  String value();

  /**
   * Returns the signature, as an optional because the record components cannot be.
   *
   * @return the signature, or empty for an unsigned property
   */
  Optional<String> optionalSignature();

  /**
   * A property with no signature, written with a false presence Boolean.
   *
   * @param name the name
   * @param value the value
   */
  record Unsigned(String name, String value) implements ProfileProperty {

    @Override
    public Optional<String> optionalSignature() {
      return Optional.empty();
    }
  }

  /**
   * A property with a signature, written with a true presence Boolean.
   *
   * @param name the name
   * @param value the value
   * @param signature the signature
   */
  record Signed(String name, String value, String signature) implements ProfileProperty {

    @Override
    public Optional<String> optionalSignature() {
      return Optional.of(signature);
    }
  }
}
