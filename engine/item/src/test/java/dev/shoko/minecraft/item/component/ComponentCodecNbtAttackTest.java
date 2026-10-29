package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.CodecFixture.Sample;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R9 to IC-R13, IC-R24, IC-R27 (adversarial): fixed seeds,
// random and mutated NBT values. Whatever the input, the answer is a Decoding (a failure holds an
// ERROR finding, a success none), and an accepted value survives the round trips of the codec.
class ComponentCodecNbtAttackTest {

  private static final String[] FIELD_KEYS = {
    "nutrition",
    "saturation",
    "can_always_eat",
    "value",
    "id",
    "rules",
    "blocks",
    "speed",
    "translate",
    "slot",
    "type",
    "amount",
    "operation",
    "items",
    "potion",
    "layers",
    "count",
    "components",
    "",
    "Not Valid",
    "minecraft:sharpness"
  };

  private static final float[] SPECIAL_FLOATS = {
    Float.NaN, -0.0f, 0.0f, 1.5f, -2.8f, Float.MAX_VALUE, Float.MIN_VALUE
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

  private static NbtTag randomTag(Random random, int depth) {
    int kind = random.nextInt(depth <= 0 ? 8 : 11);
    return switch (kind) {
      case 0 -> new ByteTag((byte) random.nextInt(256));
      case 1 -> new ShortTag((short) random.nextInt(65536));
      case 2 -> new IntTag(random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(300) - 5);
      case 3 -> new LongTag(random.nextLong());
      case 4 -> FloatTag.of(SPECIAL_FLOATS[random.nextInt(SPECIAL_FLOATS.length)]);
      case 5 -> DoubleTag.of(random.nextBoolean() ? random.nextGaussian() : -0.0);
      case 6 -> new StringTag(randomString(random));
      case 7 -> IntArrayTag.of(random.nextInt(5), random.nextInt(5));
      case 8 -> randomList(random, depth);
      default -> randomCompound(random, depth);
    };
  }

  private static String randomString(Random random) {
    String[] words = {
      "", "common", "epic", "minecraft:stone", "#minecraft:planks", "head", "add_value", "x y"
    };
    return words[random.nextInt(words.length)];
  }

  private static NbtTag randomList(Random random, int depth) {
    List<NbtTag> elements = new ArrayList<>();
    int size = random.nextInt(4);
    boolean same = random.nextBoolean();
    NbtTag first = randomTag(random, depth - 1);
    for (int i = 0; i < size; i++) {
      elements.add(same && i > 0 ? sameKind(random, first, depth) : randomTag(random, depth - 1));
    }
    return ListTag.of(elements);
  }

  private static NbtTag sameKind(Random random, NbtTag model, int depth) {
    for (int attempt = 0; attempt < 8; attempt++) {
      NbtTag candidate = randomTag(random, depth - 1);
      if (candidate.type() == model.type()) {
        return candidate;
      }
    }
    return model;
  }

  private static CompoundTag randomCompound(Random random, int depth) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    int size = random.nextInt(5);
    for (int i = 0; i < size; i++) {
      entries.put(FIELD_KEYS[random.nextInt(FIELD_KEYS.length)], randomTag(random, depth - 1));
    }
    return CompoundTag.of(entries);
  }

  private static NbtTag mutateTag(Random random, NbtTag tag) {
    if (random.nextInt(6) == 0) {
      return randomTag(random, 2);
    }
    if (tag instanceof CompoundTag compound) {
      SequencedMap<String, NbtTag> entries = new LinkedHashMap<>(compound.entries());
      List<String> keys = new ArrayList<>(entries.keySet());
      switch (random.nextInt(3)) {
        case 0 -> {
          if (!keys.isEmpty()) {
            entries.remove(keys.get(random.nextInt(keys.size())));
          }
        }
        case 1 -> {
          if (!keys.isEmpty()) {
            String key = keys.get(random.nextInt(keys.size()));
            entries.put(key, mutateTag(random, entries.get(key)));
          }
        }
        default -> entries.put(FIELD_KEYS[random.nextInt(FIELD_KEYS.length)], randomTag(random, 2));
      }
      return CompoundTag.of(entries);
    }
    if (tag instanceof ListTag list) {
      List<NbtTag> elements = new ArrayList<>(list.elements());
      if (elements.isEmpty() || random.nextInt(3) == 0) {
        elements.add(randomTag(random, 2));
      } else {
        int at = random.nextInt(elements.size());
        if (random.nextBoolean()) {
          elements.set(at, mutateTag(random, elements.get(at)));
        } else {
          elements.remove(at);
        }
      }
      return ListTag.of(elements);
    }
    return switch (random.nextInt(3)) {
      case 0 -> new IntTag(1);
      case 1 -> new StringTag(randomString(random));
      default -> randomTag(random, 1);
    };
  }

  @Test
  @Timeout(60)
  void provisionalRandomAndMutatedNbtValuesGetTypedAnswers() {
    // spec: IC-R12 (provisional), IC-R18: an accepted value reads back from its own tag
    ComponentCodec codec = CodecFixture.codec();
    List<Sample> samples = CodecFixture.samples();
    Random random = new Random(0x4B7L);
    int accepted = 0;
    for (int i = 0; i < 6000; i++) {
      Sample sample = samples.get(random.nextInt(samples.size()));
      Identifier id = CodecFixture.id(sample.component());
      NbtTag input =
          random.nextInt(4) == 0
              ? randomTag(random, 3)
              : mutateTag(
                  random, CodecFixture.decode(codec, sample.component(), sample.json()).tag());
      Decoding<ComponentValue> decoding = codec.valueFromNbt(id, input);
      assertTypedAnswer(decoding);
      if (decoding instanceof Decoding.Success<ComponentValue> success) {
        accepted++;
        assertThat(Findings.value(codec.valueFromNbt(id, success.value().tag())))
            .isEqualTo(success.value());
      }
    }
    assertThat(accepted).isPositive();
  }

  @Test
  @Timeout(60)
  void provisionalRandomComponentsCompoundsGetTypedAnswersAndRoundTrip() {
    // spec: IC-R10, IC-R24, IC-R27 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    List<Sample> samples = CodecFixture.samples();
    Random random = new Random(0x9A7C4L);
    int accepted = 0;
    for (int i = 0; i < 3000; i++) {
      SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
      int size = random.nextInt(5);
      for (int k = 0; k < size; k++) {
        Sample sample = samples.get(random.nextInt(samples.size()));
        boolean removal = random.nextInt(3) == 0;
        String key = (removal ? "!" : "") + "minecraft:" + sample.component();
        if (random.nextInt(25) == 0) {
          key = "minecraft:unknown_component";
        }
        NbtTag canonical = CodecFixture.decode(codec, sample.component(), sample.json()).tag();
        entries.put(key, removal ? CompoundTag.empty() : mutateIfNeeded(random, canonical));
      }
      Decoding<ComponentPatch> decoding = codec.patchFromNbt(CompoundTag.of(entries));
      assertTypedAnswer(decoding);
      if (decoding instanceof Decoding.Success<ComponentPatch> success) {
        accepted++;
        assertThat(Findings.value(codec.patchFromNbt(codec.patchToNbt(success.value()))))
            .isEqualTo(success.value());
      }
    }
    assertThat(accepted).isPositive();
  }

  private static NbtTag mutateIfNeeded(Random random, NbtTag canonical) {
    return random.nextInt(4) == 0 ? mutateTag(random, canonical) : canonical;
  }

  @Test
  @Timeout(60)
  void provisionalTheComponentsOfTheFakeItemsGetTheSameAnswerTwice() {
    // spec: IC-R2: no hidden state in the codec
    ComponentCodec codec = CodecFixture.codec();
    Map<String, ComponentValue> first = new LinkedHashMap<>();
    for (String name : TestItems.COMPONENT_NAMES) {
      for (Sample sample : CodecFixture.samples()) {
        if (sample.component().equals(name)) {
          first.put(sample.toString(), CodecFixture.decode(codec, name, sample.json()));
        }
      }
    }
    ComponentCodec other = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      if (first.containsKey(sample.toString())) {
        assertThat(CodecFixture.decode(other, sample.component(), sample.json()))
            .isEqualTo(first.get(sample.toString()));
      }
    }
  }
}
