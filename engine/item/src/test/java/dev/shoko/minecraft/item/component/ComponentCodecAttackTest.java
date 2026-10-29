package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.CodecFixture.Sample;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R9 to IC-R13, IC-R24, IC-R27 (adversarial): fixed seeds,
// mutated JSON and NBT. Whatever the input, the answer is a Decoding (a failure holds an ERROR
// finding, a success none), and an accepted value survives the round trips of the codec.
class ComponentCodecAttackTest {

  private static final String[] FRAGMENTS = {
    "null",
    "true",
    "false",
    "[]",
    "{}",
    "\"x\"",
    "-1",
    "0",
    "0.0",
    "-0.0",
    "99999999999",
    "\"minecraft:\"",
    "{\"a\":1}",
    "[[]]",
    "[1,2]",
    "\"#minecraft:tag\"",
    "\"\"",
    "1.5"
  };

  private static void assertTypedAnswer(Decoding<?> decoding) {
    boolean hasError =
        decoding.findings().stream().anyMatch(f -> f.code().severity() == Severity.ERROR);
    if (decoding instanceof Decoding.Success<?>) {
      assertThat(hasError).as("a success has no ERROR finding: %s", decoding.findings()).isFalse();
    } else {
      assertThat(hasError).as("a failure has an ERROR finding: %s", decoding.findings()).isTrue();
    }
  }

  private static String mutate(Random random, String text) {
    StringBuilder sb = new StringBuilder(text);
    int operations = 1 + random.nextInt(3);
    for (int i = 0; i < operations && sb.length() > 0; i++) {
      int pos = random.nextInt(sb.length());
      switch (random.nextInt(4)) {
        case 0 -> sb.delete(pos, Math.min(sb.length(), pos + 1 + random.nextInt(3)));
        case 1 -> sb.insert(pos, FRAGMENTS[random.nextInt(FRAGMENTS.length)]);
        case 2 -> sb.setCharAt(pos, "0123456789[]{}:,\"-.tfn ".charAt(random.nextInt(23)));
        default -> {
          int end = pos;
          while (end < sb.length() && ",}]".indexOf(sb.charAt(end)) < 0) {
            end++;
          }
          sb.replace(pos, end, FRAGMENTS[random.nextInt(FRAGMENTS.length)]);
        }
      }
    }
    return sb.toString();
  }

  private static Optional<JsonNode> parse(String text) {
    return JsonReader.read(text, Json.ORIGIN).value();
  }

  private static boolean checkJsonValue(ComponentCodec codec, Identifier id, JsonNode node) {
    Decoding<ComponentValue> decoding = codec.valueFromJson(id, node);
    assertTypedAnswer(decoding);
    if (!(decoding instanceof Decoding.Success<ComponentValue> success)) {
      return false;
    }
    ComponentValue value = success.value();
    JsonNode written = Json.parseStrict(codec.valueToJson(id, value));
    assertThat(Findings.value(codec.valueFromJson(id, written))).isEqualTo(value);
    assertThat(Findings.value(codec.valueFromNbt(id, value.tag()))).isEqualTo(value);
    return true;
  }

  private static String strict(Sample sample) {
    return sample.json().replace('\'', '"');
  }

  @Test
  @Timeout(60)
  void provisionalMutatedJsonValuesGetTypedAnswersAndKeepTheirRoundTrips() {
    // spec: IC-R12, IC-R13 (provisional rejections), ComponentCodec.valueToJson
    ComponentCodec codec = CodecFixture.codec();
    List<Sample> samples = CodecFixture.samples();
    Random random = new Random(0xC0DECL);
    int parsed = 0;
    int accepted = 0;
    int rejected = 0;
    for (int i = 0; i < 4000; i++) {
      Sample sample = samples.get(random.nextInt(samples.size()));
      Optional<JsonNode> node = parse(mutate(random, strict(sample)));
      if (node.isEmpty()) {
        continue;
      }
      parsed++;
      if (checkJsonValue(codec, CodecFixture.id(sample.component()), node.get())) {
        accepted++;
      } else {
        rejected++;
      }
    }
    assertThat(parsed).isGreaterThan(1000);
    assertThat(accepted).isPositive();
    assertThat(rejected).isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalTheUnmutatedSamplesAreAcceptedAndOthersAreNotConfusedWithThem() {
    // spec: IC-R12: a sample is valid for its component only (provisional for the rest)
    ComponentCodec codec = CodecFixture.codec();
    List<Sample> samples = CodecFixture.samples();
    for (Sample sample : samples) {
      assertThat(
              checkJsonValue(codec, CodecFixture.id(sample.component()), Json.parse(sample.json())))
          .as("%s", sample)
          .isTrue();
    }
    Random random = new Random(0xA77ACL);
    for (int i = 0; i < 2000; i++) {
      Sample shape = samples.get(random.nextInt(samples.size()));
      Sample other = samples.get(random.nextInt(samples.size()));
      checkJsonValue(codec, CodecFixture.id(other.component()), Json.parse(shape.json()));
    }
  }

  @Test
  @Timeout(60)
  void provisionalMutatedTablesAndPatchesGetTypedAnswers() {
    // spec: IC-R2, IC-R10, IC-R28 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    List<Sample> samples = CodecFixture.samples();
    Random random = new Random(0x7AB1EL);
    int parsed = 0;
    for (int i = 0; i < 3000; i++) {
      StringBuilder text = new StringBuilder("{");
      int members = random.nextInt(5);
      for (int m = 0; m < members; m++) {
        Sample sample = samples.get(random.nextInt(samples.size()));
        String prefix = random.nextInt(4) == 0 ? "!" : "";
        String name = random.nextInt(15) == 0 ? "not_a_component" : sample.component();
        text.append(m > 0 ? "," : "")
            .append('"')
            .append(prefix)
            .append("minecraft:")
            .append(name)
            .append("\":")
            .append(strict(sample));
      }
      String document =
          random.nextBoolean() ? text.append('}').toString() : mutate(random, text + "}");
      Optional<JsonNode> node = parse(document);
      if (node.isEmpty()) {
        continue;
      }
      parsed++;
      assertTypedAnswer(codec.tableFromJson(node.get()));
      Decoding<ComponentPatch> patch = codec.patchFromJson(node.get());
      assertTypedAnswer(patch);
      if (patch instanceof Decoding.Success<ComponentPatch> success) {
        assertThat(Findings.value(codec.patchFromNbt(codec.patchToNbt(success.value()))))
            .isEqualTo(success.value());
      }
    }
    assertThat(parsed).isGreaterThan(1000);
  }
}
