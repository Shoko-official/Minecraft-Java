package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Extraction of the resources and verification of the pins (bundle, inner archive, identity,
 * metadata) against the fake release.
 */
class BootstrapTest extends BootstrapFixture {

  @Test
  void extractsTheResourcesAndNoCode() throws Exception {
    Bootstrap.Outcome outcome = run(publishGood(), OPTIONS);

    assertThat(outcome).isEqualTo(Bootstrap.Outcome.REBUILT);
    assertThat(resourceFiles()).containsExactly(LANG, STICK);
    assertThat(layout.resources().resolve(LANG)).hasContent(LANG_TEXT);
    assertThat(layout.identity()).hasContent(identity().toString());
    assertThat(new StateStore(layout.state()).read())
        .hasValueSatisfying(state -> assertThat(state.resources().files()).isEqualTo(2));
  }

  @Test
  void classFilesUnderDataAreNotExtracted() throws Exception {
    run(publishGood(), OPTIONS);

    assertThat(layout.resources().resolve("data/minecraft/Sneaky.class")).doesNotExist();
    assertThat(layout.resources().resolve("net")).doesNotExist();
    assertThat(layout.resources().resolve("pack.mcmeta")).doesNotExist();
  }

  @Test
  void rejectsABundleWithAnotherSha256AndDoesNotKeepIt() throws Exception {
    Pins good = publishGood();
    Pins wrong =
        new Pins(
            good.gameVersion(),
            good.versionJson(),
            new Pins.Server(good.server().sha1(), "0".repeat(64), good.server().size()),
            good.innerServerSha256(),
            good.worldVersion(),
            good.protocolVersion(),
            good.packVersion());

    assertThatThrownBy(() -> run(wrong, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-256");
    assertThat(layout.serverJar()).doesNotExist();
    assertThat(layout.reference()).doesNotExist();
  }

  @Test
  void rejectsAnInnerArchiveThatDiffersFromTheList() throws Exception {
    byte[] inner = innerJar();
    String actual = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity(), inner, "0".repeat(64)), actual);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Inner server archive");
    assertThat(layout.resources()).doesNotExist();
  }

  @Test
  void rejectsAnInnerArchiveThatDiffersFromThePin() throws Exception {
    byte[] inner = innerJar();
    String actual = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity(), inner, actual), "0".repeat(64));

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Inner server archive");
    assertThat(layout.resources()).doesNotExist();
  }

  @ParameterizedTest
  @CsvSource({
    "id, 26.2",
    "world_version, 5022",
    "protocol_version, 776",
    "pack_version.data_major, 120",
    "pack_version.data_minor, 1",
    "pack_version.resource_major, 96",
    "pack_version.resource_minor, 0"
  })
  void rejectsABundleWithAnotherIdentity(String field, String value) throws Exception {
    JsonObject identity = identity();
    JsonObject owner = identity;
    String name = field;
    if (field.startsWith("pack_version.")) {
      owner = identity.getAsJsonObject("pack_version");
      name = field.substring("pack_version.".length());
    }
    if (name.equals("id")) {
      owner.addProperty(name, value);
    } else {
      owner.addProperty(name, Integer.parseInt(value));
    }
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity, inner, innerSha256), innerSha256);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("identity differs");
    assertThat(layout.resources()).doesNotExist();
  }

  @Test
  void rejectsAnIdentityWithAMissingFieldWithoutANullPointerException() throws Exception {
    JsonObject identity = identity();
    identity.remove("protocol_version");
    byte[] inner = innerJar();
    String innerSha256 = Hashes.hex(inner, "SHA-256");
    Pins pins = publish(bundle(identity, inner, innerSha256), innerSha256);

    assertThatThrownBy(() -> run(pins, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("field protocol_version");
  }

  @Test
  void rejectsMetadataThatNoLongerMatchesThePins() throws Exception {
    Pins good = publishGood();
    Pins otherSize =
        new Pins(
            good.gameVersion(),
            good.versionJson(),
            new Pins.Server(good.server().sha1(), good.server().sha256(), good.server().size() + 1),
            good.innerServerSha256(),
            good.worldVersion(),
            good.protocolVersion(),
            good.packVersion());

    assertThatThrownBy(() -> run(otherSize, OPTIONS))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Official metadata no longer matches");
  }
}
